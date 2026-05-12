package de.t14d3.rapunzellib.network.filesync;

/**
 * Role of a server in file synchronization.
 */
public enum FileSyncRole {
    /**
     * The authoritative source of files.
     */
    AUTHORITY,
    /**
     * A follower that syncs from the authority.
     */
    FOLLOWER
}

