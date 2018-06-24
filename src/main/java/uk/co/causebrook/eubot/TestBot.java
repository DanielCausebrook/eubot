package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.*;
import uk.co.causebrook.eubot.packets.commands.*;
import uk.co.causebrook.eubot.packets.events.BounceEvent;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.packets.replies.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("SpellCheckingInspection")
public class TestBot {
    public static void main(String[] args) throws Exception /*cos I'm lazy*/ {
        Logger logger = Logger.getLogger("test-log");
        CookieConfig normalCookie = new CookieConfig("cookie.txt");
        CookieConfig accountCookie = new CookieConfig("accountCookie.txt");

        Session accountRoom = EuphoriaSession.getRoom("toolboxbots", accountCookie);
        accountRoom.setNick("TauBot");
        accountRoom.addPacketListener(BounceEvent.class, e -> accountRoom.sendWithReplyListener(
                new Login("tauneutrin00@gmail.com", "c5Cc#uqk8QGY#UUN"),
                LoginReply.class, e2 -> {
                    if(e2.getData().getSuccess()) logger.info("Logged into account");
                    else logger.severe("Unable to login to account.");
                })
        );
        accountRoom.open();

        Behaviour pmBot = new StandardBehaviour("TauBot", "Hi, I'm @TauBot. I'll be doing various things as TauNeutrin0 works on his new bot library. Stay tuned!");
        pmBot.addMessageListener(new RegexListener("^!pm @([\\w]+)$",
                (e, m) -> new Thread(
                        () -> initPMConnection(e.getData().getSender(), m.group(1), e.getSession(), accountRoom, accountCookie)
                ).start()
        ));

        Session room = EuphoriaSession.getRoom("test", normalCookie);
        pmBot.add(room);
        room.open();
        Thread.sleep(Duration.ofHours(2).toMillis());
    }

    private static String initPMConnection(SessionView initiator, String targetName, Session room, Session accountRoom, CookieConfig accountCookie) {
        try {
            CompletableFuture<List<SessionView>> futMatchingUsers = new CompletableFuture<>();
            room.sendWithReplyListener(new Who(), WhoReply.class, e2 -> {
                List<SessionView> sessions = e2.getData().getListing();
                futMatchingUsers.complete(
                        sessions.stream()
                                .filter(sV -> targetName.equals(sV.getName().replaceAll("\\W", "")))
                                .collect(Collectors.toList())
                );
            });
            final List<SessionView> matchingUsers = futMatchingUsers.get();
            if (!matchingUsers.isEmpty()) {
                SessionView[] user = new SessionView[2];
                user[0] = initiator;
                user[1] = matchingUsers.get(0);
                Session[] pm = new Session[2];

                CompletableFuture<Session> s0 = new CompletableFuture<>();
                CompletableFuture<Session> s1 = new CompletableFuture<>();
                accountRoom.sendWithReplyListener(new PMInitiate(user[0].getId()),
                        PMInitiateReply.class, e2 -> openPM(accountCookie, s0, e2.getData().getPmId()));
                accountRoom.sendWithReplyListener(new PMInitiate(user[1].getId()),
                        PMInitiateReply.class, e2 -> openPM(accountCookie, s1, e2.getData().getPmId()));
                pm[0] = s0.get();
                pm[1] = s1.get();
                Map<String, String> threadMessages = new HashMap<>();
                CompletableFuture<String> root0 = new CompletableFuture<>();
                CompletableFuture<String> root1 = new CompletableFuture<>();
                pm[0].sendWithReplyListener(new Send("New PM with " + user[1].getName() + ". Any reply to this thread will be sent privately."),
                        SendReply.class, e2 -> root0.complete(e2.getData().getId()));
                pm[1].sendWithReplyListener(new Send("New PM with " + user[0].getName() + ". Any reply to this thread will be sent privately."),
                        SendReply.class, e2 -> root1.complete(e2.getData().getId()));
                String[] rootMessage = new String[]{root0.get(), root1.get()};
                pm[0].setNick(user[1].getName());
                pm[1].setNick(user[0].getName());
                threadMessages.put(rootMessage[0], rootMessage[1]);
                threadMessages.put(rootMessage[1], rootMessage[0]);
                addExchangeListener(pm[0], pm[1], threadMessages);
                addExchangeListener(pm[1], pm[0], threadMessages);
                return "PM opened successfully";
            } else {
                return "Could not find user " + targetName + ".";
            }
        } catch (InterruptedException | ExecutionException err) {
            return "Failed to create and join PM rooms.";
        }

    }

    private static void openPM(CookieConfig accountCookie, CompletableFuture<Session> futSession, String pmId) {
        try {
            Session s = EuphoriaSession.getPM(pmId, accountCookie);
            s.setNick("TauBot");
            s.open();
            futSession.complete(s);
        } catch (URISyntaxException | IOException e1) {
            futSession.completeExceptionally(e1);
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
