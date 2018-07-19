package uk.co.causebrook.eubot.relay;

import uk.co.causebrook.eubot.packets.events.SendEvent;

import java.util.concurrent.CompletableFuture;

public class Message {
    private MessageThread thread;
    private SendEvent data;
    private Message parent;

    public Message(MessageThread thread, Message parent, SendEvent data) {
        this.thread = thread;
        this.parent = parent;
        this.data = data;
    }

    public CompletableFuture<Message> reply(String message) {
        return thread.sendMessage(message, this);
    }

    public CompletableFuture<Message> replyAs(String message, String nick) {
        return thread.sendMessageAs(message, this, nick);
    }

    public Message getParent() {
        return parent;
    }

    public SendEvent getData() {
        return data;
    }
}
