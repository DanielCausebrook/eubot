package uk.co.causebrook.eubot.examples;

import uk.co.causebrook.eubot.CookieConfig;
import uk.co.causebrook.eubot.EuphoriaSession;
import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.StandardBehaviour;
import uk.co.causebrook.eubot.events.RegexListener;
import uk.co.causebrook.eubot.events.RoomBounceEvent;
import uk.co.causebrook.eubot.events.SessionEvent;
import uk.co.causebrook.eubot.events.SessionListener;
import uk.co.causebrook.eubot.packets.events.DisconnectEvent;
import uk.co.causebrook.eubot.packets.events.SnapshotEvent;
import uk.co.causebrook.eubot.relay.RelayMessageThread;
import uk.co.causebrook.eubot.relay.SharedMessageThread;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PortalBot extends StandardBehaviour {

    public PortalBot(CookieConfig cookie) {
        super("PortalBot", "Use \"!portal &test\" to open a portal to that room!\n" +
                "Use \"!close\" to close the portal again.\n" +
                "Unless you're portalling into &test, only hosts may use this bot. " +
                "This is to prevent disruption to other communities. Sorry!\n" +
                "Created by @TauNeutrin0");
        addMessageListener(new RegexListener("^!portal &([a-z]+)$", (e, m) -> {
            if(e.getData().getSender().getIsManager() ||
                    e.getSenderId().equals("account:010uy6bgfzqio") ||
                    m.group(1).equals("test")) {
                try {
                    Session room = EuphoriaSession.getRoom(m.group(1), cookie);
                    room.setNick("PortalBot");
                    room.open();
                    String botHomeId = e.getSession().getSessionView().getId();
                    var wrapper = new Object(){ String botAwayId; boolean open = false; };
                    room.addSessionListener(new SessionListener() {
                        @Override
                        public void onJoin(SessionEvent<SnapshotEvent> e) {
                            wrapper.botAwayId = room.getSessionView().getId();
                        }
                        @Override public void onBounce(RoomBounceEvent e) {}
                        @Override public void onDisconnect(SessionEvent<DisconnectEvent> e) {}
                    });
                    int random = new Random().nextInt(10000);
                    e.reply("To open the portal into &" + m.group(1) + ", use the command \"!portal " + random + "\" in that room.");
                    room.addMessageListener(new RegexListener("^!portal ([\\d]{4})$", (e2, m2) -> {
                        if(!wrapper.open &&
                                e2.getSenderId().equals(e.getSenderId()) &&
                                m2.group(1).equals(Integer.toString(random))) {
                            wrapper.open = true;
                            List<RelayMessageThread> threads = new ArrayList<>();
                            threads.add(new RelayMessageThread(e.getSession(), e.getData()));
                            threads.add(new RelayMessageThread(room, e2.getData()));

                            SharedMessageThread sharedThread = new SharedMessageThread(threads);
                            sharedThread.addMessageListener(message -> {
                                if(message.getMessages().get(0).getContent().equals("!close")) {
                                    try {
                                        sharedThread.getRoot().reply("Closing the portal!");
                                        room.close();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            });
                            threads.forEach(t -> t.addMessageListener(message -> {
                                String sender = message.getSenderId();
                                if(!sender.equals(botHomeId) && !sender.equals(wrapper.botAwayId)) sharedThread.shareMessage(message);
                            }));
                            sharedThread.getRoot().reply("Opened the portal!");
                        }
                    }));
                } catch (URISyntaxException | IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                e.reply("You must be a host to use this bot.\n" +
                        "This is to prevent disruption to other communities. Sorry!");
            }
        }));
    }
}
