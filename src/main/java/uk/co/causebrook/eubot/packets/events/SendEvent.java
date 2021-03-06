package uk.co.causebrook.eubot.packets.events;

import uk.co.causebrook.eubot.packets.Data;
import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.commands.GetMessage;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.fields.SessionView;

@SuppressWarnings("unused")
public class SendEvent extends Data {
    private String      id;
    private String      parent;
    private String      previous_edit_id;
    private int         time;
    private SessionView sender;
    private String      content;
    private String      encryption_key_id;
    private int         edited;
    private int         deleted;
    private boolean     truncated;

    public GetMessage createFullMessageRequest() {
        return new GetMessage(id);
    }

    public String      getId()              { return id;                }
    public String      getParent()          { return parent;            }
    public String      getPreviousEditId()  { return previous_edit_id;  }
    public int         getTime()            { return time;              }
    public SessionView getSender()          { return sender;            }
    public String      getContent()         { return content;           }
    public String      getEncryptionKeyId() { return encryption_key_id; }
    public int         getEdited()          { return edited;            }
    public int         getDeleted()         { return deleted;           }
    public boolean     getTruncated()       { return truncated;         }
}
