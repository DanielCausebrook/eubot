package uk.co.causebrook.eubot.packets.replies;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class AuthReply extends Data {
    private boolean success;
    private String  reason;

    public boolean getSuccess() { return success; }
    public String  getReason()  { return reason;  }
}
