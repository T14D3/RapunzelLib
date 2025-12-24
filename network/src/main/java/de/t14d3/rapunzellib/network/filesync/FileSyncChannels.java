package de.t14d3.rapunzellib.network.filesync;

public final class FileSyncChannels {
    private FileSyncChannels() {
    }

    public static final String REQUEST = "rapunzellib:filesync:req";
    public static final String RESPONSE_META = "rapunzellib:filesync:res_meta";
    public static final String RESPONSE_CHUNK = "rapunzellib:filesync:res_chunk";
    public static final String INVALIDATE = "rapunzellib:filesync:invalidate";
}

