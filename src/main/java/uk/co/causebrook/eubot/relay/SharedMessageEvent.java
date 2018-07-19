package uk.co.causebrook.eubot.relay;

public class SharedMessageEvent {
    private SharedMessage message;
    private String content;
    private SharedMessageThread pool;

    public SharedMessageEvent(SharedMessage message, String content, SharedMessageThread pool) {
        this.message = message;
        this.content = content;
        this.pool = pool;
    }

    public SharedMessage getMessage() {
        return message;
    }

    public String getContent() {
        return content;
    }

    public SharedMessageThread getPool() {
        return pool;
    }
}
