package uk.co.causebrook.eubot.packets.commands;

import uk.co.causebrook.eubot.packets.Data;

@SuppressWarnings("unused")
public class Auth extends Data {
    private final String type;
    private final String passcode;

    public Auth(String passcode) {
        type="passcode";
        this.passcode=passcode;
    }

    public String getAuthType() { return type;     }
    public String getPasscode() { return passcode; }
}
