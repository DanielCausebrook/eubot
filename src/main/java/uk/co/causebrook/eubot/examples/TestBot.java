package uk.co.causebrook.eubot.examples;

import uk.co.causebrook.eubot.*;
import uk.co.causebrook.eubot.events.RegexListener;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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



        Behaviour tauBot = new StandardBehaviour("TauBot", "Hi, I'm @TauBot. I'll be doing various things as TauNeutrin0 works on his new bot library. Stay tuned!");
        tauBot.addMessageListener(new RegexListener("^chirp!?$", (((e, m) -> {
            if(m.matches() && e.getSenderNick().equals("bbb")) e.reply("chirp!");
        }))));



        Behaviour pmBot = new StandardBehaviour("PmBot", "Hi, I'm @PmBot. I'm a test of TauNeutrin0's new bot PM abilities. Type !pm @User to initiate a private message with them.");
        Behaviour cGBot = new CardGameBot(accountRoom);
        pmBot.addMessageListener(new RegexListener("^!pm((?: @[\\S]+)+)$",
                (e, m) -> new Thread(() -> {
                    try {
                        String usersStr = m.group(1);
                        String[] users = usersStr.split(" @");
                        List<CompletableFuture<Session>> futPms = new ArrayList<>();
                        for(int i = 1; i < users.length; i++) {
                            List<SessionView> matchingUsers = e.getSession().getUsersByName(users[i], "\\s").get();
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
        CookieConfig cookie = new CookieConfig("cookie.txt");
        Session tauRoom = EuphoriaSession.getRoom("test", cookie);
        Session pmRoom = EuphoriaSession.getRoom("test", cookie);
        Session cGRoom = EuphoriaSession.getRoom("test", cookie);
        cGBot.add(cGRoom);
        tauBot.add(tauRoom);
        pmBot.add(pmRoom);
        pmBot.add(accountRoom);
        cGRoom.open();
        accountRoom.open();
        tauRoom.open();
        pmRoom.open();

        Thread.sleep(Duration.ofDays(1).toMillis());
    }

}