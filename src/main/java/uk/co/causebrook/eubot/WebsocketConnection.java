package uk.co.causebrook.eubot;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonParseException;
import uk.co.causebrook.eubot.events.ConnectionListener;
import uk.co.causebrook.eubot.events.PacketEvent;
import uk.co.causebrook.eubot.events.PacketListener;
import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.Packet;

import javax.websocket.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a single connection to one room.
 *
 * Each connection will appear as a separate bot.
 */
public class WebsocketConnection extends Endpoint implements Connection {
    private long nextId = 0;
    private final URI server;
    private final CookieConfig cookie;
    private javax.websocket.Session session;
    //private final ListMultimap<Type, PacketListener> pListeners = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private final ConcurrentHashMap<Type, CopyOnWriteArrayList<PacketListener>> pListeners = new ConcurrentHashMap<>();
    private final List<ConnectionListener> cListeners = new CopyOnWriteArrayList<>();
    private static final Logger logger = Logger.getLogger("connection-log");

    public WebsocketConnection(URI server) {
        this.server = server;
        cookie = null;
    }

    public WebsocketConnection(URI server, CookieConfig cookie) {
        this.server = server;
        this.cookie = cookie;
    }

    @Override
    public void onOpen(javax.websocket.Session session, EndpointConfig config) {
        this.session = session;
        session.addMessageHandler(String.class, this::onMessage);
        for (ConnectionListener l : cListeners) l.onConnect(this);
    }

    @Override
    public void onClose(javax.websocket.Session session, CloseReason reason) {
        if(session.equals(this.session)) this.session = null;
        for (ConnectionListener l : cListeners) l.onDisconnect(this);
        //TODO Pass close reason to listeners.
//        if(reason.getCloseCode().equals(CloseReason.CloseCodes.SERVICE_RESTART)) {
//            try {
//                open();
//            } catch(IOException e) {
//                onError(e);
//            }
//        }
    }

    @Override
    public void onError(javax.websocket.Session session, Throwable t) {
        for (ConnectionListener l : cListeners) l.onError(this, t);
    }

    private void onMessage(String message) {
        try {
            Packet<? extends Data> p = Packet.fromJson(message);
            logger.info("Received packet "+p.getType()+" with id "+p.getId()+".");
            handle(p);
        } catch (JsonParseException e) {
            logger.info(e.getMessage());
        }
    }

    // Safe due to restrictions imposed in addPacketListener()
    @SuppressWarnings("unchecked")
    private <T extends Data> void handle(Packet<T> p) {
        if(p.getData() == null) {
            logger.log(Level.WARNING, "Could not handle " + p.getType() + " packet with null data.");
            //TODO Handle error packets.
            return;
        }
        List<PacketListener> listenerList = pListeners.get(p.getData().getClass());
        if(listenerList != null) for (PacketListener l : listenerList) {
            ((PacketListener<T>) l).onPacket(new PacketEvent<>(this, p));
        }
    }

    @Override
    public <T extends Data> void addPacketListener(Class<T> clazz, PacketListener<T> listener) {
        pListeners.computeIfAbsent(clazz, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public <T extends Data> void removePacketListener(Class<T> clazz, PacketListener<T> listener) {
        pListeners.get(clazz).remove(listener);
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        cListeners.add(listener);
    }

    @Override
    public void removeConnectionListener(ConnectionListener listener) {
        cListeners.remove(listener);
    }

    @Override
    public void open() throws IOException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .configurator(cookie.get())
                .build();
        try {
            container.connectToServer(this, config, server);
        } catch (DeploymentException e) {
            logger.log(Level.SEVERE, "WebSocket client class is invalid.", e);
        }
    }

    public void restart(String reason) throws IOException {
        session.close(new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART , reason));
        open(); //TODO Verify calling open() is permitted before websocket close.
    }

    @Override
    public void send(Data data) {
        if(isOpen()) session.getAsyncRemote().sendText(data.toPacket().toJson());
    }

    @Override
    public <T extends Data> void sendWithReplyListener(Data d, Class<T> clazz, PacketListener<T> listener) {
        if(!isOpen()) return;

        final String id = Long.toHexString(nextId);
        final PacketListener<T> replyListener = new PacketListener<T>() {
            @Override
            public void onPacket(PacketEvent<T> e) {
                if(e.getPacket().getId().equals(id)) {
                    listener.onPacket(e);
                    removePacketListener(clazz, this);
                }
            }
        };
        addPacketListener(clazz, replyListener);
        nextId++;

        logger.info("Sent packet "+d.toPacket().getType()+" with id "+id+".");
        session.getAsyncRemote().sendText(d.toPacket(id).toJson());
    }

    @Override
    public boolean isOpen() {
        return session != null;
    }
}
