package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Provides access to the raw transient attachment entries map.
 *
 * <p>Used internally by {@link RAttachmentContainer} implementations to expose
 * transient data for serialization or iteration.</p>
 */
public interface AttachmentMapAccess {
    /**
     * Returns all transient attachment entries.
     *
     * @return a map of attachment keys to values
     */
    @NotNull Map<RAttachmentKey<?>, Object> transientEntries();
}
