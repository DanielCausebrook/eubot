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

    public CompletableFuture<RelayMessage> reply(String message) {
        return thread.sendMessage(message, this);
    }

    public CompletableFuture<RelayMessage> replyAs(String message, String nick) {
        return thread.sendMessageAs(message, this, nick);
    }

    public RelayMessage getParent() {
        return parent;
    }

    public SendEvent getData() {
        return data;
    }
}
