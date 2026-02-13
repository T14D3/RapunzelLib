package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface AttachmentMapAccess {
    @NotNull Map<RAttachmentKey<?>, Object> transientEntries();
}
