package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.Connection;
import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.Packet;

public class PacketEvent<T extends Data> {
    private final Connection connection;
    private final Packet<T> packet;

    public PacketEvent(Connection connection, Packet<T> packet) {
        this.connection = connection;
        this.packet = packet;
    }

    public Packet<T> getPacket() {
        return packet;
    }

    public T getData() {
        return packet.getData();
    }

    public Connection getRoomConnection() {
        return connection;
    }
}
