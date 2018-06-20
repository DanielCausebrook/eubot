package uk.co.causebrook.eubot.packets.replies;

import uk.co.causebrook.eubot.packets.Data;

public class PMInitiateReply extends Data {
    private String pm_id;
    private String to_nick;

    public String getPmId()   { return pm_id;   }
    public String getToNick() { return to_nick; }
}
