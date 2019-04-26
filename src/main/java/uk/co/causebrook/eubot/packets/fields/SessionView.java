package uk.co.causebrook.eubot.packets.fields;

@SuppressWarnings("unused")
public class SessionView {
    private String id;
    private String name;
    private String server_id;
    private String server_era;
    private String session_id;
    private boolean is_staff;
    private boolean is_manager;
    private String client_address;
    private String real_client_address;

    public String getId()                { return id;                  }
    public String getName()              { return name;                }
    public String getServerId()          { return server_id;           }
    public String getServerEra()         { return server_era;          }
    public String getSessionId()         { return session_id;          }
    public boolean getIsStaff()           { return is_staff;            }
    public boolean getIsManager()         { return is_manager;          }
    public String getClientAddress()     { return client_address;      }
    public String getRealClientAddress() { return real_client_address; }
}
