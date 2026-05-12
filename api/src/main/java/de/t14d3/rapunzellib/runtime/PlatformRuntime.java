package de.t14d3.rapunzellib.runtime;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Describes the runtime environment in which RapunzelLib is operating.
 *
 * <p>Includes the platform identity, role, engine family, available capabilities,
 * and lifecycle owner.</p>
 */
public final class PlatformRuntime {
    private final PlatformId platformId;
    private final RuntimeRole role;
    private final EngineFamily engineFamily;
    private final Set<RuntimeCapability> capabilities;
    private final LifecycleOwner lifecycleOwner;

    /**
     * Creates a platform runtime descriptor.
     *
     * @param platformId      the platform identifier
     * @param role            the runtime role
     * @param engineFamily    the engine family
     * @param capabilities    the set of supported capabilities
     * @param lifecycleOwner  the lifecycle owner
     */
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

    /**
     * Returns the platform identifier.
     *
     * @return the platform ID
     */
    public @NotNull PlatformId platformId() {
        return platformId;
    }

    /**
     * Returns the runtime role.
     *
     * @return the role
     */
    public @NotNull RuntimeRole role() {
        return role;
    }

    /**
     * Returns the engine family.
     *
     * @return the engine family
     */
    public @NotNull EngineFamily engineFamily() {
        return engineFamily;
    }

    /**
     * Returns the set of supported capabilities.
     *
     * @return the capabilities
     */
    public @NotNull Set<RuntimeCapability> capabilities() {
        return capabilities;
    }

    /**
     * Checks whether the runtime has the given capability.
     *
     * @param capability the capability to check
     * @return true if the capability is supported
     */
    public boolean hasCapability(@NotNull RuntimeCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return capabilities.contains(capability);
    }

    /**
     * Requires that the runtime has the given capability, throwing an {@link IllegalStateException} if not.
     *
     * @param capability the required capability
     * @throws IllegalStateException if the capability is not supported
     */
    public void requireCapability(@NotNull RuntimeCapability capability) {
        requireCapability(capability, capability.name().toLowerCase());
    }

    /**
     * Requires that the runtime has the given capability, with a use case description.
     *
     * @param capability the required capability
     * @param useCase    the use case description for error messages
     * @throws IllegalStateException if the capability is not supported
     */
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

    /**
     * Returns the lifecycle owner.
     *
     * @return the lifecycle owner
     */
    public @NotNull LifecycleOwner lifecycleOwner() {
        return lifecycleOwner;
    }

    /**
     * Returns the lifecycle owner.
     *
     * @return the lifecycle owner
     */
    public @NotNull LifecycleOwner owner() {
        return lifecycleOwner;
    }

    /**
     * Returns the raw owner object.
     *
     * @return the raw owner
     */
    public @NotNull Object rawOwner() {
        return owner().raw();
    }

    /**
     * Generates a persistent owner ID based on the platform and data directory.
     *
     * @param dataDirectory the data directory path
     * @return the persistent owner ID string
     */
    public @NotNull String persistentOwnerId(@NotNull Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        return platformId.name() + ":" + dataDirectory.toAbsolutePath().normalize();
    }
}
