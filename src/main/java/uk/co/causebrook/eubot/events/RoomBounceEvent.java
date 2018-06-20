package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.EuphoriaSession;
import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.commands.Auth;
import uk.co.causebrook.eubot.packets.events.BounceEvent;
import uk.co.causebrook.eubot.packets.replies.AuthReply;

public class RoomBounceEvent extends PacketEvent<BounceEvent> {
    private PacketListener<AuthReply> listener;

    public RoomBounceEvent(EuphoriaSession connection, Packet<BounceEvent> packet) {
        super(connection, packet);
    }

    /**
     * Attempts a login to the room using a passcode.
     * @param passcode The passcode to attempt.
     */
    public void attemptLogin(String passcode) {
        if(listener == null) {
            getRoomConnection().send(new Auth(passcode));
        } else {
            getRoomConnection().sendWithReplyListener(new Auth(passcode), AuthReply.class, e -> {
                if (!e.getData().getSuccess()) listener.onPacket(e);
            });
        }
    }

    /**
     * Sets the listener to be executed if the login fails.
     * Only one listener may be added, any subsequent calls will overwrite the previous.
     * The listener must be added before attempting a login.
     * @param listener The listener to add to the login attempt.
     */
    public void setLoginFailedListener(PacketListener<AuthReply> listener) {
        this.listener = listener;
    }
}
