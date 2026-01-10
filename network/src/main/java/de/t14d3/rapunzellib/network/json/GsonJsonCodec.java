package de.t14d3.rapunzellib.network.json;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.Objects;

public final class GsonJsonCodec implements JsonCodec {
    private final Gson gson;

    public GsonJsonCodec(Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    @Override
    public String toJson(Object value) {
        return gson.toJson(value);
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    @Override
    public <T> T fromJson(String json, Type type) {
        @SuppressWarnings("unchecked")
        T parsed = gson.fromJson(json, type);
        return parsed;
    }
}

