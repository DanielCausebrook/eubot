package uk.co.causebrook.eubot.packets.events;

import uk.co.causebrook.eubot.Connection;
import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.replies.PingReply;

@SuppressWarnings("unused")
public class PingEvent extends Data {
    private int time;
    private int next;

    public Packet createPingReply() {
        return new PingReply(time).toPacket();
    }

    /**
     * Sends a replyWithReplyListener to a ping event.
     *
     * @param c The connection to send the replyWithReplyListener to.
     */
    public void reply(Connection c) {
        c.send(new PingReply(time));
    }

    public int getTime() { return time; }
    public int getNext() { return next; }
}
