package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.NickReply;

@SuppressWarnings("unused")
public class Nick extends ReplyableData<NickReply> {
    private final String name;

    public Nick(String nick) { name=nick; }

    public String getName() { return name; }

    @Override
    public Class<NickReply> getReplyClass() {
        return NickReply.class;
    }
}
