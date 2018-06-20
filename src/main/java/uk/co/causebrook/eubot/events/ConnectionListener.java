package uk.co.causebrook.eubot.events;

import uk.co.causebrook.eubot.Connection;

public interface ConnectionListener {

    void onConnect(Connection c);
    void onDisconnect(Connection c);
    void onError(Connection c, Throwable err);
}
