package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.WhoReply;

public class Who extends ReplyableData<WhoReply> {
    @Override
    public Class<WhoReply> getReplyClass() {
        return WhoReply.class;
    }
}

