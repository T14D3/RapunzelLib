package de.t14d3.rapunzellib.common.attachments;

import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.nbt.RNbtByteArray;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtPrimitive;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * YAML-backed store for persistent attachment roots on platforms without a native persistence API.
 *
 * <p>The payload intentionally mirrors what {@code NbtAttachmentValueMapper} emits for attachments:
 * primitive values and byte arrays at the top level of an {@link RNbtCompound}. More complex NBT
 * shapes are rejected instead of being silently truncated.</p>
 *
 * <p>Thread safety is provided by synchronizing on the underlying {@link YamlConfig} instance.</p>
 */
public final class YamlPersistentAttachmentStore implements AutoCloseable {
    private static final String BYTES_KEY = "$bytes";

    private final Logger logger;
    private final YamlConfig config;

    public YamlPersistentAttachmentStore(
        @NotNull Logger logger,
        @NotNull ConfigService configService,
        @NotNull Path file
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(configService, "configService");
        Objects.requireNonNull(file, "file");
        this.config = configService.load(file, "");
    }

    public @NotNull RNbtCompound get(@NotNull String path) {
        Objects.requireNonNull(path, "path");
        synchronized (config) {
            return readCompound(config.get(path));
        }
    }

    public void put(@NotNull String path, @NotNull RNbtCompound root) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(root, "root");
        synchronized (config) {
            config.set(path, root.isEmpty() ? null : writeCompound(root));
            config.save();
        }
    }

    @Override
    public void close() {
        try {
            synchronized (config) {
                config.save();
            }
        } catch (Exception e) {
            logger.debug("Failed to flush YAML persistent attachment store", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull RNbtCompound readCompound(@Nullable Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return RNbtCompound.empty();
        }
        RNbtCompound out = RNbtCompound.empty();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            RNbtValue value = readValue(entry.getValue());
            if (value != null) {
                out = out.put(String.valueOf(entry.getKey()), value);
            }
        }
        return out;
    }

    private static @Nullable RNbtValue readValue(@Nullable Object raw) {
        if (raw instanceof Map<?, ?> map && map.size() == 1 && map.get(BYTES_KEY) instanceof String base64) {
            return new RNbtByteArray(Base64.getDecoder().decode(base64));
        }
        if (raw instanceof String value) return RNbtPrimitive.ofString(value);
        if (raw instanceof Byte value) return RNbtPrimitive.ofByte(value);
        if (raw instanceof Short value) return RNbtPrimitive.ofShort(value);
        if (raw instanceof Integer value) return RNbtPrimitive.ofInt(value);
        if (raw instanceof Long value) return RNbtPrimitive.ofLong(value);
        if (raw instanceof Float value) return RNbtPrimitive.ofFloat(value);
        if (raw instanceof Double value) return RNbtPrimitive.ofDouble(value);
        if (raw instanceof Boolean value) return RNbtPrimitive.ofBoolean(value);
        if (raw instanceof Number value) return RNbtPrimitive.ofDouble(value.doubleValue());
        return null;
    }

    private static @NotNull Map<String, Object> writeCompound(@NotNull RNbtCompound root) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        root.asMap().forEach((key, value) -> out.put(key, writeValue(value)));
        return out;
    }

    private static @NotNull Object writeValue(@NotNull RNbtValue value) {
        return switch (value) {
            case RNbtByteArray byteArray -> Map.of(BYTES_KEY, Base64.getEncoder().encodeToString(byteArray.value()));
            case RNbtPrimitive primitive -> primitive.value();
            default -> throw new IllegalArgumentException("Unsupported YAML attachment payload type " + value.type());
        };
    }
}
