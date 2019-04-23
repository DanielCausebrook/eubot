package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.events.SendEvent;
import uk.co.causebrook.eubot.packets.replies.SendReply;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class MessageEvent<T extends SendEvent> extends PacketEvent<T> {
    private final Session session;

    public MessageEvent(Session session, Packet<T> packet) {
        super(session, packet);
        this.session = session;
    }

    public Session getSession() {
        return session;
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
        session.addMessageReplyListener(getData(), replyListener);
    }

    public void addReplyListener(MessageListener replyListener, Duration timeout) {
        session.addMessageReplyListener(getData(),replyListener, timeout);
    }

    public CompletableFuture<MessageEvent<?>> reply(String text) {
        return session.send(new Send(text, getData().getId()))
                .thenApply(e-> new MessageEvent<>(session, e.getPacket()));
    }
}
