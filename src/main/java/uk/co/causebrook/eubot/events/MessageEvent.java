package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.events.SendEvent;
import uk.co.causebrook.eubot.packets.replies.SendReply;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class MessageEvent<T extends SendEvent> extends SessionEvent<T> {

    public MessageEvent(Session session, Packet<T> packet) {
        super(session, packet);
    }

    public String getContent() {
        return getData().getContent();
    }

    public String getId() {
        return getData().getId();
    }

    public String getSenderNick() {
        return getData().getSender().getName();
    }

    public String getSenderId() {
        return getData().getSender().getId();
    }

    public void addReplyListener(MessageListener replyListener) {
        getSession().addMessageReplyListener(getData(), replyListener);
    }

    public boolean removeReplyListener(MessageListener replyListener) {
        return getSession().removeMessageReplyListener(getData(), replyListener);
    }

    public CompletableFuture<MessageEvent<?>> reply(String text) {
        return getSession().send(new Send(text, getData().getId()))
                .thenApply(e-> new MessageEvent<>(getSession(), e.getPacket()));
    }
}
