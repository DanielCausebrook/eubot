package uk.co.causebrook.eubot.relay;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.packets.commands.Send;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class SharedMessageThread {
    private SharedMessage root;
    private List<RelayMessageThread> threads;
    private Map<RelayMessage, SharedMessage> sharedMessages = new HashMap<>();
    private List<SharedMessageListener> mListeners = new CopyOnWriteArrayList<>();

    public SharedMessageThread(List<RelayMessageThread> threads) {
        this.threads = threads;
        List<RelayMessage> rootList = new ArrayList<>();
        root = new SharedMessage(rootList, null, this);
        for(RelayMessageThread t : threads) {
            // Put thread root message in map.
            sharedMessages.put(t.getRoot(), root);
            rootList.add(t.getRoot());
        }
    }

    void registerSharedMessage(RelayMessage message, SharedMessage sharedMessage) {
        sharedMessages.put(message, sharedMessage);
    }

    public static SharedMessageThread openPoolWithMessage(List<Session> sessions, String message) throws InterruptedException {
        List<CompletableFuture<RelayMessageThread>> futThreads = new ArrayList<>();
        for(Session s : sessions) {
            futThreads.add(s.send(new Send(message)).thenApply((e) -> new RelayMessageThread(s, e.getData())));
        }
        List<RelayMessageThread> threads = new ArrayList<>();
        for(CompletableFuture<RelayMessageThread> fut : futThreads) {
            try {
                threads.add(fut.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }
        return new SharedMessageThread(threads);
    }

    public List<RelayMessageThread> getThreads() {
        return Collections.unmodifiableList(threads);
    }

    public SharedMessage getRoot() {
        return root;
    }

    public void addMessageListener(SharedMessageListener l) {
        mListeners.add(l);
    }

    public void removeMessageListener(SharedMessageListener l) {
        mListeners.remove(l);
    }

    public CompletableFuture<SharedMessage> shareMessage(RelayMessage message) {
        if(!sharedMessages.containsKey(message.getParent())) throw new IllegalArgumentException("The provided RelayMessage does not exist in this SharedThread.");
        return sharedMessages.get(message.getParent())
                .shareChild(message)
                .whenComplete((child, ex) -> {
                    if(child != null) mListeners.forEach((l) -> l.onMessage(child));
                });
    }
}
