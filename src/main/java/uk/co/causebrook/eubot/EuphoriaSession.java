package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.*;
import uk.co.causebrook.eubot.packets.commands.*;
import uk.co.causebrook.eubot.packets.events.*;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.packets.replies.NickReply;
import uk.co.causebrook.eubot.packets.replies.SendReply;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EuphoriaSession extends WebsocketConnection implements Session {
    private static Logger logger = Logger.getLogger("connection-log");
    private final List<SessionListener> sListeners = new CopyOnWriteArrayList<>();
    private final List<MessageListener> mListeners = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService listenerTimeouts = Executors.newScheduledThreadPool(1);
    private SessionView session;
    private String nick;
    private String currNick;
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

    public CompletableFuture<Session> initPM(SessionView user) {
        return initPM(user.getId());
    }

    @Override
    public CompletableFuture<List<SessionView>> getUsersByName(String name, String regexIgnored) {
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
        addPacketListener(SnapshotEvent.class, e -> setNick(nick));
        addPacketListener(HelloEvent.class, p -> {
            session = p.getPacket().getData().getSession();
            currNick = session.getName();
            if(nick == null) nick = currNick;
        });
        //addPacketListener(NickReply.class, p -> nick = p.getData().getTo());
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
        });
        addPacketListener(SnapshotEvent.class, p -> {
            for(SessionListener rCL : sListeners) rCL.onJoin(p);
        });
        addPacketListener(BounceEvent.class, p -> {
            for(SessionListener rCL : sListeners) rCL.onBounce(new RoomBounceEvent(this, p.getPacket()));
        });
        addPacketListener(DisconnectEvent.class, p -> {
            for(SessionListener rCL : sListeners) rCL.onDisconnect(p);
        });

    }

    public void addSessionListener(SessionListener listener) {
        sListeners.add(listener);
    }

    public void removeSessionListener(SessionListener listener) {
        sListeners.remove(listener);
    }

    @Override
    public void addMessageListener(MessageListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void removeMessageListener(MessageListener listener) {
        mListeners.remove(listener);
    }

    public String getNick() {
        return nick;
    }

    @Override
    public CompletableFuture<MessageEvent<SendReply>> send(String message) {
        var wrapper = new Object(){ long stamp = nickLock.readLock(); };
        return send(new Send(message)).whenComplete((e, ex) -> nickLock.unlockRead(wrapper.stamp))
                .thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    @Override
    public CompletableFuture<MessageEvent<SendReply>> reply(String message, SendEvent parent) {
        var wrapper = new Object(){ long stamp = nickLock.readLock(); };
        return send(new Send(message, parent)).whenComplete((e, ex) -> nickLock.unlockRead(wrapper.stamp))
                .thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    @Override
    public CompletableFuture<MessageEvent<SendReply>> reply(String message, String parentId) {
        var wrapper = new Object(){ long stamp = nickLock.readLock(); };
        return send(new Send(message, parentId)).whenComplete((e, ex) -> nickLock.unlockRead(wrapper.stamp))
                .thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    private CompletableFuture<MessageEvent<SendReply>> sendAs(Send message, String tempNick) {
        var wrapper = new Object(){ long stamp = nickLock.writeLock(); };
        return send(new Nick(tempNick))
                .thenCompose(e -> send(message))
                .whenComplete((e, ex) -> send(new Nick(nick))
                        .whenComplete((nE, nEx) -> {
                            if(nEx != null) logger.log(Level.WARNING, "sendAs call was unable to reset nick.", nEx);
                            nickLock.unlockWrite(wrapper.stamp);
                        })
                )
                .whenComplete((e, ex) -> nickLock.unlockWrite(wrapper.stamp))
                .thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    @Override
    public CompletableFuture<MessageEvent<SendReply>> sendAs(String message, String nick) {
        return sendAs(new Send(message), nick);
    }

    @Override
    public CompletableFuture<MessageEvent<SendReply>> replyAs(String message, String nick, SendEvent parent) {
        return sendAs(new Send(message, parent), nick);
    }

    @Override
    public CompletableFuture<MessageEvent<SendReply>> replyAs(String message, String nick, String parentId) {
        return sendAs(new Send(message, parentId), nick);
    }

    @Override
    public void addMessageReplyListener(SendEvent message, MessageListener replyListener) {
        addMessageListener(e2 -> {
            if(message.getId().equals(e2.getData().getParent())) replyListener.onPacket(e2);
        });
    }

    @Override
    public void addMessageReplyListener(SendEvent message, MessageListener replyListener, Duration timeout) {
        MessageListener l = e2 -> {
            if(message.getId().equals(e2.getData().getParent())) replyListener.onPacket(e2);
        };
        addMessageListener(l);
        listenerTimeouts.schedule(
                () -> removeMessageListener(l),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    public CompletionStage<Void> setNick(String nick) {
        this.nick = nick;
        if(isOpen()) return send(new Nick(nick)).thenAccept(e -> {});
        else return CompletableFuture.failedStage(new IllegalStateException("The session is not open."));
    }

}
