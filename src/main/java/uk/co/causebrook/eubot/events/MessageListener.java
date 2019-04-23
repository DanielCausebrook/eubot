package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.packets.events.SendEvent;

@FunctionalInterface
public interface MessageListener {
    void onPacket(MessageEvent<?> e);
}
