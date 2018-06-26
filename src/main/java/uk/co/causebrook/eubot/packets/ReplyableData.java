package uk.co.causebrook.eubot.packets;

public abstract class ReplyableData<T extends Data> extends Data {
    public abstract Class<T> getReplyClass();
}
