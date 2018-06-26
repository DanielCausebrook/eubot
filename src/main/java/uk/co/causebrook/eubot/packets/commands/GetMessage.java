package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.GetMessageReply;

@SuppressWarnings("unused")
public class GetMessage extends ReplyableData<GetMessageReply> {
    private final String id;

    public GetMessage(String id) { this.id=id; }

    public String getId() { return id; }

    @Override
    public Class<GetMessageReply> getReplyClass() {
        return GetMessageReply.class;
    }
}
