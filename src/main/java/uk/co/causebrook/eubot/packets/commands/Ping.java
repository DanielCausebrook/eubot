package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.PingReply;

@SuppressWarnings("unused")
public class Ping extends ReplyableData<PingReply> {
    private final int time;

    public Ping(int time) { this.time=time; }

    public int getTime() { return time; }

    @Override
    public Class<PingReply> getReplyClass() {
        return PingReply.class;
    }
}
