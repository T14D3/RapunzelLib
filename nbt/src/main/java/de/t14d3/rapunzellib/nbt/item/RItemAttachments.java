package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.attachments.RAttachmentKey;
import de.t14d3.rapunzellib.attachments.RAttachmentScope;
import de.t14d3.rapunzellib.attachments.AttachmentStorageSupport;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.nbt.attachments.NbtAttachmentValueMapper;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

final class RItemAttachments {
    static final String ROOT_KEY = "rapunzellib:attachments";

    private RItemAttachments() {
    }

    static @NotNull AttachmentStorageSupport support() {
        return AttachmentStorageSupport.PERSISTENT_ONLY;
    }

    static boolean supports(@NotNull RAttachmentKey<?> key) {
        return support().supports(Objects.requireNonNull(key, "key").scope());
    }

    static <T> @NotNull Optional<T> get(@NotNull RItem item, @NotNull RAttachmentKey<T> key) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(key, "key");
        if (!supports(key)) {
            return Optional.empty();
        }
        RNbtValue value = attachments(item.customData()).get(key.id().asString()).orElse(null);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(decode(key, value));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    static <T> @NotNull RItem with(@NotNull RItem item, @NotNull RAttachmentKey<T> key, @NotNull T value) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(key, "key");
        if (!supports(key)) {
            throw new UnsupportedOperationException("Items only support persistent attachments");
        }
        RNbtCompound attachments = attachments(item.customData()).put(key.id().asString(), encode(key, value));
        return item.withCustomData(write(item.customData(), attachments));
    }

    static @NotNull RItem without(@NotNull RItem item, @NotNull RAttachmentKey<?> key) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(key, "key");
        if (!supports(key)) {
            return item;
        }
        RNbtCompound attachments = attachments(item.customData()).remove(key.id().asString());
        return item.withCustomData(write(item.customData(), attachments));
    }

    static <T> @NotNull RItemBuilder with(@NotNull RItemBuilder builder, @NotNull RAttachmentKey<T> key, @NotNull T value) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(key, "key");
        if (!supports(key)) {
            throw new UnsupportedOperationException("Items only support persistent attachments");
        }
        RNbtCompound attachments = attachments(builder.data).put(key.id().asString(), encode(key, value));
        builder.customData(write(builder.data.get(RItemFields.CUSTOM_DATA).orElse(RNbtCompound.empty()), attachments));
        return builder;
    }

    static @NotNull RItemBuilder without(@NotNull RItemBuilder builder, @NotNull RAttachmentKey<?> key) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(key, "key");
        if (!supports(key)) {
            return builder;
        }
        RNbtCompound attachments = attachments(builder.data.get(RItemFields.CUSTOM_DATA).orElse(RNbtCompound.empty())).remove(key.id().asString());
        builder.customData(write(builder.data.get(RItemFields.CUSTOM_DATA).orElse(RNbtCompound.empty()), attachments));
        return builder;
    }

    private static @NotNull RNbtCompound attachments(@NotNull RNbtCompound customData) {
        return customData.get(ROOT_KEY).map(RNbtValue::asCompound).orElse(RNbtCompound.empty());
    }

    private static @NotNull RNbtCompound write(@NotNull RNbtCompound customData, @NotNull RNbtCompound attachments) {
        return attachments.isEmpty() ? customData.remove(ROOT_KEY) : customData.put(ROOT_KEY, attachments);
    }

    private static <T> @NotNull RNbtValue encode(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        return NbtAttachmentValueMapper.encode(key, value);
    }

    private static <T> @NotNull T decode(@NotNull RAttachmentKey<T> key, @NotNull RNbtValue value) {
        return NbtAttachmentValueMapper.decode(key, value);
    }
}
