package uk.co.causebrook.eubot.relay;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.packets.commands.Send;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class SharedMessageThread {
    private SharedMessage root;
    private Map<Message, SharedMessage> sharedMessages = new HashMap<>();
    private List<SharedMessageListener> mListeners = new CopyOnWriteArrayList<>();
    private boolean running = false;
    private Pattern filter;

    public SharedMessageThread(List<MessageThread> threads) {
        List<Message> rootList = new ArrayList<>();
        root = new SharedMessage(rootList, null, this);
        for(MessageThread t : threads) {
            t.addMessageListener((m) -> {
                // Only clone messages that are inside the thread and pass the filter.
                if(running && sharedMessages.containsKey(m.getParent()) &&
                        (filter == null || filter.matcher(m.getData().getContent()).find())) {
                    SharedMessage p = sharedMessages.get(m.getParent());
                    SharedMessage child = p.shareChild(m);

                    mListeners.forEach((l) -> l.onMessage(child));
                }
            });
            // Put thread root message in map.
            sharedMessages.put(t.getRoot(), root);
            rootList.add(t.getRoot());
        }
    }

    void registerSharedMessage(Message message, SharedMessage sharedMessage) {
        sharedMessages.put(message, sharedMessage);
    }

    public static SharedMessageThread openPoolWithMessage(List<Session> sessions, String message) throws InterruptedException {
        List<CompletableFuture<MessageThread>> futThreads = new ArrayList<>();
        for(Session s : sessions) {
            futThreads.add(s.send(new Send(message)).thenApply((e) -> new MessageThread(s, e.getData())));
        }
        List<MessageThread> threads = new ArrayList<>();
        for(CompletableFuture<MessageThread> fut : futThreads) {
            try {
                threads.add(fut.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }
        return new SharedMessageThread(threads);
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    /**
     * Sets a regular expression that shared messages have to match.
     * If set, any messages not matching this pattern will not be shared.
     * @param filter The pattern to filter incoming messages by.
     */
    public void setFilter(Pattern filter) {
        this.filter = filter;
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
}
