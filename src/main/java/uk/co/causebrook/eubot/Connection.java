package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.ConnectionListener;
import uk.co.causebrook.eubot.events.PacketEvent;
import uk.co.causebrook.eubot.events.PacketListener;
import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.ReplyableData;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface Connection {
    /**
     * Sends a packet of data to the server.
     * @param p The data to send over the connection.
     */
    void send(Data p);

    /**
     * Sends a packet of data and listens for replies to the packet.
     * @param p The data to send over the connection.
     * @param <T> The type of the replyWithReplyListener to listen for.
     * @return A future object that will be updated with the reply to this packet.
     */
    <T extends Data> CompletableFuture<PacketEvent<T>> send(ReplyableData<T> p);

    /**
     * Adds a listener to be triggered when a packet of the specified type is received.
     * @param clazz The data type to listen for.
     * @param listener The listener to call when the packet is received.
     * @param <T> The data type to listen for.
     */
    <T extends Data> void addPacketListener(Class<T> clazz, PacketListener<T> listener);

    /**
     * Removes a packet listener that has previously been added.
     * @param clazz The data type the listener is listening for.
     * @param listener The listener to remove.
     * @param <T> The data type the listener is listening for.
     */
    <T extends Data> void removePacketListener(Class<T> clazz, PacketListener<T> listener);

    /**
     * Adds a listener to be triggered when the websocket changes state.
     * @param listener The listener to add to this connection.
     */
    void addConnectionListener(ConnectionListener listener);

    /**
     * Removes a websocket state listener from this connection.
     * @param listener The listener to remove from this connection.
     */
    void removeConnectionListener(ConnectionListener listener);

    /**
     * Initiates the connection to the server.
     */
    void open() throws IOException;

    /**
     * Returns the current state of the websocket connection.
     * @return true if the websocket connection is open.
     */
    boolean isOpen();

    CookieConfig getCookieConfig();
}
