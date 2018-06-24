package uk.co.causebrook.eubot;

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
        addMessageListener(new RegexListener("^!help @" + quotedNick + "$", (e, m) -> e.reply(helpText)));
        HashMap<Connection, LocalDateTime> roomUptimes = new HashMap<>();
        final LocalDateTime uptime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        addPacketListener(SnapshotEvent.class,
                e -> roomUptimes.put(e.getRoomConnection(), LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        );
        addMessageListener(new RegexListener("^!uptime @" + quotedNick + "$", (e, m) -> {
            e.reply("/me has been up since " + uptime.toString());
            LocalDateTime roomUptime = roomUptimes.get(e.getRoomConnection());
            if(roomUptime != null) e.reply("/me has been online in this room since " + roomUptime.toString());
        }));
        addMessageListener(new RegexListener("^!ping( @" + quotedNick + ")?$", (e, m) -> e.reply("Pong!")));
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
