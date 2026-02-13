package de.t14d3.rapunzellib.runtime;

public final class RuntimeProfiles {
    public static final RuntimeProfile SERVER_STANDARD = RuntimeProfile.of(
        RuntimeCapability.ATTACHMENTS,
        RuntimeCapability.COMMANDS,
        RuntimeCapability.ENTITIES,
        RuntimeCapability.EVENTS,
        RuntimeCapability.GUI,
        RuntimeCapability.INVENTORY,
        RuntimeCapability.NBT,
        RuntimeCapability.WORLDS,
        RuntimeCapability.BLOCKS
    );

    public static final RuntimeProfile PROXY_STANDARD = RuntimeProfile.of(
        RuntimeCapability.ATTACHMENTS
    );

    private RuntimeProfiles() {
    }
}
