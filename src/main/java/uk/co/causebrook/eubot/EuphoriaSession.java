package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.*;
import uk.co.causebrook.eubot.packets.commands.*;
import uk.co.causebrook.eubot.packets.events.*;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.packets.replies.NickReply;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EuphoriaSession extends WebsocketConnection implements Session {
    private static Logger logger = Logger.getLogger("connection-log");
    private final List<SessionListener> sListeners = new CopyOnWriteArrayList<>();
    private final List<MessageListener> mListeners = new CopyOnWriteArrayList<>();
    private final Map<String, List<MessageListener>> rListeners = new ConcurrentHashMap<>();
    private SessionView session;
    private String currNick;
    private CompletableFuture<PacketEvent<NickReply>> nickInitListener;
    private StampedLock nickLock = new StampedLock();

    private EuphoriaSession(URI uri) {
        super(uri);
        initSessionListeners();
        initStandardListeners(uri.getRawPath());
    }

    private EuphoriaSession(URI uri, CookieConfig cookie) {
        super(uri, cookie);
        initSessionListeners();
        initStandardListeners(uri.getRawPath());
    }

    public static Session getRoom(String room) throws URISyntaxException {
        return new EuphoriaSession(new URI("wss://euphoria.io/room/" + room + "/ws"));
    }

    public static Session getRoom(String room, CookieConfig cookie) throws URISyntaxException {
        return new EuphoriaSession(new URI("wss://euphoria.io/room/" + room + "/ws"), cookie);
    }

    public static Session getPM(String pmid, CookieConfig cookie) throws URISyntaxException {
        return new EuphoriaSession(new URI("wss://euphoria.io/room/pm:" + pmid + "/ws"), cookie);
    }

    @Override
    public CompletableFuture<Session> initPM(String userId) {
        CompletableFuture<Session> pmRoom = new CompletableFuture<>();
        if(hasCookie()) {
            send(new PMInitiate(userId))
                    .thenAccept(e -> {
                        try {
                            pmRoom.complete(EuphoriaSession.getPM(e.getData().getPmId(), getCookieConfig()));
                        } catch(URISyntaxException err) {
                            pmRoom.completeExceptionally(err);
                        }
                    });
        } else pmRoom.completeExceptionally(new IllegalStateException("This session is not using a cookie and cannot initialise PMs."));
        return pmRoom;
    }

    @Override
    public CompletableFuture<Session> initPM(SessionView user) {
        return initPM(user.getId());
    }

    @Override
    public CompletableFuture<List<SessionView>> requestUsersByName(String name, String regexIgnored) {
        String modifiedName = name.replaceAll(regexIgnored, "");
        CompletableFuture<List<SessionView>> futMatchingUsers = new CompletableFuture<>();
        send(new Who()).thenAccept(e2 -> {
            List<SessionView> sessions = e2.getData().getListing();
            futMatchingUsers.complete(
                    sessions.stream()
                            .filter(sV -> modifiedName.equals(sV.getName().replaceAll(regexIgnored, "")))
                            .collect(Collectors.toList())
            );
        });
        return futMatchingUsers;
    }

    private void initStandardListeners(String room) {
        addPacketListener(PingEvent.class, p -> p.getData().reply(this));
        addPacketListener(SnapshotEvent.class, e -> {
            if(currNick == null) currNick = session.getName();
            else setNick(currNick).whenComplete((nE, nEx) -> {
                if(nEx != null) nickInitListener.completeExceptionally(nEx);
                else nickInitListener.complete(nE);
            });
        });
        addPacketListener(HelloEvent.class, p -> session = p.getPacket().getData().getSession());
        addPacketListener(NickReply.class, p -> currNick = p.getData().getTo());
        addPacketListener(DisconnectEvent.class, p -> {
            try {
                if(p.getData().getReason().equals("authentication changed")) restart("authentication changed");
            } catch(IOException e) {
                logger.log(Level.SEVERE, "Could not reconnect to room " + room + " after authentication change.", e);
            }
        });
    }

    private void initSessionListeners() {
        addPacketListener(SendEvent.class, p -> {
            for(MessageListener mL : mListeners) mL.onPacket(new MessageEvent<>(this, p.getPacket()));
            for(MessageListener mL : rListeners.get(p.getData().getParent()))
                mL.onPacket(new MessageEvent<>(this, p.getPacket()));
        });
        addPacketListener(SnapshotEvent.class, p -> {
            for(SessionListener rCL : sListeners) rCL.onJoin(new SessionEvent<>(this, p.getPacket()));
        });
        addPacketListener(BounceEvent.class, p -> {
            for(SessionListener rCL : sListeners) rCL.onBounce(new RoomBounceEvent(this, p.getPacket()));
        });
        addPacketListener(DisconnectEvent.class, p -> {
            for(SessionListener rCL : sListeners) rCL.onDisconnect(new SessionEvent<>(this, p.getPacket()));
        });

    }

    @Override
    public void addSessionListener(SessionListener listener) {
        sListeners.add(listener);
    }

    @Override
    public boolean removeSessionListener(SessionListener listener) {
        return sListeners.remove(listener);
    }

    @Override
    public void addMessageListener(MessageListener listener) {
        mListeners.add(listener);
    }

    @Override
    public boolean removeMessageListener(MessageListener listener) {
        return mListeners.remove(listener);
    }

    @Override
    public CompletableFuture<MessageEvent<?>> send(String message) {
        var wrapper = new Object(){ long stamp = nickLock.readLock(); };
        return send(new Send(message)).whenComplete((e, ex) -> nickLock.unlockRead(wrapper.stamp))
                .thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    @Override
    public CompletableFuture<MessageEvent<?>> reply(String message, String parentId) {
        var wrapper = new Object(){ long stamp = nickLock.readLock(); };
        return send(new Send(message, parentId)).whenComplete((e, ex) -> nickLock.unlockRead(wrapper.stamp))
                .thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    private CompletableFuture<MessageEvent<?>> sendAs(Send message, String tempNick) {
        var wrapper = new Object(){ long stamp = nickLock.writeLock(); String oldNick = currNick; };
        return send(new Nick(tempNick))
                .thenCompose(e -> send(message))
                .whenComplete((e, ex) -> send(new Nick(wrapper.oldNick))
                        .whenComplete((nE, nEx) -> {
                            if(nEx != null) logger.log(Level.WARNING, "sendAs call was unable to reset the nick.", nEx);
                            nickLock.unlockWrite(wrapper.stamp);
                        })
                )
                .thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    @Override
    public CompletableFuture<MessageEvent<?>> sendAs(String message, String nick) {
        return sendAs(new Send(message), nick);
    }

    @Override
    public CompletableFuture<MessageEvent<?>> replyAs(String message, String nick, String parentId) {
        return sendAs(new Send(message, parentId), nick);
    }


    @Override
    public void addMessageReplyListener(String messageId, MessageListener replyListener) {
        rListeners.putIfAbsent(messageId, new CopyOnWriteArrayList<>());
        rListeners.get(messageId).add(replyListener);
    }

    @Override
    public boolean removeMessageReplyListener(String messageId, MessageListener replyListener) {
        if(!rListeners.containsKey(messageId)) return false;
        return rListeners.get(messageId).remove(replyListener);
    }

    @Override
    public String getNick() {
        return currNick;
    }

    @Override
    public CompletableFuture<PacketEvent<NickReply>> setNick(String nick) {
        if(isOpen()) {
            var wrapper = new Object() { long stamp = nickLock.writeLock(); };
            return send(new Nick(nick))
                    .whenComplete((e, ex)-> nickLock.unlockWrite(wrapper.stamp));
        } else {
            this.currNick = nick;
            return nickInitListener = new CompletableFuture<>();
        }
    }

}
