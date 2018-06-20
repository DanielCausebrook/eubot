package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class Log extends Data {
    private final int    n;
    private final String before;

    public Log(int n, String before) {
        this.n = n;
        this.before = before;
    }

    public int getN()         { return n;      }
    public String getBefore() { return before; }
}
