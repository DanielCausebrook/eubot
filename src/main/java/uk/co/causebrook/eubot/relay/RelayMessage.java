package uk.co.causebrook.eubot.relay;

import uk.co.causebrook.eubot.packets.events.SendEvent;

import java.util.concurrent.CompletableFuture;

public class RelayMessage {
    private RelayMessageThread thread;
    private SendEvent data;
    private RelayMessage parent;

    public RelayMessage(RelayMessageThread thread, RelayMessage parent, SendEvent data) {
        this.thread = thread;
        this.parent = parent;
        this.data = data;
    }

    public String getContent() {
        return data.getContent();
    }

    public String getSenderNick() {
        return data.getSender().getName();
    }

    public String getSenderId() {
        return data.getSender().getId();
    }

    public CompletableFuture<RelayMessage> reply(String message) {
        return thread.reply(message, this);
    }

    public CompletableFuture<RelayMessage> replyAs(String message, String nick) {
        return thread.replyAs(message, nick, this);
    }

    public RelayMessage getParent() {
        return parent;
    }

    public SendEvent getData() {
        return data;
    }
}
