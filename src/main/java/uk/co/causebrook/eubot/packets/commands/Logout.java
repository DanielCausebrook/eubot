package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.LogoutReply;

public class Logout extends ReplyableData<LogoutReply> {
    @Override
    public Class<LogoutReply> getReplyClass() {
        return LogoutReply.class;
    }
}
