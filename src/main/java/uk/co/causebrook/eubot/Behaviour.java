package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.*;
import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.events.DisconnectEvent;
import uk.co.causebrook.eubot.packets.events.SnapshotEvent;

import javax.websocket.CloseReason;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Contains a set of listeners to apply to any session added to this bot.
 */
public class Behaviour {
    private final Set<Session> rooms = new LinkedHashSet<>();
    private String nick;
    private Map<Type, List<PacketListener>> pListeners = new ConcurrentHashMap<>();
    private List<MessageListener> mListeners = new CopyOnWriteArrayList<>();
    private List<SessionListener> sListeners = new CopyOnWriteArrayList<>();
    private List<ConnectionListener> cListeners = new CopyOnWriteArrayList<>();
    private PacketListener<Data> pHandler = this::handlePacket;
    private MessageListener mHandler = this::handleMessage;
    private SessionListener sHandler = new SessionListener() {
        @Override public void onJoin(SessionEvent<SnapshotEvent> e)         { sListeners.forEach(l -> l.onJoin(e));       }
        @Override public void onBounce(RoomBounceEvent e)                   { sListeners.forEach(l -> l.onBounce(e));     }
        @Override public void onDisconnect(SessionEvent<DisconnectEvent> e) { sListeners.forEach(l -> l.onDisconnect(e)); }
    };
    private ConnectionListener cHandler = new ConnectionListener() {
        @Override public void onConnect(Connection c)                    { cListeners.forEach(l -> l.onConnect(c));        }
        @Override public void onDisconnect(Connection c, CloseReason cR) { cListeners.forEach(l -> l.onDisconnect(c, cR)); }
        @Override public void onError(Connection c, Throwable err)       { cListeners.forEach(l -> l.onError(c, err));     }
    };

    public Behaviour() {}
    
    public Behaviour(String nick) {
        this.nick = nick;
    }
    
    public void add(Session s) {
        synchronized (rooms) {
            if(rooms.add(s)) {
                if(nick != null) s.setNick(nick);
                s.addPacketListener(Data.class, pHandler);
                s.addMessageListener(mHandler);
                s.addSessionListener(sHandler);
                s.addConnectionListener(cHandler);
            }
        }
    }

    public void remove(Session s) {
        synchronized (rooms) {
            if(rooms.remove(s)) {
                s.removePacketListener(Data.class, pHandler);
                s.removeMessageListener(mHandler);
                s.removeSessionListener(sHandler);
                s.removeConnectionListener(cHandler);
            }
        }
    }
    
    public void setNick(String nick) {
        this.nick = nick;
        rooms.forEach(r -> r.setNick(nick));
    }
    
    public String getNick() {
        return nick;
    }

    // Safe due to restrictions imposed in addPacketListener()
    @SuppressWarnings("unchecked")
    private <T extends Data> void handlePacket(PacketEvent<T> e) {
        List<PacketListener> listenerList = pListeners.get(e.getData().getClass());
        if (listenerList != null) listenerList.forEach(l -> ((PacketListener<T>) l).onPacket(e));
    }

    public <T extends Data> void addPacketListener(Class<T> clazz, PacketListener<T> listener) {
        pListeners.computeIfAbsent(clazz, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public <T extends Data> void removePacketListener(Class<T> clazz, PacketListener<T> listener) {
        pListeners.get(clazz).remove(listener);
    }

    private void handleMessage(MessageEvent e) {
        for(MessageListener l : mListeners) l.onPacket(e);
    }

    public void addMessageListener(MessageListener listener) {
        mListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        mListeners.remove(listener);
    }

    public void addSessionListener(SessionListener listener) {
        sListeners.add(listener);
    }

    public void removeSessionListener(SessionListener listener) {
        sListeners.remove(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        cListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        cListeners.remove(listener);
    }
}