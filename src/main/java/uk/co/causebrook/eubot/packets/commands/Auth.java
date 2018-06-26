package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.AuthReply;

@SuppressWarnings("unused")
public class Auth extends ReplyableData<AuthReply> {
    private final String type;
    private final String passcode;

    public Auth(String passcode) {
        type="passcode";
        this.passcode=passcode;
    }

    public String getAuthType() { return type;     }
    public String getPasscode() { return passcode; }

    @Override
    public Class<AuthReply> getReplyClass() {
        return AuthReply.class;
    }
}
