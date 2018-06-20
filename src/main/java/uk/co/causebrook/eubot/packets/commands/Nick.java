package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class Nick extends Data {
    private final String name;

    public Nick(String nick) { name=nick; }

    public String getName() { return name; }
}
