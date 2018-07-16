package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.PacketEvent;
import uk.co.causebrook.eubot.packets.commands.Nick;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.replies.SendReply;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;

public class SharedThread {
    private Session room;
    private String rootId;
    private StampedLock nickLock;

    public SharedThread(Session room, String rootId) {
        this.room = room;
        this.rootId = rootId;
    }

    public CompletableFuture<PacketEvent<SendReply>> sendMessage(Send message) {
        long[] stamp = new long[1];
        return CompletableFuture.runAsync(() -> stamp[0] = nickLock.readLock())
                .thenCompose((v) -> room.send(message))
                .whenComplete((e, err) -> nickLock.unlockRead(stamp[0]));
    }

    public CompletableFuture<PacketEvent<SendReply>> sendMessageAs(Send message, String nick) {
        long[] stamp = new long[1];
        String[] currNick = new String[1];
        return CompletableFuture.runAsync(() -> {
                stamp[0] = nickLock.writeLock();
                currNick[0] = room.getNick();
        })
                .thenCompose((v) -> room.send(new Nick(nick)))
                .thenCompose((e) -> room.send(message))
                .whenComplete((e, err) -> room.send(new Nick(currNick[0]))
                        .whenComplete((e2, err2) -> nickLock.unlockWrite(stamp[0])));
    }

    public String getRootId() {
        return rootId;
    }

    public Session getSession() {
        return room;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SharedThread){
            SharedThread sT = (SharedThread) o;
            return sT.room.equals(room) && sT.rootId.equals(rootId);
        }
        return false;
    }
}
