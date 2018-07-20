package uk.co.causebrook.eubot.relay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SharedMessage {
    private List<RelayMessage> messages;
    private SharedMessageThread thread;
    private SharedMessage parent;

    public SharedMessage(List<RelayMessage> messages, SharedMessage parent, SharedMessageThread thread) {
        this.messages = messages;
        this.parent = parent;
        this.thread = thread;
    }

    public SharedMessage reply(String message) {
        List<RelayMessage> replies = new ArrayList<>();
        SharedMessage sharedMessage = new SharedMessage(replies, this, thread);
        for(RelayMessage m : messages) m.reply(message).thenAccept((clone) -> {
            replies.add(clone);
            thread.registerSharedMessage(clone, sharedMessage);
        });
        return sharedMessage;
    }

    public CompletableFuture<SharedMessage> replyAs(String message, String nick) {
        return messages.stream().collect(
                () -> CompletableFuture.completedFuture(new ArrayList<RelayMessage>()),
                (fut, m) -> fut.thenCombine(m.replyAs(message, nick), List::add),
                (futA, futB) -> futA.thenCombine(futB, List::addAll)
        ).thenApply((l) -> new SharedMessage(l, this, thread));
    }

    CompletableFuture<SharedMessage> shareChild(RelayMessage child) {
        if(!messages.contains(child.getParent())) throw new IllegalArgumentException("RelayMessage is not a child.");
        return messages.stream()
                .filter((m) -> !m.equals(child.getParent()))
                .collect(
                        () -> {
                            ArrayList<RelayMessage> l = new ArrayList<>();
                            l.add(child);
                            return CompletableFuture.completedFuture(l);
                        },
                        (fut, m) -> fut.thenCombine(m.replyAs(child.getData().getContent(), child.getData().getSender().getName()), List::add),
                        (futA, futB) -> futA.thenCombine(futB, List::addAll)
                )
                .thenApply((l) -> {
                    SharedMessage sM = new SharedMessage(l, this, thread);
                    for(RelayMessage m : messages) thread.registerSharedMessage(m, sM);
                    return sM;
                });
    }
}
