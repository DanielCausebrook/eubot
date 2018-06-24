package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.*;
import uk.co.causebrook.eubot.packets.commands.*;
import uk.co.causebrook.eubot.packets.events.BounceEvent;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.packets.replies.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

@SuppressWarnings("SpellCheckingInspection")
public class TestBot {
    public static void main(String[] args) throws Exception /*cos I'm lazy*/ {
        Logger logger = Logger.getLogger("test-log");

        Session accountRoom = EuphoriaSession.getRoom("toolboxbots", new CookieConfig("accountCookie.txt"));
        accountRoom.addPacketListener(BounceEvent.class, e -> accountRoom.sendWithReplyListener(
                new Login("tauneutrin00@gmail.com", "c5Cc#uqk8QGY#UUN"),
                LoginReply.class, e2 -> {
                    if(e2.getData().getSuccess()) logger.info("Logged into account");
                    else logger.severe("Unable to login to account.");
                })
        );

        Behaviour pmBot = new StandardBehaviour("TauBot", "Hi, I'm @TauBot. I'll be doing various things as TauNeutrin0 works on his new bot library. Stay tuned!");
        pmBot.addMessageListener(new RegexListener("^!pm @([\\w]+)$",
                (e, m) -> new Thread(
                        () -> e.reply(initPMConnection(e.getData().getSender(), m.group(1), e.getSession(), accountRoom))
                ).start()
        ));

        CookieConfig cookie = new CookieConfig("cookie.txt");
        Session room = EuphoriaSession.getRoom("test", cookie);
        pmBot.add(room);
        pmBot.add(accountRoom);
        accountRoom.open();
        room.open();

        Thread.sleep(Duration.ofHours(2).toMillis());
    }

    private static String initPMConnection(SessionView initiator, String targetName, Session room, Session accountRoom) {
        try {
            // Get users
            List<SessionView> matchingUsers = room.getUsersByName(targetName, "\\s").get();
            if(matchingUsers.isEmpty()) return "Could not find user " + targetName + ".";

            // Open PMs
            SessionView[] user = new SessionView[] { initiator, matchingUsers.get(0)};
            Session[] pm = new Session[] { accountRoom.initPM(user[0]).get(), accountRoom.initPM(user[1]).get() };
            pm[0].open();
            pm[1].open();

            //Set nick and wait for confirmation.
            Future nick0 = pm[0].sendWithReplyListener(new Nick("TauBot"), NickReply.class);
            Future nick1 = pm[1].sendWithReplyListener(new Nick("TauBot"), NickReply.class);
            nick0.get();
            nick1.get();

            // Add relay logic.
            Map<String, String> threadMessages = new HashMap<>();
            String initMsg = "New PM with %s. Any reply to this thread will be sent privately.";
            Future<PacketEvent<SendReply>> root0 = pm[0].sendWithReplyListener(new Send(String.format(initMsg, user[1].getName())), SendReply.class);
            Future<PacketEvent<SendReply>> root1 = pm[1].sendWithReplyListener(new Send(String.format(initMsg, user[0].getName())), SendReply.class);
            String[] rootMessage = new String[] { root0.get().getData().getId(), root1.get().getData().getId() };
            pm[0].setNick(user[1].getName());
            pm[1].setNick(user[0].getName());
            threadMessages.put(rootMessage[0], rootMessage[1]);
            threadMessages.put(rootMessage[1], rootMessage[0]);
            addExchangeListener(pm[0], pm[1], threadMessages);
            addExchangeListener(pm[1], pm[0], threadMessages);
            return "PM opened.";
        } catch (InterruptedException | ExecutionException | IOException err) {
            return "Failed to create and join PM rooms.";
        }
    }

    private static void addExchangeListener(Session pm1, Session pm2, Map<String, String> threadMessages) {
        pm1.addMessageListener(e1 -> {
            if(threadMessages.containsKey(e1.getData().getParent())) {
                pm2.sendWithReplyListener(new Send(e1.getContent(), threadMessages.get(e1.getData().getParent())), SendReply.class, e2 -> {
                    threadMessages.put(e1.getId(), e2.getData().getId());
                    threadMessages.put(e2.getData().getId(), e1.getId());
                });
            }
        });
    }
}