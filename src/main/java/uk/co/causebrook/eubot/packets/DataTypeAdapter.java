package uk.co.causebrook.eubot.packets;

import uk.co.causebrook.eubot.packets.commands.*;
import uk.co.causebrook.eubot.packets.events.*;
import uk.co.causebrook.eubot.packets.replies.*;

import java.util.HashMap;
import java.util.Map;

public class DataTypeAdapter {
    private final Map<String, Class<? extends Data>> typeToClass = new HashMap<>();
    private final Map<Class<? extends Data>, String> classToType = new HashMap<>();
    private static final DataTypeAdapter instance = new DataTypeAdapter();

    static {
        instance.registerType(Auth.class, "auth");
        instance.registerType(GetMessage.class, "get-message");
        instance.registerType(Log.class, "log");
        instance.registerType(Nick.class, "nick");
        instance.registerType(Ping.class, "ping");
        instance.registerType(Send.class, "send");
        instance.registerType(Who.class, "who");
        instance.registerType(Login.class, "login");
        instance.registerType(Logout.class, "logout");
        instance.registerType(PMInitiate.class, "pm-initiate");
        instance.registerType(BounceEvent.class, "bounce-event");
        instance.registerType(DisconnectEvent.class, "disconnect-event");
        instance.registerType(HelloEvent.class, "hello-event");
        instance.registerType(JoinEvent.class, "join-event");
        instance.registerType(NickEvent.class, "nick-event");
        instance.registerType(PartEvent.class, "part-event");
        instance.registerType(PingEvent.class, "ping-event");
        instance.registerType(SendEvent.class, "send-event");
        instance.registerType(SnapshotEvent.class, "snapshot-event");
        instance.registerType(AuthReply.class, "auth-reply");
        instance.registerType(GetMessageReply.class, "get-message-reply");
        instance.registerType(LogReply.class, "log-reply");
        instance.registerType(NickReply.class, "nick-reply");
        instance.registerType(PingReply.class, "ping-reply");
        instance.registerType(SendReply.class, "send-reply");
        instance.registerType(WhoReply.class, "who-reply");
        instance.registerType(LoginReply.class, "login-reply");
        instance.registerType(LogoutReply.class, "logout-reply");
        instance.registerType(PMInitiateReply.class, "pm-initiate-reply");
    }

    private DataTypeAdapter() {}

    public static DataTypeAdapter get() {
        return instance;
    }
    
    public String getType(Class<? extends Data> clazz) {
        return classToType.get(clazz);
    }

    public Class<? extends Data> getClass(String type) {
        return typeToClass.get(type);
    }

    public void registerType(Class<? extends Data> clazz, String type) {
        classToType.put(clazz, type);
        typeToClass.put(type, clazz);
    }
}
