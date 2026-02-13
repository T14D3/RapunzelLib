package de.t14d3.rapunzellib.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class RuntimeProfile {
    private final Set<RuntimeCapability> capabilities;

    private RuntimeProfile(@NotNull Set<RuntimeCapability> capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        EnumSet<RuntimeCapability> capabilitySet = capabilities.isEmpty()
            ? EnumSet.noneOf(RuntimeCapability.class)
            : EnumSet.copyOf(capabilities);
        this.capabilities = Collections.unmodifiableSet(capabilitySet);
    }

    public static @NotNull RuntimeProfile of(@NotNull Set<RuntimeCapability> capabilities) {
        return new RuntimeProfile(capabilities);
    }

    public static @NotNull RuntimeProfile of(@NotNull RuntimeCapability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        EnumSet<RuntimeCapability> capabilitySet = EnumSet.noneOf(RuntimeCapability.class);
        for (RuntimeCapability capability : capabilities) {
            capabilitySet.add(Objects.requireNonNull(capability, "capability"));
        }
        return new RuntimeProfile(capabilitySet);
    }

    public @NotNull Set<RuntimeCapability> capabilities() {
        return capabilities;
    }

    public boolean hasCapability(@NotNull RuntimeCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return capabilities.contains(capability);
    }

    public @NotNull RuntimeProfile with(@NotNull RuntimeCapability... additionalCapabilities) {
        Objects.requireNonNull(additionalCapabilities, "additionalCapabilities");
        EnumSet<RuntimeCapability> merged = capabilities.isEmpty()
            ? EnumSet.noneOf(RuntimeCapability.class)
            : EnumSet.copyOf(capabilities);
        for (RuntimeCapability capability : additionalCapabilities) {
            merged.add(Objects.requireNonNull(capability, "capability"));
        }
        return new RuntimeProfile(merged);
    }
}
