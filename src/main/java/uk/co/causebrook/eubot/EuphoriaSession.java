package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.*;
import uk.co.causebrook.eubot.packets.commands.Nick;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.events.*;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.packets.replies.NickReply;
import uk.co.causebrook.eubot.packets.replies.SendReply;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EuphoriaSession extends WebsocketConnection implements Session {
    private static Logger logger = Logger.getLogger("connection-log");
    private final List<SessionListener> sListeners = new CopyOnWriteArrayList<>();
    private final List<MessageListener> mListeners = new CopyOnWriteArrayList<>();
    private SessionView session;
    private String nick;
    private String nextNick;

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

    private void initStandardListeners(String room) {
        addPacketListener(PingEvent.class, p -> p.getData().reply(this));
        addPacketListener(SnapshotEvent.class, e -> {
            if(nextNick != null) setNick(nextNick);
        });
        addPacketListener(HelloEvent.class, p -> {
            session = p.getPacket().getData().getSession();
            nick = session.getName();
        });
        addPacketListener(NickReply.class, p -> nick = p.getData().getTo());
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
            for(MessageListener mL : mListeners) mL.onPacket(new MessageEvent(this, p.getPacket()));
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
    public void sendMessage(Send message) {
        send(message);
    }

    @Override
    public void sendMessageWithReplyListener(Send message, MessageListener replyListener) {
        sendWithReplyListener(message, SendReply.class, e -> addMessageListener(e2 -> {
            if(e.getData().getId().equals(e2.getData().getParent())) replyListener.onPacket(e2);
        }));
    }
    @Override
    public void sendMessageWithReplyListener(Send message, MessageListener replyListener, Duration timeout) {
        sendWithReplyListener(message, SendReply.class, e -> {
            MessageListener l = e2 -> {
                if(e.getData().getId().equals(e2.getData().getParent())) replyListener.onPacket(e2);
            };
            addMessageListener(l);
            Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> removeMessageListener(l),
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );
        });
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
        Executors.newSingleThreadScheduledExecutor().schedule(
                () -> removeMessageListener(l),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    public void setNick(String nick) {
        nextNick = nick;
        send(new Nick(nick));
    }

}
