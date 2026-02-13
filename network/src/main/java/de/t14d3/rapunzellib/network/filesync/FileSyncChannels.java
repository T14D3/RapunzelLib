package de.t14d3.rapunzellib.network.filesync;

import de.t14d3.rapunzellib.network.runtime.NetworkTopic;

public final class FileSyncChannels {
    private FileSyncChannels() {
    }

    public static final String REQUEST = "rapunzellib:filesync:req";
    public static final String RESPONSE_META = "rapunzellib:filesync:res_meta";
    public static final String RESPONSE_CHUNK = "rapunzellib:filesync:res_chunk";
    public static final String INVALIDATE = "rapunzellib:filesync:invalidate";

    public static final NetworkTopic<FileSyncRequest> REQUEST_TOPIC = NetworkTopic.of(REQUEST, FileSyncRequest.class);
    public static final NetworkTopic<FileSyncResponseMeta> RESPONSE_META_TOPIC =
        NetworkTopic.of(RESPONSE_META, FileSyncResponseMeta.class);
    public static final NetworkTopic<FileSyncResponseChunk> RESPONSE_CHUNK_TOPIC =
        NetworkTopic.of(RESPONSE_CHUNK, FileSyncResponseChunk.class);
    public static final NetworkTopic<FileSyncInvalidate> INVALIDATE_TOPIC =
        NetworkTopic.of(INVALIDATE, FileSyncInvalidate.class);
}
