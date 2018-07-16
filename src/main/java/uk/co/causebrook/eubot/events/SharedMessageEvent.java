package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.SharedMessage;
import uk.co.causebrook.eubot.SharedThreadPool;

public class SharedMessageEvent {
    private SharedMessage message;
    private SharedThreadPool pool;

    public SharedMessageEvent(SharedMessage message, SharedThreadPool pool) {
        this.message = message;
        this.pool = pool;
    }

    public SharedMessage getMessage() {
        return message;
    }

    public SharedThreadPool getPool() {
        return pool;
    }
}
