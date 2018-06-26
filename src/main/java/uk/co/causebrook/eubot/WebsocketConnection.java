package uk.co.causebrook.eubot;

import com.google.gson.JsonParseException;
import uk.co.causebrook.eubot.events.ConnectionListener;
import uk.co.causebrook.eubot.events.PacketEvent;
import uk.co.causebrook.eubot.events.PacketListener;
import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.ReplyableData;

import javax.websocket.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    private final Map<Type, List<PacketListener>> pListeners = new ConcurrentHashMap<>();
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
            // Remember to update Behaviour.
            return;
        }
        // Listeners for this packet type.
        List<PacketListener> listenerList = pListeners.get(p.getData().getClass());
        if(listenerList != null) for (PacketListener l : listenerList) {
            ((PacketListener<T>) l).onPacket(new PacketEvent<>(this, p));
        }
        // Listeners for any packet type.
        listenerList = pListeners.get(Data.class);
        if(listenerList != null) for (PacketListener l : listenerList) {
            ((PacketListener<Data>) l).onPacket(new PacketEvent<>(this, (Packet<Data>) p));
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
        ClientEndpointConfig.Builder builder = ClientEndpointConfig.Builder.create();
        if(cookie != null) builder.configurator(cookie.get());
        ClientEndpointConfig config = builder.build();
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
    public <T extends Data> CompletableFuture<PacketEvent<T>> send(ReplyableData<T> d) {
        if(!isOpen()) throw new IllegalStateException("Connection not open yet.");

        CompletableFuture<PacketEvent<T>> event = new CompletableFuture<>();

        final String id = Long.toHexString(nextId);
        final PacketListener<T> replyListener = new PacketListener<T>() {
            @Override
            public void onPacket(PacketEvent<T> e) {
                if(e.getPacket().getId().equals(id)) {
                    event.complete(e);
                    removePacketListener(d.getReplyClass(), this);
                }
            }
        };
        addPacketListener(d.getReplyClass(), replyListener);
        nextId++;

        session.getAsyncRemote().sendText(d.toPacket(id).toJson());
        return event;
    }

    public boolean hasCookie() {
        return cookie != null;
    }

    public CookieConfig getCookie() {
        return cookie;
    }

    @Override
    public boolean isOpen() {
        return session != null;
    }
}
