package uk.co.causebrook.eubot.relay;

@FunctionalInterface
public interface MessageListener {
    void onMessage(Message m);
}
