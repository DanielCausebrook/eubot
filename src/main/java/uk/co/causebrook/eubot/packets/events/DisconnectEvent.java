package uk.co.causebrook.eubot.packets.events;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class DisconnectEvent extends Data {
    private String reason;

    public String getReason() { return reason; }
}
