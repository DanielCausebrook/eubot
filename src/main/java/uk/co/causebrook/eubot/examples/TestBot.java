package uk.co.causebrook.eubot.examples;

import uk.co.causebrook.eubot.*;
import uk.co.causebrook.eubot.events.*;
import uk.co.causebrook.eubot.packets.commands.Login;
import uk.co.causebrook.eubot.packets.events.BounceEvent;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.relay.SharedMessageThread;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
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


        StandardBehaviour annoyBot = new StandardBehaviour("AnnoyBot", "You're the one who needs help.");
        ScheduledExecutorService ex = Executors.newScheduledThreadPool(1);
        annoyBot.enableKill("Rude.", e -> ex.shutdownNow());
        annoyBot.addMessageListener(new RegexListener("^!annoy @([\\S]+)$", (e,m) -> {
            e.getSession().requestUsersByName(m.group(1), "\\s").thenAccept(e2 -> {
                if(e2.isEmpty()) {
                    e.reply("Couldn't find user " + m.group(1) + ", sorry.");
                } else {
                    for(int i = 0; i < 30; i+=5) ex.schedule(() -> accountRoom.initPM(e2.get(0)), i, TimeUnit.SECONDS);
                    e.reply("Annoying " + m.group(1) + " for 20 minutes.");
                }
            });
        }));


        Behaviour tauBot = new StandardBehaviour("TauBot", "Hi, I'm @TauBot. I'll be doing various things as TauNeutrin0 works on his new bot library. Stay tuned!");
        tauBot.addMessageListener(new RegexListener("^chirp!?$", ((e -> {
            if(e.getSenderNick().equals("bbb")) e.reply("chirp!");
        }))));


        StandardBehaviour pmBot = new StandardBehaviour("PmBot", "Hi, I'm @PmBot. I'm a test of TauNeutrin0's new bot PM abilities. Type !pm @User to initiate a private message with them.");
        pmBot.enableKill("/me exits.");


        Behaviour cGBot = new CardGameBot(accountRoom);
        pmBot.addMessageListener(new RegexListener("^!pm((?: @[\\S]+)+)$",
                (e, m) -> new Thread(() -> {
                    try {
                        String usersStr = m.group(1);
                        String[] users = usersStr.split(" @");
                        List<CompletableFuture<Session>> futPms = new ArrayList<>();
                        for(int i = 1; i < users.length; i++) {
                            List<SessionView> matchingUsers = e.getSession().requestUsersByName(users[i], "\\s").get();
                            if(matchingUsers.isEmpty()) {
                                e.reply("Could not find user " + users[i] + ".");
                                return;
                            }
                            futPms.add(accountRoom.initPM(matchingUsers.get(0)));
                        }
                        futPms.add(accountRoom.initPM(e.getData().getSender()));
                        List<Session> pms = new ArrayList<>();
                        for(CompletableFuture<Session> fut : futPms) {
                            Session pm = fut.get();
                            pm.setNick("PmBot");
                            pm.open();
                            pms.add(pm);
                        }
                        SharedMessageThread t = SharedMessageThread.openPoolWithMessage(pms, "New PM with: @" + e.getSenderNick().replaceAll("\\s", "") + " " + usersStr + ". Any replies to this thread will be sent privately.");

                        t.addMessageListener(sM -> {
                            if(sM.getMessages().get(0).getData().getContent().equals("!close")){
                                sM.reply("Closing PM...");
                                t.getRoot().reply("PM will now be closed. New replies will not be shared.");
                                t.stop();
                                for(Session pm : pms) {
                                    try {
                                        pm.close();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                        });
                        t.start();
                        e.reply("Opened PM.");
                    } catch(InterruptedException | ExecutionException | IOException e1) {
                        e.reply("Failed to create and join PM rooms.");
                    }
                }).start()
        ));


        String room = "test";
        CookieConfig cookie = new CookieConfig("cookie.txt");
        Session tauRoom = EuphoriaSession.getRoom(room, cookie);
        Session annoyRoom = EuphoriaSession.getRoom(room, cookie);
        Session pmRoom = EuphoriaSession.getRoom(room, cookie);
        Session cGRoom = EuphoriaSession.getRoom(room, cookie);
        cGBot.add(cGRoom);
        tauBot.add(tauRoom);
        annoyBot.add(annoyRoom);
        pmBot.add(pmRoom);
        pmBot.add(accountRoom);
        cGRoom.open();
        accountRoom.open();
        tauRoom.open();
        annoyRoom.open();
        pmRoom.open();

        Thread.sleep(Duration.ofDays(1).toMillis());
    }

}