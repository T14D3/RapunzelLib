package de.t14d3.rapunzellib.runtime;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class PlatformRuntime {
    private final PlatformId platformId;
    private final RuntimeRole role;
    private final EngineFamily engineFamily;
    private final Set<RuntimeCapability> capabilities;
    private final LifecycleOwner lifecycleOwner;

    public PlatformRuntime(
        @NotNull PlatformId platformId,
        @NotNull RuntimeRole role,
        @NotNull EngineFamily engineFamily,
        @NotNull Set<RuntimeCapability> capabilities,
        @NotNull LifecycleOwner lifecycleOwner
    ) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.role = Objects.requireNonNull(role, "role");
        this.engineFamily = Objects.requireNonNull(engineFamily, "engineFamily");
        Objects.requireNonNull(capabilities, "capabilities");
        EnumSet<RuntimeCapability> capabilitySet = capabilities.isEmpty()
            ? EnumSet.noneOf(RuntimeCapability.class)
            : EnumSet.copyOf(capabilities);
        this.capabilities = Collections.unmodifiableSet(capabilitySet);
        this.lifecycleOwner = Objects.requireNonNull(lifecycleOwner, "lifecycleOwner");
    }

    public @NotNull PlatformId platformId() {
        return platformId;
    }

    public @NotNull RuntimeRole role() {
        return role;
    }

    public @NotNull EngineFamily engineFamily() {
        return engineFamily;
    }

    public @NotNull Set<RuntimeCapability> capabilities() {
        return capabilities;
    }

    public boolean hasCapability(@NotNull RuntimeCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return capabilities.contains(capability);
    }

    public void requireCapability(@NotNull RuntimeCapability capability) {
        requireCapability(capability, capability.name().toLowerCase());
    }

    public void requireCapability(@NotNull RuntimeCapability capability, @NotNull String useCase) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(useCase, "useCase");
        if (hasCapability(capability)) {
            return;
        }
        throw new IllegalStateException(
            "RapunzelLib " + useCase + " requires capability " + capability + " but runtime " + platformId +
                " is " + role + " / " + engineFamily
        );
    }

    public @NotNull LifecycleOwner lifecycleOwner() {
        return lifecycleOwner;
    }

    public @NotNull LifecycleOwner owner() {
        return lifecycleOwner;
    }

    public @NotNull Object rawOwner() {
        return owner().raw();
    }

    public @NotNull String persistentOwnerId(@NotNull Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        return platformId.name() + ":" + dataDirectory.toAbsolutePath().normalize();
    }
}
