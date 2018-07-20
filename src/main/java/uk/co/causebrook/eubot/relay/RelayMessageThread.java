package uk.co.causebrook.eubot.relay;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.packets.commands.Nick;
import uk.co.causebrook.eubot.packets.commands.Send;
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

    CompletableFuture<RelayMessage> sendMessage(String message, RelayMessage parent) {
        if(!messages.containsValue(parent)) throw new IllegalArgumentException("Cannot send message outside of thread.");
        long[] stamp = new long[1];
        return CompletableFuture.runAsync(() -> stamp[0] = nickLock.readLock())
                .thenCompose((v) ->  room.send(new Send(message, parent.getData().getId())))
                .thenApply((e) -> {
                    RelayMessage m = new RelayMessage(this, parent, e.getData());
                    messages.put(e.getData().getId(), m);
                    return m;
                })
                .whenComplete((e, err) -> nickLock.unlockRead(stamp[0]));
    }

    CompletableFuture<RelayMessage> sendMessageAs(String message, RelayMessage parent, String nick) {
        if(!messages.containsValue(parent)) throw new IllegalArgumentException("Cannot send message outside of thread.");
        long[] stamp = new long[1];
        String[] currNick = new String[1];
        return CompletableFuture.runAsync(() -> {
                stamp[0] = nickLock.writeLock();
                currNick[0] = room.getNick();
        })
                .thenCompose((v) -> room.send(new Nick(nick)))
                .thenCompose((e) -> room.send(new Send(message, parent.getData().getId())))
                .thenApply((e) -> {
                    RelayMessage m = new RelayMessage(this, parent, e.getData());
                    messages.put(e.getData().getId(), m);
                    return m;
                })
                .whenComplete((e, err) -> room.send(new Nick(currNick[0]))
                        .whenComplete((e2, err2) -> nickLock.unlockWrite(stamp[0])));
    }

    public RelayMessage getRoot() {
        return root;
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
