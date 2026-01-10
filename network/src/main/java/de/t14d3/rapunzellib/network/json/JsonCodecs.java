package de.t14d3.rapunzellib.network.json;

import com.google.gson.Gson;

public final class JsonCodecs {
    private static final Gson DEFAULT_GSON = new Gson();
    private static final JsonCodec DEFAULT_CODEC = new GsonJsonCodec(DEFAULT_GSON);

    private JsonCodecs() {
    }

    public static Gson gson() {
        return DEFAULT_GSON;
    }

    public static JsonCodec codec() {
        return DEFAULT_CODEC;
    }
}

