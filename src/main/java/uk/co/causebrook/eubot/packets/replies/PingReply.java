package uk.co.causebrook.eubot.packets.replies;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class PingReply extends Data {
    private final int time;

    public PingReply(int t) { time=t; }

    public int getTime() { return time; }
}
