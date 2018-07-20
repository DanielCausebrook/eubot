package uk.co.causebrook.eubot.relay;

@FunctionalInterface
public interface RelayMessageListener {
    void onMessage(RelayMessage m);
}
