package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.packets.events.*;

/**
 * Listens for key changes in the state of this session.
 */
public interface SessionListener {
    /**
     * Indicates that the session has successfully joined the room.
     */
    void onJoin(PacketEvent<SnapshotEvent> e);

    /**
     * Indicates that the room is private and the user must authorise themselves.
     */
    void onBounce(RoomBounceEvent e);

    /**
     * Indicates that the server is about to disconnect the client.
     */
    void onDisconnect(PacketEvent<DisconnectEvent> e);
}
