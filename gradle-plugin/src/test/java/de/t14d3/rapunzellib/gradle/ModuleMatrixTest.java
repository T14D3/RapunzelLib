package de.t14d3.rapunzellib.gradle;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModuleMatrixTest {
    @Test
    void autoSharedCoreFamilyFollowsPlatformDefaults() {
        assertEquals(ModuleMatrix.SHARED_CORE_FAMILY_MOJANG, ModuleMatrix.normalizeSharedCoreFamily("auto", "fabric"));
        assertEquals(ModuleMatrix.SHARED_CORE_FAMILY_MOJANG, ModuleMatrix.normalizeSharedCoreFamily("auto", "paper"));
        assertEquals(ModuleMatrix.SHARED_CORE_FAMILY_NONE, ModuleMatrix.normalizeSharedCoreFamily("auto", "minestom"));
    }

    @Test
    void defaultSharedCoreFeaturesMatchCurrentShimLayout() {
        assertEquals(Set.of("events", "gui", "inventory", "nbt"), ModuleMatrix.defaultSharedCoreFeatures(ModuleMatrix.SHARED_CORE_FAMILY_MOJANG, "fabric"));
        assertEquals(Set.of("events", "gui", "inventory", "nbt"), ModuleMatrix.defaultSharedCoreFeatures(ModuleMatrix.SHARED_CORE_FAMILY_MOJANG, "neoforge"));
        assertEquals(Set.of("nbt"), ModuleMatrix.defaultSharedCoreFeatures(ModuleMatrix.SHARED_CORE_FAMILY_MOJANG, "paper"));
        assertEquals(Set.of(), ModuleMatrix.defaultSharedCoreFeatures(ModuleMatrix.SHARED_CORE_FAMILY_NONE, "fabric"));
    }

    @Test
    void installerExpectationDetectionSkipsSharedCoreModules() {
        assertEquals(
            "de.t14d3.rapunzellib.attachments.AttachmentFeatureInstaller",
            ModuleMatrix.installerExpectationForProject("platform-paper").installerType()
        );
        assertEquals(
            "de.t14d3.rapunzellib.events.GameEventBridgeInstaller",
            ModuleMatrix.installerExpectationForProject("events-fabric").installerType()
        );
        assertNull(ModuleMatrix.installerExpectationForProject("events-shared"));
        assertNull(ModuleMatrix.installerExpectationForProject("platform-shared"));
    }
}
