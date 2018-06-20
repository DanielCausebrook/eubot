package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class GetMessage extends Data {
    private final String id;

    public GetMessage(String id) { this.id=id; }

    public String getId() { return id; }
}
