package uk.co.causebrook.eubot.examples;

import uk.co.causebrook.eubot.Session;
import uk.co.causebrook.eubot.StandardBehaviour;
import uk.co.causebrook.eubot.events.RegexListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnnoyBot extends StandardBehaviour {
    private ScheduledExecutorService ex = Executors.newScheduledThreadPool(1);
    public AnnoyBot(Session accountRoom) {
        super("AnnoyBot", "You're the one who needs help.");
        enableKill(e -> {
            e.reply("Rude.");
            ex.shutdownNow();
        });
        addMessageListener(new RegexListener("^!annoy @([\\S]+)$", (e, m) -> {
            e.getSession().requestUsersByName(m.group(1), "\\s").thenAccept(e2 -> {
                if(e2.isEmpty()) {
                    e.reply("Couldn't find user " + m.group(1) + ", sorry.");
                } else {
                    for(int i = 0; i < 30; i+=5) ex.schedule(() -> accountRoom.initPM(e2.get(0)), i, TimeUnit.SECONDS);
                    e.reply("Annoying " + m.group(1) + " for 20 minutes.");
                }
            });
        }));
    }
}
