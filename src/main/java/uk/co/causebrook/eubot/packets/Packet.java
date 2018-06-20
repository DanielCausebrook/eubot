package uk.co.causebrook.eubot.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class Packet<T extends Data> {
    private String  id;
    private String  type;
    private T       data;
    private String  error;
    private boolean throttled;
    private String  throttled_reason;
    private static final Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(Packet.class, DataDeserializer.get());
        gson = builder.create();
    }

    public Packet(T data) {
        this.data = data;
        type = DataTypeAdapter.get().getType(data.getClass());
    }

    public Packet(T data, String id) {
        this(data);
        this.id = id;
    }

    public T getData() {
        return data;
    }

    // Safe because Packet's type always extends data.
    @SuppressWarnings("unchecked")
    public static Packet<? extends Data> fromJson(String json) throws JsonParseException {
        return gson.fromJson(json, Packet.class);
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
