package uk.co.causebrook.eubot.relay;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.packets.events.SendEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;

public class RelayMessageThread {
    private Session room;
    private RelayMessage root;
    private Map<String, RelayMessage> messages = new HashMap<>();
    private StampedLock nickLock = new StampedLock();
    private List<RelayMessageListener> mListeners = new CopyOnWriteArrayList<>();

    public RelayMessageThread(Session room, SendEvent root) {
        this.room = room;
        this.root = new RelayMessage(this, null, root);
        messages.put(root.getId(), this.root);
        room.addMessageListener((mE) -> {
            if(messages.containsKey(mE.getData().getParent())) {
                RelayMessage m = new RelayMessage(this, messages.get(mE.getData().getParent()), mE.getData());
                messages.put(mE.getId(), m);
                for(RelayMessageListener l : mListeners) l.onMessage(m);
            }
        });
    }

    CompletableFuture<RelayMessage> reply(String message, RelayMessage parent) {
        if(!messages.containsValue(parent)) throw new IllegalArgumentException("Cannot send message outside of thread.");
        return room.reply(message, parent.getData())
                .thenApply((e) -> {
                    RelayMessage m = new RelayMessage(this, parent, e.getData());
                    messages.put(e.getData().getId(), m);
                    return m;
                });
    }

    CompletableFuture<RelayMessage> replyAs(String message, String nick, RelayMessage parent) {
        if(!messages.containsValue(parent)) throw new IllegalArgumentException("Cannot send message outside of thread.");
        return room.replyAs(message, nick, parent.getData())
                .thenApply((e) -> {
                    RelayMessage m = new RelayMessage(this, parent, e.getData());
                    messages.put(e.getData().getId(), m);
                    return m;
                });
    }

    public RelayMessage getRoot() {
        return root;
    }

    public Session getSession() {
        return room;
    }

    public void addMessageListener(RelayMessageListener listener) {
        mListeners.add(listener);
    }

    public void removeMessageListener(RelayMessageListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof RelayMessageThread){
            RelayMessageThread sT = (RelayMessageThread) o;
            return sT.room.equals(room) && sT.root.equals(root);
        }
        return false;
    }
}
