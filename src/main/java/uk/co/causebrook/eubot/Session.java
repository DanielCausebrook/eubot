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
import uk.co.causebrook.eubot.packets.replies.NickReply;
import uk.co.causebrook.eubot.packets.replies.SendReply;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Represents a euphoria session.
 *
 * Provides utilities to:
 * <ul>
 *     <li>Set the nick.</li>
 *     <li>Send and reply to messages.</li>
 *     <li>Send and reply to messages as another nick.</li>
 *     <li>Listen for any messages sent by other users.</li>
 *     <li>Monitor the session state.</li>
 * </ul>
 */
public interface Session extends Connection {


    SessionView getSessionView();

    /**
     * Sets the nick of the current session. If the session is not yet open, the stage will complete when the session is opened and the nick is updated.
     * @param nick The nick to change to.
     * @return A CompletableFuture that will return the server's response. Will complete exceptionally if there is an error or timeout.
     */
    CompletableFuture<PacketEvent<NickReply>> setNick(String nick);

    /**
     * Gets the current nick.
     * @return The nick currently in use.
     */
    String getNick();

    /**
     * Send a message in the room at the root level.
     * @param message The message content.
     * @return A CompletableFuture that will contain the server's response. Will complete exceptionally if there is an error or timeout.
     */
    CompletableFuture<MessageEvent<?>> send(String message);

    /**
     * Send a message in the room as a reply to another message.
     * @param message The message content.
     * @param parentId The id of the parent message to reply to.
     * @return A CompletableFuture that will contain the server's response. Will complete exceptionally if there is an error or timeout.
     */
    CompletableFuture<MessageEvent<?>> reply(String message, String parentId);

    /**
     * Send a message in the room as a reply to another message.
     * @param message The message content.
     * @param parent The parent message to reply to.
     * @return A CompletableFuture that will contain the server's response. Will complete exceptionally if there is an error or timeout.
     */
    default CompletableFuture<MessageEvent<?>> reply(String message, SendEvent parent) {
        return reply(message, parent.getId());
    }

    /**
     * Send a message in the room at the root level with a specified nick.
     * This will block all other message sends until the nick is reverted.
     * @param message The message content.
     * @param nick The temporary nick to send the message as.
     * @return A CompletableFuture that will contain the server's response. Will complete exceptionally if there is an error or timeout.
     */
    CompletableFuture<MessageEvent<?>> sendAs(String message, String nick);

    /**
     * Send a message in the room as a reply to another message with a specified nick.
     * This will block all other message sends until the nick is reverted.
     * @param message The message content.
     * @param nick The temporary nick to send the message as.
     * @param parentId The id of the parent message to reply to.
     * @return A CompletableFuture that will contain the server's response. Will complete exceptionally if there is an error or timeout.
     */
    CompletableFuture<MessageEvent<?>> replyAs(String message, String nick, String parentId);

    /**
     * Send a message in the room as a reply to another message with a specified nick.
     * This will block all other message sends until the nick is reverted.
     * @param message The message content.
     * @param nick The temporary nick to send the message as.
     * @param parent The parent message to reply to.
     * @return A CompletableFuture that will contain the server's response. Will complete exceptionally if there is an error or timeout.
     */
    default CompletableFuture<MessageEvent<?>> replyAs(String message, String nick, SendEvent parent) {
        return replyAs(message, nick, parent.getId());
    }

    /**
     * Send a request to get the full message content of a message which has been truncated.
     * @param messageId The id of the message to request.
     * @return A CompletableFuture that will contain the server's response. WIll complete exceptionally if there is an error or timeout.
     */
    default CompletableFuture<MessageEvent<?>> requestFullMessage(String messageId) {
        return send(new GetMessage(messageId)).thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    /**
     * Send a request to get the full message content of a message which has been truncated.
     * @param message The message to request a full version of.
     * @return A CompletableFuture that will contain the server's response. WIll complete exceptionally if there is an error or timeout.
     */
    default CompletableFuture<MessageEvent<?>> requestFullMessage(SendEvent message) {
        return send(new GetMessage(message)).thenApply(e -> new MessageEvent<>(this, e.getPacket()));
    }

    /**
     * Add a listener that is fired when replies to a specified message are received.
     * @param messageId The message to listen for replies to.
     * @param replyListener The listener to add.
     */
    void addMessageReplyListener(String messageId, MessageListener replyListener);

    /**
     * Add a listener that is fired when replies to a specified message are received.
     * @param message The message to listen for replies to.
     * @param replyListener The listener to add.
     */
    default void addMessageReplyListener(SendEvent message, MessageListener replyListener) {
        addMessageReplyListener(message.getId(), replyListener);
    }

    /**
     * Removes a message reply listener, if present.
     * @param messageId The id of the message the listener is listening for replies to.
     * @param replyListener The listener to remove.
     * @return true if the listener was present.
     */
    boolean removeMessageReplyListener(String messageId, MessageListener replyListener);

    /**
     * Removes a message reply listener, if present.
     * @param message The message the listener is listening for replies to.
     * @param replyListener The listener to remove.
     * @return true if the listener was present.
     */
    default boolean removeMessageReplyListener(SendEvent message, MessageListener replyListener) {
        return removeMessageReplyListener(message.getId(), replyListener);
    }

    /**
     * Add a listener which monitors the state of the session.
     * The listener is triggered on room join, bounce, and disconnect.
     * @param listener The listener to add.
     */
    void addSessionListener(SessionListener listener);

    /**
     * Remove a SessionListener, if present.
     * @param listener The listener to remove.
     * @return true if the listener was present.
     */
    boolean removeSessionListener(SessionListener listener);

    /**
     * Add a listener that is fired when any other user sends a message in the room.
     * @param listener The listener to add.
     */
    void addMessageListener(MessageListener listener);

    /**
     * Removes a MessageListener, if present.
     * @param listener The listener to remove.
     */
    boolean removeMessageListener(MessageListener listener);

    /**
     * Requests a PM room with another user, and returns a new Session for connecting to it.
     * @param userId The id of the user to request a PM with.
     * @return A CompletionStage that will contain the new PM Session ready to be started.
     */
    CompletableFuture<Session> initPM(String userId);

    /**
     * Requests a PM room with another user, and returns a new Session for connecting to it.
     * @param user The the user to request a PM with.
     * @return A CompletionStage that will contain the new PM Session ready to be started.
     */
    CompletableFuture<Session> initPM(SessionView user);

    /**
     * Gets a list of all users in the current room that have the specified nick.
     * A list of characters can be specified which will be ignored in both the input string and the requested nicks.
     * For example:
     *     requestUsersByName("Bot Bot", "\\s")
     *     will return any users named:
     *     "BotBot", "Bot Bot", "Bo  tB ot" etc.
     *     since any whitespace characters are ignored.
     * @param name The name to search for.
     * @param regexIgnored A regex matching any characters or groups that should be ignored.
     * @return A list of users which have the requested name.
     */
    CompletableFuture<List<SessionView>> requestUsersByName(String name, String regexIgnored);
}
