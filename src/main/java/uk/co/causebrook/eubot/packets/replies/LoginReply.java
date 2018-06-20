package uk.co.causebrook.eubot.packets.replies;

import uk.co.causebrook.eubot.packets.Data;

public class LoginReply extends Data {
    private boolean success;
    private String  reason;
    private String  account_id;

    public boolean getSuccess()   { return success;    }
    public String  getReason()    { return reason;     }
    public String  getAccountId() { return account_id; }
}
