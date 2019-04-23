package uk.co.causebrook.eubot.relay;

import uk.co.causebrook.eubot.packets.events.SendEvent;

import java.util.concurrent.CompletableFuture;

public class RelayMessage {
    private RelayMessageThread thread;
    private SendEvent data;
    private RelayMessage parent;
    //TODO Add flag to prevent relaying, instead of regex filter in SharedMessageThread

    public RelayMessage(RelayMessageThread thread, RelayMessage parent, SendEvent data) {
        this.thread = thread;
        this.parent = parent;
        this.data = data;
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
