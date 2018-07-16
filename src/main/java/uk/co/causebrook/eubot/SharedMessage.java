package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.packets.commands.Send;

import java.util.concurrent.CompletableFuture;

public class SharedMessage {
    private SharedThread thread;
    private String id;

    public SharedMessage(SharedThread thread, String id) {
        this.thread = thread;
        this.id = id;
    }

    public CompletableFuture<SharedMessage> reply(String message) {
        return thread.sendMessage(new Send(message, id)).thenApply((e) -> new SharedMessage(thread, e.getData().getId()));
    }

    public CompletableFuture<SharedMessage> replyAs(String message, String nick) {
        return thread.sendMessageAs(new Send(message, id), nick).thenApply((e) -> new SharedMessage(thread, e.getData().getId()));
    }

    public String getId() {
        return id;
    }

    public SharedThread getThread() {
        return thread;
    }
}
