package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.events.SendEvent;

import java.time.Duration;

public class MessageEvent extends PacketEvent<SendEvent> {
    private final Session session;

    public MessageEvent(Session session, Packet<SendEvent> packet) {
        super(session, packet);
        this.session = session;
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
        session.addReplyListener(getData(),replyListener);
    }

    public void addReplyListener(MessageListener replyListener, Duration timeout) {
        session.addReplyListener(getData(),replyListener, timeout);
    }

    public void reply(String text) {
        session.sendMessage(new Send(text, getData().getId()));
    }

    public void replyWithReplyListener(String text, MessageListener replyListener) {
        session.sendMessageWithReplyListener(new Send(text, getData().getId()), replyListener);
    }


    public void replyWithReplyListener(String text, MessageListener replyListener, Duration timeout) {
        session.sendMessageWithReplyListener(new Send(text, getData().getId()), replyListener, timeout);
    }

}
