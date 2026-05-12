package de.t14d3.rapunzellib.network.filesync;

import de.t14d3.rapunzellib.network.runtime.NetworkTopic;

/**
 * Channel and topic constants for the file synchronization subsystem.
 */
public final class FileSyncChannels {
    private FileSyncChannels() {
    }

    /** Channel for file sync requests. */
    public static final String REQUEST = "rapunzellib:filesync:req";
    /** Channel for file sync response metadata. */
    public static final String RESPONSE_META = "rapunzellib:filesync:res_meta";
    /** Channel for file sync response data chunks. */
    public static final String RESPONSE_CHUNK = "rapunzellib:filesync:res_chunk";
    /** Channel for file sync invalidation signals. */
    public static final String INVALIDATE = "rapunzellib:filesync:invalidate";

    /** Typed topic for file sync requests. */
    public static final NetworkTopic<FileSyncRequest> REQUEST_TOPIC = NetworkTopic.of(REQUEST, FileSyncRequest.class);
    /** Typed topic for file sync response metadata. */
    public static final NetworkTopic<FileSyncResponseMeta> RESPONSE_META_TOPIC =
        NetworkTopic.of(RESPONSE_META, FileSyncResponseMeta.class);
    /** Typed topic for file sync response data chunks. */
    public static final NetworkTopic<FileSyncResponseChunk> RESPONSE_CHUNK_TOPIC =
        NetworkTopic.of(RESPONSE_CHUNK, FileSyncResponseChunk.class);
    /** Typed topic for file sync invalidation signals. */
    public static final NetworkTopic<FileSyncInvalidate> INVALIDATE_TOPIC =
        NetworkTopic.of(INVALIDATE, FileSyncInvalidate.class);
}
