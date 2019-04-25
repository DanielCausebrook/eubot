package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.Connection;

import javax.websocket.CloseReason;

public interface ConnectionListener {
    void onConnect(Connection c);
    void onDisconnect(Connection c, CloseReason closeReason);
    void onError(Connection c, Throwable err);
}
