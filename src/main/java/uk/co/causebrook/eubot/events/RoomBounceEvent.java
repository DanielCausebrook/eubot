package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.EuphoriaSession;
import uk.co.causebrook.eubot.packets.Packet;
import uk.co.causebrook.eubot.packets.commands.Auth;
import uk.co.causebrook.eubot.packets.events.BounceEvent;
import uk.co.causebrook.eubot.packets.replies.AuthReply;

public class RoomBounceEvent extends PacketEvent<BounceEvent> {
    private PacketListener<AuthReply> failHandler;
    private PacketListener<AuthReply> successHandler;

    public RoomBounceEvent(EuphoriaSession connection, Packet<BounceEvent> packet) {
        super(connection, packet);
    }

    /**
     * Attempts a login to the room using a passcode.
     * @param passcode The passcode to attempt.
     */
    public void attemptLogin(String passcode) {
        if(failHandler == null) {
            getConnection().send(new Auth(passcode));
        } else {
            getConnection().send(new Auth(passcode))
                    .thenAccept(e -> {
                        if (e.getData().getSuccess()) successHandler.onPacket(e);
                        else failHandler.onPacket(e);
                    });
        }
    }

    /**
     * Sets the handler to be executed if the login fails.
     * Only one handler may be added, any subsequent calls will replace it.
     * The handler must be added before attempting a login.
     * @param listener The handler to add to the login attempt.
     */
    public void setLoginFailHandler(PacketListener<AuthReply> listener) {
        this.failHandler = listener;
    }

    /**
     * Sets the handler to be executed if the login succeeds.
     * Only one handler may be added, any subsequent calls will replace it.
     * The handler must be added before attempting a login.
     * @param listener The handler to add to the login attempt.
     */
    public void setLoginSuccessHandler(PacketListener<AuthReply> listener) {
        this.successHandler = listener;
    }
}
