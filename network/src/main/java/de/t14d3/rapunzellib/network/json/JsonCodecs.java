package de.t14d3.rapunzellib.network.json;

import com.google.gson.Gson;

/**
 * Holder for shared default JSON codec instances.
 */
public final class JsonCodecs {
    private static final Gson DEFAULT_GSON = new Gson();
    private static final JsonCodec DEFAULT_CODEC = new GsonJsonCodec(DEFAULT_GSON);

    private JsonCodecs() {
    }

    /**
     * Returns the default Gson instance.
     *
     * @return the default Gson
     */
    public static Gson gson() {
        return DEFAULT_GSON;
    }

    /**
     * Returns the default JSON codec.
     *
     * @return the default codec
     */
    public static JsonCodec codec() {
        return DEFAULT_CODEC;
    }
}

