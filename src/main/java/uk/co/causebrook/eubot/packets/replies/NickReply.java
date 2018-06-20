package uk.co.causebrook.eubot.packets.replies;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class NickReply extends Data {
    private String session_id;
    private String id;
    private String from;
    private String to;

    public String getSessionId() { return session_id; }
    public String getId()        { return id;         }
    public String getFrom()      { return from;       }
    public String getTo()        { return to;         }
}
