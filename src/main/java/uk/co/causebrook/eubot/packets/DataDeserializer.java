package uk.co.causebrook.eubot.packets;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class DataDeserializer implements JsonDeserializer<Packet> {
    private static final Gson gson = new Gson();
    private static final DataDeserializer instance = new DataDeserializer();

    private DataDeserializer() {}

    public static DataDeserializer get() {
        return instance;
    }

    private Type resolveType(String type) {
        return TypeToken.getParameterized(Packet.class, DataTypeAdapter.get().getClass(type)).getType();
    }

    @Override
    public Packet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String type = json.getAsJsonObject().get("type").getAsString();
        try {
            return gson.fromJson(json, resolveType(type));
        } catch(NullPointerException e) {
            throw new JsonParseException("Unknown packet type: \"" + type + "\".");
        }
    }
}
