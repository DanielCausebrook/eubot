package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.MessageListener;
import uk.co.causebrook.eubot.events.SessionListener;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.events.SendEvent;
import uk.co.causebrook.eubot.packets.fields.SessionView;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;

public interface Session extends Connection {
    void setNick(String nick);
    String getNick();

    void sendMessage(Send message);
    void sendMessageWithReplyListener(Send message, MessageListener replyListener);
    void sendMessageWithReplyListener(Send message, MessageListener replyListener, Duration timeout);

    void addMessageReplyListener(SendEvent message, MessageListener replyListener);
    void addMessageReplyListener(SendEvent message, MessageListener replyListener, Duration timeout);

    void addSessionListener(SessionListener listener);
    void removeSessionListener(SessionListener listener);

    void addMessageListener(MessageListener listener);
    void removeMessageListener(MessageListener listener);

    Future<Session> initPM(String userId);
    Future<Session> initPM(SessionView user);

    Future<List<SessionView>> getUsersByName(String name, String regexIgnored);
}
