package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InventoryFeaturesTest {
    @BeforeEach
    void setUp() {
        Rapunzel.shutdownAll();
        TestInventoryFeatureInstallers.reset();
    }

    @AfterEach
    void tearDown() {
        Rapunzel.shutdownAll();
    }

    @Test
    void installNoOpsWhenInventoriesAlreadyRegistered(@TempDir Path dir) {
        TestSupport.TestContext context = new TestSupport.TestContext(
            TestSupport.serverRuntime(PlatformId.NEOFORGE, RuntimeCapability.INVENTORY),
            dir
        );
        Inventories existing = InventoryFeatureInstallerSupport.registerInventories(
            context,
            PlatformId.NEOFORGE,
            List.of(TestSupport.testFactory(PlatformId.NEOFORGE))
        );

        Rapunzel.bootstrap(this, context);
        Inventories installed = InventoryFeatures.install();

        assertSame(existing, installed);
        assertSame(existing, InventoryFeatures.inventories());
        assertEquals(0, TestInventoryFeatureInstallers.paperInstallCalls());
        assertEquals(0, TestInventoryFeatureInstallers.velocityInstallCalls());
    }

    @Test
    void resolvesInstallerByPlatformFromServiceLoader(@TempDir Path dir) {
        TestSupport.TestContext context = new TestSupport.TestContext(
            TestSupport.serverRuntime(PlatformId.PAPER, RuntimeCapability.INVENTORY),
            dir
        );
        Rapunzel.bootstrap(this, context);

        Inventories installed = InventoryFeatures.install();

        assertEquals(1, TestInventoryFeatureInstallers.paperInstallCalls());
        assertEquals(0, TestInventoryFeatureInstallers.velocityInstallCalls());
        assertEquals(PlatformId.PAPER, installed.platformId());
        assertSame(installed, InventoryFeatures.inventories());
        assertTrue(installed.wrap(new TestSupport.TestNativeInventory(3)).isPresent());
    }

    @Test
    void rejectsUnsupportedRuntimeBeforeResolvingInstaller(@TempDir Path dir) {
        TestSupport.TestContext context = new TestSupport.TestContext(
            TestSupport.proxyRuntime(PlatformId.VELOCITY),
            dir
        );
        Rapunzel.bootstrap(this, context);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            InventoryFeatures::install
        );

        assertEquals(0, TestInventoryFeatureInstallers.paperInstallCalls());
        assertEquals(0, TestInventoryFeatureInstallers.velocityInstallCalls());
        assertEquals(
            "RapunzelLib inventory features requires capability INVENTORY but runtime VELOCITY is PROXY / PROXY",
            ex.getMessage()
        );
    }
}
