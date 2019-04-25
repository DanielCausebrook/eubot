package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.Packet;

public class SessionEvent<T extends Data> extends PacketEvent<T> {
    private final Session session;

    public SessionEvent(Session session, Packet<T> packet) {
        super(session, packet);
        this.session = session;
    }

    public Session getSession() {
        return session;
    }
}
