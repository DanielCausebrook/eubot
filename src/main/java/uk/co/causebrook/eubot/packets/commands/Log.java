package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.LogReply;

@SuppressWarnings("unused")
public class Log extends ReplyableData<LogReply> {
    private int    n;
    private String before;

    public Log(int n) {
        this.n = n;
    }

    public Log(int n, String before) {
        this.n = n;
        this.before = before;
    }

    public int getN()         { return n;      }
    public String getBefore() { return before; }

    @Override
    public Class<LogReply> getReplyClass() {
        return LogReply.class;
    }
}
