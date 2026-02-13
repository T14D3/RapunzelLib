package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

public interface RAttachmentCodec<T> {
    byte @NotNull [] encode(@NotNull T value);

    @NotNull T decode(byte @NotNull [] bytes);
}
