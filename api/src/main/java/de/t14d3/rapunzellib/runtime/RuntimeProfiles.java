package de.t14d3.rapunzellib.runtime;

/**
 * Pre-defined {@link RuntimeProfile} constants for standard server and proxy setups.
 */
public final class RuntimeProfiles {
    /**
     * Standard runtime profile for server platforms with all capabilities.
     */
    public static final RuntimeProfile SERVER_STANDARD = RuntimeProfile.of(
        RuntimeCapability.ATTACHMENTS,
        RuntimeCapability.COMMANDS,
        RuntimeCapability.ENTITIES,
        RuntimeCapability.EVENTS,
        RuntimeCapability.GUI,
        RuntimeCapability.INVENTORY,
        RuntimeCapability.NBT,
        RuntimeCapability.VISUALS,
        RuntimeCapability.WORLDS,
        RuntimeCapability.BLOCKS
    );

    /**
     * Standard runtime profile for proxy platforms with minimal capabilities.
     */
    public static final RuntimeProfile PROXY_STANDARD = RuntimeProfile.of(
        RuntimeCapability.ATTACHMENTS
    );

    private RuntimeProfiles() {
    }
}
