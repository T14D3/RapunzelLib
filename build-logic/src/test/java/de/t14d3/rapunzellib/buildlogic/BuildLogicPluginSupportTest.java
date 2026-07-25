package de.t14d3.rapunzellib.buildlogic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildLogicPluginSupportTest {
    @Test
    void fabricMappingsCutoffStillIncludesOldMinecraftVersions() {
        assertTrue(BuildLogicPluginSupport.requiresOfficialMojangMappings("1.21.11"));
        assertTrue(BuildLogicPluginSupport.requiresOfficialMojangMappings("1.21.10"));
    }

    @Test
    void fabricMappingsCutoffExcludesModernMinecraftVersions() {
        assertFalse(BuildLogicPluginSupport.requiresOfficialMojangMappings("26.1.2"));
        assertFalse(BuildLogicPluginSupport.requiresOfficialMojangMappings("1.21.12"));
    }
}
