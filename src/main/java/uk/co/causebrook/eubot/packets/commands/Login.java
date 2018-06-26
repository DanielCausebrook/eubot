package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.ReplyableData;
import uk.co.causebrook.eubot.packets.replies.LoginReply;

@SuppressWarnings("unused")
public class Login extends ReplyableData<LoginReply> {
    private String namespace;
    private String id;
    private String password;

    public Login(String email, String password) {
        namespace = "email";
        id = email;
        this.password = password;
    }

    // No password getter for security reasons.
    public String getNamespace() { return namespace; }
    public String getId()        { return id;        }

    @Override
    public Class<LoginReply> getReplyClass() {
        return LoginReply.class;
    }
}
