package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.events.SendEvent;
import uk.co.causebrook.eubot.packets.replies.SendReply;

@SuppressWarnings("unused")
public class Send extends ReplyableData<SendReply> {
    private final String content;
    private String parent;

    public Send(String message, String parent) {
        content = message;
        this.parent = parent;
    }

    public Send(String message, Packet<SendEvent> parent) {
        content = message;
        this.parent = parent.getData().getId();
    }

    public Send(String message) { content = message; }

    public String getContent() { return content; }
    public String getParent()  { return parent;  }

    @Override
    public Class<SendReply> getReplyClass() {
        return SendReply.class;
    }
}
