package uk.co.causebrook.eubot.examples;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.StandardBehaviour;
import uk.co.causebrook.eubot.events.RegexListener;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.relay.SharedMessageThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PmBot extends StandardBehaviour {
    public PmBot(Session accountRoom) {
        super("PmBot", "Hi, I'm @PmBot. I'm a test of TauNeutrin0's new bot PM abilities. Type !pm @User to initiate a private message with them.");
        enableKill("/me exits.");

        addMessageListener(new RegexListener("^!pm((?: @[\\S]+)+)$",
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
                        SharedMessageThread t = SharedMessageThread.openPoolWithMessage(pms, "New PM with: @" + e.getSenderNick().replaceAll("\\s", "") + " " + usersStr + ". Any replies to this thread will be sent privately.");;
                        t.getThreads().forEach(thread -> thread.addMessageListener(t::shareMessage));
                        t.addMessageListener(sM -> {
                            if(sM.getMessages().get(0).getContent().equals("!close")){
                                sM.reply("Closing PM...");
                                t.getRoot().reply("PM will now be closed. New replies will not be shared.");
                                for(Session pm : pms) {
                                    try {
                                        pm.close();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                        });
                        e.reply("Opened PM.");
                    } catch(InterruptedException | ExecutionException | IOException e1) {
                        e.reply("Failed to create and join PM rooms.");
                    }
                }).start()
        ));
    }
}
