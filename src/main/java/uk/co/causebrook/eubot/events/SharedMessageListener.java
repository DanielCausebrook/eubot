package uk.co.causebrook.eubot.events;

@FunctionalInterface
public interface SharedMessageListener {
    void onMessage(SharedMessageEvent e);
}
