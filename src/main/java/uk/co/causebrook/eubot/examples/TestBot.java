package uk.co.causebrook.eubot.examples;

import uk.co.causebrook.eubot.*;
import uk.co.causebrook.eubot.events.*;
import uk.co.causebrook.eubot.packets.commands.Login;
import uk.co.causebrook.eubot.packets.commands.Nick;
import uk.co.causebrook.eubot.packets.events.BounceEvent;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.relay.SharedMessageThread;

import javax.websocket.CloseReason;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("SpellCheckingInspection")
public class TestBot {

    public static void main(String[] args) throws Exception /*cos I'm lazy*/ {
        Logger logger = Logger.getLogger("test-log");

        String password = Files.readAllLines(Paths.get("password.txt")).get(0);

        Session accountRoom = EuphoriaSession.getRoom("toolboxbots", new CookieConfig("accountCookie.txt"));
        accountRoom.addPacketListener(BounceEvent.class,
            e -> accountRoom.send(new Login("tauneutrin00@gmail.com", password))
                .thenAccept(e2 -> {
                    if(e2.getData().getSuccess()) logger.info("Logged into account");
                    else logger.severe("Unable to login to account.");
                })
        );
        accountRoom.open();

        String room = "test";
        CookieConfig cookie = new CookieConfig("cookie.txt");

        StandardBehaviour tauBot = new StandardBehaviour("TauBot", "Hi, I'm @TauBot. I'll be doing various things as TauNeutrin0 works on his new bot library. Stay tuned!");
        tauBot.enableLogging(logger);
        Session tauRoom = EuphoriaSession.getRoom(room, cookie);
        tauBot.add(tauRoom);

        Behaviour pmBot = new PmBot(accountRoom);
        Session pmRoom = EuphoriaSession.getRoom(room, cookie);
        pmBot.add(pmRoom);

        StandardBehaviour cGBot = new CardGameBot(accountRoom);
        cGBot.enableLogging(logger);
        Session cGRoom = EuphoriaSession.getRoom(room, cookie);
        cGBot.add(cGRoom);

        Behaviour annoyBot = new AnnoyBot(accountRoom);
        Session annoyRoom = EuphoriaSession.getRoom(room, cookie);
        annoyBot.add(annoyRoom);

        tauRoom.open();
        cGRoom.open();
        annoyRoom.open();
        pmRoom.open();

        Thread.sleep(Duration.ofDays(1).toMillis());
    }

}