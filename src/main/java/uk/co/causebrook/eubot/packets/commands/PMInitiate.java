package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.Data;

public class PMInitiate extends Data {
    private String user_id;

    public PMInitiate(String userId) {
        user_id = userId;
    }

    public String getUserId() { return user_id; }
}
