package uk.co.causebrook.eubot.packets.events;

import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.fields.PersonalAccountView;
import uk.co.causebrook.eubot.packets.fields.SessionView;

@SuppressWarnings("unused")
public class HelloEvent extends Data {
    private String              id;
    private PersonalAccountView account;
    private SessionView session;
    private boolean             account_has_access;
    private boolean             account_email_verified;
    private boolean             room_is_private;
    private String              version;

    public String              getId()                   { return id;                     }
    public PersonalAccountView getAccount()              { return account;                }
    public SessionView         getSession()              { return session;                }
    public boolean             getAccountHasAccess()     { return account_has_access;     }
    public boolean             getAccountEmailVerified() { return account_email_verified; }
    public boolean             getRoomIsPrivate()        { return room_is_private;        }
    public String              getVersion()              { return version;                }
}
