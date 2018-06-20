package uk.co.causebrook.eubot.packets.events;

import uk.co.causebrook.eubot.packets.Data;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class BounceEvent extends Data {
    private String reason;
    private ArrayList<String> auth_options;
    private String agent_id;
    private String ip;

    /*public void attemptPasscode(WebsocketConnection room, String passcode, ReplyEventListener evtLst) {
        room.sendPacket(new Auth("passcode",passcode).createPacket(),evtLst);
    }*/

    public String            getReason()      { return reason;       }
    public ArrayList<String> getAuthOptions() { return auth_options; }
    public String            getAgentId()     { return agent_id;     }
    public String            getIp()          { return ip;           }
}
