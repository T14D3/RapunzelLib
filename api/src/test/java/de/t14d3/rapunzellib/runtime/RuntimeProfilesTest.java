package de.t14d3.rapunzellib.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeProfilesTest {
    @Test
    void serverStandardBundlesSharedBackendCapabilities() {
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.ATTACHMENTS));
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.COMMANDS));
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.ENTITIES));
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.EVENTS));
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.GUI));
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.INVENTORY));
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.NBT));
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.WORLDS));
        assertTrue(RuntimeProfiles.SERVER_STANDARD.hasCapability(RuntimeCapability.BLOCKS));
    }

    @Test
    void proxyStandardKeepsProxyCapabilitySurfaceNarrow() {
        assertTrue(RuntimeProfiles.PROXY_STANDARD.hasCapability(RuntimeCapability.ATTACHMENTS));
        assertFalse(RuntimeProfiles.PROXY_STANDARD.hasCapability(RuntimeCapability.COMMANDS));
        assertFalse(RuntimeProfiles.PROXY_STANDARD.hasCapability(RuntimeCapability.WORLDS));
    }
}
