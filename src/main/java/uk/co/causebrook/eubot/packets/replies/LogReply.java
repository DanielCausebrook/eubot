package uk.co.causebrook.eubot.packets.replies;

import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.fields.Message;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class LogReply extends Data {
    ArrayList<Message> log;
    String             before;

    public ArrayList<Message> getLog()    { return log;    }
    public String             getBefore() { return before; }
}
