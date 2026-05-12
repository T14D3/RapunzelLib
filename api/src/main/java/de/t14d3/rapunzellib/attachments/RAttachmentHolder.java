package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

/**
 * Marks an object as capable of holding attachment data.
 */
public interface RAttachmentHolder {
    /**
     * Returns the attachment container for this holder.
     *
     * @return the attachment container
     */
    @NotNull RAttachmentContainer attachments();
}
