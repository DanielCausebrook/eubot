package uk.co.causebrook.eubot.relay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class SharedMessage {
    private List<RelayMessage> messages;
    private SharedMessageThread thread;
    private SharedMessage parent;

    public SharedMessage(List<RelayMessage> messages, SharedMessage parent, SharedMessageThread thread) {
        this.messages = messages;
        this.parent = parent;
        this.thread = thread;
    }

    public List<RelayMessage> getMessages() {
        return Collections.unmodifiableList(messages);
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
                () -> new AtomicReference<CompletableFuture<List<RelayMessage>>>(CompletableFuture.completedFuture(new ArrayList<>())),
                (fut, m) -> fut.getAndUpdate((f) -> f.thenCombine(m.replyAs(message, nick), (l, ch) -> {l.add(ch); return l;})),
                (futA, futB) -> futA.getAndUpdate((f) -> f.thenCombine(futB.get(), (l1, l2) -> {l1.addAll(l2); return l1;}))
        ).get().thenApply((l) -> new SharedMessage(l, this, thread));
    }

    CompletableFuture<SharedMessage> shareChild(RelayMessage child) {
        if(!messages.contains(child.getParent())) throw new IllegalArgumentException("RelayMessage is not a child.");
        return messages.stream()
                .filter((m) -> !m.equals(child.getParent()))
                .collect(
                        () -> new AtomicReference<CompletableFuture<List<RelayMessage>>>(CompletableFuture.completedFuture(new ArrayList<>())),
                        (fut, m) -> fut.getAndUpdate((f) -> f.thenCombine(m.replyAs(child.getData().getContent(), child.getData().getSender().getName()), (l,ch) -> {l.add(ch); return l;})),
                        (futA, futB) -> futA.getAndUpdate((f) -> f.thenCombine(futB.get(), (l1,l2) -> {l1.addAll(l2); return l1;}))
                )
                .get().thenApply((l) -> {
                    l.add(child);
                    SharedMessage sM = new SharedMessage(l, this, thread);
                    for(RelayMessage m : l) thread.registerSharedMessage(m, sM);
                    return sM;
                });
    }
}
