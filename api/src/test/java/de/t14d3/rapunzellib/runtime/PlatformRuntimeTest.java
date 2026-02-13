package de.t14d3.rapunzellib.runtime;

import de.t14d3.rapunzellib.PlatformId;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlatformRuntimeTest {
    @Test
    void exposesCapabilitiesAndTypedLifecycleOwner() {
        Object owner = new Object();
        PlatformRuntime runtime = new PlatformRuntime(
            PlatformId.PAPER,
            RuntimeRole.SERVER,
            EngineFamily.MOJANG_SERVER,
            EnumSet.of(RuntimeCapability.COMMANDS, RuntimeCapability.WORLDS),
            new LifecycleOwner(owner)
        );

        assertTrue(runtime.hasCapability(RuntimeCapability.COMMANDS));
        assertFalse(runtime.hasCapability(RuntimeCapability.GUI));
        assertSame(owner, runtime.lifecycleOwner().raw());
        assertTrue(runtime.lifecycleOwner().is(Object.class));
        assertSame(owner, runtime.lifecycleOwner().require(Object.class));
    }

    @Test
    void rejectsMissingCapabilityWithExplicitRuntimeDetails() {
        PlatformRuntime runtime = new PlatformRuntime(
            PlatformId.VELOCITY,
            RuntimeRole.PROXY,
            EngineFamily.PROXY,
            EnumSet.noneOf(RuntimeCapability.class),
            new LifecycleOwner(new Object())
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> runtime.requireCapability(RuntimeCapability.NBT, "NBT features")
        );

        assertEquals(
            "RapunzelLib NBT features requires capability NBT but runtime VELOCITY is PROXY / PROXY",
            ex.getMessage()
        );
    }
}
