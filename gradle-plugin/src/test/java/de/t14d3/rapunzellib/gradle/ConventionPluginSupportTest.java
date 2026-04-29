package de.t14d3.rapunzellib.gradle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConventionPluginSupportTest {
    @Test
    void fabricMappingsCutoffStillIncludesOldMinecraftVersions() {
        assertTrue(ConventionPluginSupport.requiresOfficialMojangMappings("1.21.11"));
        assertTrue(ConventionPluginSupport.requiresOfficialMojangMappings("1.21.10"));
    }

    @Test
    void fabricMappingsCutoffExcludesModernMinecraftVersions() {
        assertFalse(ConventionPluginSupport.requiresOfficialMojangMappings("26.1.2"));
        assertFalse(ConventionPluginSupport.requiresOfficialMojangMappings("1.21.12"));
    }
}
