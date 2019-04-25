package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.ConnectionListener;
import uk.co.causebrook.eubot.events.RegexListener;
import uk.co.causebrook.eubot.packets.events.SnapshotEvent;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.regex.Pattern;

public class StandardBehaviour extends Behaviour {
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
        addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect(Connection c) {

            }

            @Override
            public void onDisconnect(Connection c) {

            }

            @Override
            public void onError(Connection c, Throwable err) {

            }
        });
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
