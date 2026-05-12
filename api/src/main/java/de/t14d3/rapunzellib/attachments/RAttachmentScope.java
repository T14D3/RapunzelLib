package de.t14d3.rapunzellib.attachments;

/**
 * Defines the persistence scope of an attachment.
 */
public enum RAttachmentScope {
    /** In-memory only, lost on restart. */
    TRANSIENT,
    /** Persisted to disk across restarts. */
    PERSISTENT,
}
