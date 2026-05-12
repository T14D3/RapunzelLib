package de.t14d3.rapunzellib.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable set of {@link RuntimeCapability} flags describing a runtime profile.
 */
public final class RuntimeProfile {
    private final Set<RuntimeCapability> capabilities;

    private RuntimeProfile(@NotNull Set<RuntimeCapability> capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        EnumSet<RuntimeCapability> capabilitySet = capabilities.isEmpty()
            ? EnumSet.noneOf(RuntimeCapability.class)
            : EnumSet.copyOf(capabilities);
        this.capabilities = Collections.unmodifiableSet(capabilitySet);
    }

    /**
     * Creates a profile from a set of capabilities.
     *
     * @param capabilities the capabilities
     * @return the profile
     */
    public static @NotNull RuntimeProfile of(@NotNull Set<RuntimeCapability> capabilities) {
        return new RuntimeProfile(capabilities);
    }

    /**
     * Creates a profile from a varargs array of capabilities.
     *
     * @param capabilities the capabilities
     * @return the profile
     */
    public static @NotNull RuntimeProfile of(@NotNull RuntimeCapability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        EnumSet<RuntimeCapability> capabilitySet = EnumSet.noneOf(RuntimeCapability.class);
        for (RuntimeCapability capability : capabilities) {
            capabilitySet.add(Objects.requireNonNull(capability, "capability"));
        }
        return new RuntimeProfile(capabilitySet);
    }

    /**
     * Returns the capabilities in this profile.
     *
     * @return the capabilities
     */
    public @NotNull Set<RuntimeCapability> capabilities() {
        return capabilities;
    }

    /**
     * Checks whether the profile contains the given capability.
     *
     * @param capability the capability to check
     * @return true if the capability is present
     */
    public boolean hasCapability(@NotNull RuntimeCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return capabilities.contains(capability);
    }

    /**
     * Creates a new profile with the additional capabilities merged in.
     *
     * @param additionalCapabilities the capabilities to add
     * @return the new merged profile
     */
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
