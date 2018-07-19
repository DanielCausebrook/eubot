package uk.co.causebrook.eubot.relay;

@FunctionalInterface
public interface SharedMessageListener {
    void onMessage(SharedMessage sM);
}
