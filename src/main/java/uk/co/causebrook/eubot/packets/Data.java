package uk.co.causebrook.eubot.packets;

public abstract class Data {
    public final Packet toPacket() {
        return new Packet<>(this);
    }

    public final Packet toPacket(String id) {
        return new Packet<>(this, id);
    }
}
