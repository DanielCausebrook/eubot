package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.MessageListener;
import uk.co.causebrook.eubot.events.SharedMessageEvent;
import uk.co.causebrook.eubot.events.SharedMessageListener;
import uk.co.causebrook.eubot.packets.commands.Send;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SharedThreadPool {
    private List<SharedThread> threads;
    private HashMap<String, List<SharedMessage>> posts = new HashMap<>();
    private List<SharedMessageListener> mListeners = new CopyOnWriteArrayList<>();

    public SharedThreadPool(List<SharedThread> threads) {
        this.threads = threads;
        for(SharedThread t : threads) {
            t.getSession().addMessageListener((e) -> {
                if(posts.containsKey(e.getData().getParent())) {
                    List<SharedMessage> clones = new ArrayList<>();
                    // Put self in list of clones
                    SharedMessage message = new SharedMessage(t, e.getId());
                    posts.put(e.getId(), clones);
                    clones.add(message);
                    mListeners.forEach((l) -> l.onMessage(new SharedMessageEvent(message, this)));
                    // Post clones and add to list
                    for(SharedMessage cloneParent : posts.get(e.getData().getParent())) {
                        if(!cloneParent.getThread().equals(t)) {
                            cloneParent.replyAs(e.getContent(), e.getSenderNick()).thenAccept((clone) -> {
                                posts.put(clone.getId(), clones);
                                clones.add(new SharedMessage(cloneParent.getThread(), clone.getId()));
                            });
                        }
                    }
                }
            });
        }
    }

    public void broadcast(Send message) {
        if(!posts.containsKey(message.getParent())) throw new IllegalArgumentException("Message is not inside thread.");
        List<SharedMessage> clones = new ArrayList<>();
        for(SharedThread t : threads) {
            t.sendMessage(message).thenAccept((e) -> {
                posts.put(e.getData().getId(), clones);
                clones.add(new SharedMessage(t, e.getData().getId()));
            });
        }
    }

    public void addMessageListener(SharedMessageListener l) {
        mListeners.add(l);
    }

    public void removeMessageListener(SharedMessageListener l) {
        mListeners.remove(l);
    }
}
