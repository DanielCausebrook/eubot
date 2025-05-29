package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.ConnectionListener;
import uk.co.causebrook.eubot.events.MessageListener;
import uk.co.causebrook.eubot.events.RegexListener;
import uk.co.causebrook.eubot.packets.events.SnapshotEvent;

import jakarta.websocket.CloseReason;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class StandardBehaviour extends Behaviour {
    private boolean killEnabled = false;
    private String killMessage = null;
    private MessageListener beforeKill;
    private Logger logger;

    public StandardBehaviour(String nick, String helpText) {
        super(nick);
        String quotedNick = Pattern.quote(nick.replace(" ", ""));
        addMessageListener(new RegexListener("^!help @" + quotedNick + "$", (e) -> e.reply(helpText)));
        HashMap<Connection, LocalDateTime> roomUptimes = new HashMap<>();
        final LocalDateTime uptime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        addPacketListener(SnapshotEvent.class,
                e -> roomUptimes.put(e.getConnection(), LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        );
        addMessageListener(new RegexListener("^!uptime @" + quotedNick + "$", (e) -> {
            e.reply("/me has been up since " + uptime.toString());
            LocalDateTime roomUptime = roomUptimes.get(e.getConnection());
            if(roomUptime != null) e.reply("/me has been online in this room since " + roomUptime.toString());
        }));
        addMessageListener(new RegexListener("^!ping( @" + quotedNick + ")?$", (e) -> e.reply("Pong!")));
        addMessageListener(new RegexListener("^!kill @" + quotedNick + "$", (e, m) -> {
            if(killEnabled) {
                if(beforeKill != null) beforeKill.onPacket(e);
                if(killMessage != null) e.reply(killMessage);
                try {
                    e.getSession().close();
                } catch (IOException exc) {
                    if(logger != null) logger.log(Level.WARNING, "Error when exiting room from !kill command.", exc);
                }
            }
        }));
        addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect(Connection c) {

            }

            @Override
            public void onDisconnect(Connection c, CloseReason closeReason) {

            }

            @Override
            public void onError(Connection c, Throwable err) {
                if(logger != null) logger.log(Level.SEVERE, "An exception has occurred in " + getNick() + ".", err);
            }
        });
    }

    public void enableKill(String killMessage) {
        killEnabled = true;
        this.killMessage = killMessage;
    }

    public void enableKill(MessageListener beforeKill) {
        killEnabled = true;
        this.beforeKill = beforeKill;
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    // TODO Add room switching.
    // Must keep track of connected room names somehow.

//    public StandardBehaviour addRoomSwitching() {
//        String quoteNick = Pattern.quote(getNick.replace(" ", ""))
//        addMessageListener(new RegexListener("^!goto @" + quoteNick + " &([a-z]+)$", (e, m) -> {
//
//        }));
//    }
}
