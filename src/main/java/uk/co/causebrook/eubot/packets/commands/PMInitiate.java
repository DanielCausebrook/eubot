package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.PMInitiateReply;

public class PMInitiate extends ReplyableData<PMInitiateReply> {
    private String user_id;

    public PMInitiate(String userId) {
        user_id = userId;
    }

    public String getUserId() { return user_id; }

    @Override
    public Class<PMInitiateReply> getReplyClass() {
        return PMInitiateReply.class;
    }
}
