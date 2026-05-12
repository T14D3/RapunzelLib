package de.t14d3.rapunzellib.network.json;

import java.lang.reflect.Type;

/**
 * Abstraction for JSON serialization and deserialization.
 */
public interface JsonCodec {
    /**
     * Serializes an object to a JSON string.
     *
     * @param value the object to serialize
     * @return the JSON string
     */
    String toJson(Object value);

    /**
     * Deserializes a JSON string to an object of the given class.
     *
     * @param json the JSON string
     * @param type the target class
     * @param <T> the target type
     * @return the deserialized object
     */
    <T> T fromJson(String json, Class<T> type);

    /**
     * Deserializes a JSON string to an object of the given type.
     *
     * @param json the JSON string
     * @param type the target type
     * @param <T> the target type
     * @return the deserialized object
     */
    <T> T fromJson(String json, Type type);
}

