package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.packets.Data;

/**
 * Listens for when a packet of a specific type is received.
 * @param <T> The packet type that this listener listens for.
 */
@FunctionalInterface
public interface PacketListener<T extends Data> {
    void onPacket(PacketEvent<T> e);
}
