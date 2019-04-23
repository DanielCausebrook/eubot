package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.MessageEvent;
import uk.co.causebrook.eubot.events.MessageListener;
import uk.co.causebrook.eubot.events.PacketEvent;
import uk.co.causebrook.eubot.events.SessionListener;
import uk.co.causebrook.eubot.packets.commands.GetMessage;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.events.SendEvent;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.packets.replies.GetMessageReply;
import uk.co.causebrook.eubot.packets.replies.SendReply;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Represents a euphoria session
 */
public interface Session extends Connection {
    CompletionStage<Void> setNick(String nick);
    String getNick();

    CompletableFuture<MessageEvent<SendReply>> send(String message);

    CompletableFuture<MessageEvent<SendReply>> reply(String message, SendEvent parent);

    CompletableFuture<MessageEvent<SendReply>> reply(String message, String parentId);

    CompletableFuture<MessageEvent<SendReply>> sendAs(String message, String nick);

    CompletableFuture<MessageEvent<SendReply>> replyAs(String message, String nick, SendEvent parent);

    CompletableFuture<MessageEvent<SendReply>> replyAs(String message, String nick, String parentId);

    default CompletableFuture<PacketEvent<GetMessageReply>> requestFullMessage(String messageId) {
        return send(new GetMessage(messageId));
    }

    default CompletableFuture<PacketEvent<GetMessageReply>> requestFullMessage(SendEvent message) {
        return send(new GetMessage(message));
    }

    void addMessageReplyListener(SendEvent message, MessageListener replyListener);
    void addMessageReplyListener(SendEvent message, MessageListener replyListener, Duration timeout);

    void addSessionListener(SessionListener listener);
    void removeSessionListener(SessionListener listener);

    void addMessageListener(MessageListener listener);
    void removeMessageListener(MessageListener listener);

    CompletableFuture<Session> initPM(String userId);
    CompletableFuture<Session> initPM(SessionView user);

    CompletableFuture<List<SessionView>> getUsersByName(String name, String regexIgnored);
}
