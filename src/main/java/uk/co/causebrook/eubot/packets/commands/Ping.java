package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class Ping extends Data {
    private final int time;

    public Ping(int time) { this.time=time; }

    public int getTime() { return time; }
}
