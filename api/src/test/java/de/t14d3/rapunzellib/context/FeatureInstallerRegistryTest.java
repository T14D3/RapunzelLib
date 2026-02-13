package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.fixtures.TestCompleteFeatureInstaller;
import de.t14d3.rapunzellib.context.fixtures.TestPartialFeatureInstaller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeatureInstallerRegistryTest {
    @Test
    void resolvesInstallerForEachPlatformIdWhenServicesArePresent() {
        FeatureInstallerRegistry<TestCompleteFeatureInstaller> registry = FeatureInstallerRegistry.create(
            TestCompleteFeatureInstaller.class,
            TestCompleteFeatureInstaller::platformId,
            "rapunzellib-"
        );

        for (PlatformId platformId : PlatformId.values()) {
            TestCompleteFeatureInstaller installer = registry.resolve(platformId);
            assertEquals(platformId, installer.platformId());
        }
    }

    @Test
    void throwsClearErrorWhenInstallerForPlatformIsMissing() {
        FeatureInstallerRegistry<TestPartialFeatureInstaller> registry = FeatureInstallerRegistry.create(
            TestPartialFeatureInstaller.class,
            TestPartialFeatureInstaller::platformId,
            "rapunzellib-"
        );

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> registry.resolve(PlatformId.VELOCITY));

        assertTrue(error.getMessage().contains("No TestPartialFeatureInstaller found for platform VELOCITY"));
        assertTrue(error.getMessage().contains("Add dependency rapunzellib-velocity."));
        assertTrue(error.getMessage().contains("Available installer platforms: PAPER."));
    }

    @Test
    void supportsCustomDependencyHints() {
        FeatureInstallerRegistry<TestPartialFeatureInstaller> registry = FeatureInstallerRegistry.create(
            TestPartialFeatureInstaller.class,
            TestPartialFeatureInstaller::platformId,
            platformId -> "Add dependency rapunzellib-platform-" + platformId.name().toLowerCase() + "."
        );

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> registry.resolve(PlatformId.VELOCITY));

        assertTrue(error.getMessage().contains("Add dependency rapunzellib-platform-velocity."));
    }
}
