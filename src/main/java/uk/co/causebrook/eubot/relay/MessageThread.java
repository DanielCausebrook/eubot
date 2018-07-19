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

public class MessageThread {
    private Session room;
    private Message root;
    private Map<String, Message> messages = new HashMap<>();
    private StampedLock nickLock = new StampedLock();
    private List<MessageListener> mListeners = new CopyOnWriteArrayList<>();

    public MessageThread(Session room, SendEvent root) {
        this.room = room;
        this.root = new Message(this, null, root);
        messages.put(root.getId(), this.root);
        room.addMessageListener((mE) -> {
            if(messages.containsKey(mE.getData().getParent())) {
                Message m = new Message(this, messages.get(mE.getData().getParent()), mE.getData());
                messages.put(mE.getId(), m);
                for(MessageListener l : mListeners) l.onMessage(m);
            }
        });
    }

    CompletableFuture<Message> sendMessage(String message, Message parent) {
        if(!messages.containsValue(parent)) throw new IllegalArgumentException("Cannot send message outside of thread.");
        long[] stamp = new long[1];
        return CompletableFuture.runAsync(() -> stamp[0] = nickLock.readLock())
                .thenCompose((v) ->  room.send(new Send(message, parent.getData().getId())))
                .thenApply((e) -> {
                    Message m = new Message(this, parent, e.getData());
                    messages.put(e.getData().getId(), m);
                    return m;
                })
                .whenComplete((e, err) -> nickLock.unlockRead(stamp[0]));
    }

    CompletableFuture<Message> sendMessageAs(String message, Message parent, String nick) {
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
                    Message m = new Message(this, parent, e.getData());
                    messages.put(e.getData().getId(), m);
                    return m;
                })
                .whenComplete((e, err) -> room.send(new Nick(currNick[0]))
                        .whenComplete((e2, err2) -> nickLock.unlockWrite(stamp[0])));
    }

    public Message getRoot() {
        return root;
    }

    public void addMessageListener(MessageListener listener) {
        mListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof MessageThread){
            MessageThread sT = (MessageThread) o;
            return sT.room.equals(room) && sT.root.equals(root);
        }
        return false;
    }
}
