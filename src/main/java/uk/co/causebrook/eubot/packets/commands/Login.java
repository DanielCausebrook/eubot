package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class Login extends Data {
    private String namespace;
    private String id;
    private String password;

    public Login(String email, String password) {
        namespace = "email";
        id = email;
        this.password = password;
    }

    public String getNamespace() { return namespace; }
    public String getId()        { return id;        }
    // No password getter for security reasons.
}
