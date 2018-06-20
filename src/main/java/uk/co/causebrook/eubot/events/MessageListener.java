package uk.co.causebrook.eubot.events;

@FunctionalInterface
public interface MessageListener {
    void onPacket(MessageEvent e);
}
