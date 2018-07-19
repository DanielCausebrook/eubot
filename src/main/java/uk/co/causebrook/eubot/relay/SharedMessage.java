package uk.co.causebrook.eubot.relay;

import java.util.ArrayList;
import java.util.List;

public class SharedMessage {
    private List<Message> messages;
    private SharedMessageThread thread;
    private SharedMessage parent;

    public SharedMessage(List<Message> messages, SharedMessage parent, SharedMessageThread thread) {
        this.messages = messages;
        this.parent = parent;
        this.thread = thread;
    }

    public SharedMessage reply(String message) {
        List<Message> replies = new ArrayList<>();
        SharedMessage sharedMessage = new SharedMessage(replies, this, thread);
        for(Message m : messages) m.reply(message).thenAccept((clone) -> {
            replies.add(clone);
            thread.registerSharedMessage(clone, sharedMessage);
        });
        return sharedMessage;
    }

    public SharedMessage replyAs(String message, String nick) {
        List<Message> replies = new ArrayList<>();
        SharedMessage sharedMessage = new SharedMessage(replies, this, thread);
        for(Message m : messages) m.replyAs(message, nick).thenAccept((clone) -> {
            replies.add(clone);
            thread.registerSharedMessage(clone, sharedMessage);
        });
        return sharedMessage;
    }

    SharedMessage shareChild(Message child) {
        if(!messages.contains(child.getParent())) throw new IllegalArgumentException("Message is not a child.");
        List<Message> childrenList = new ArrayList<>();
        SharedMessage sharedMessage = new SharedMessage(childrenList, this, thread);
        childrenList.add(child);
        thread.registerSharedMessage(child, sharedMessage);
        for(Message m : messages) {
            if(!m.equals(child.getParent())) {
                m.replyAs(child.getData().getContent(), child.getData().getSender().getName())
                        .thenAccept((clone) -> {
                            childrenList.add(clone);
                            thread.registerSharedMessage(clone, sharedMessage);
                        });
            }
        }
        return sharedMessage;
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
}
