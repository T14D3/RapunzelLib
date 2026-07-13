package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Manages the lifecycle and registry of all visual objects.
 * <p>
 * Provides factory methods for each visual type and supports
 * registration, lookup, and bulk removal of visuals.
 */
public interface VisualManager {

    /**
     * Registers a visual with this manager.
     *
     * @param visual the visual to register
     */
    void register(@NotNull Visual<?> visual);

    /**
     * Unregisters a visual from this manager.
     *
     * @param visual the visual to unregister
     */
    void unregister(@NotNull Visual<?> visual);

    @NotNull Collection<Visual<?>> all();

    /**
     * Finds a registered visual by its identifier.
     *
     * @param id the visual ID to look up
     * @return an {@link Optional} containing the visual if found, or empty if not
     */
    @NotNull Optional<Visual<?>> find(@NotNull VisualId id);

    /**
     * Removes all registered visuals.
     */
    void removeAll();

    /**
     * Creates a new particle visual with the given configuration and audience.
     *
     * @param config   the particle configuration
     * @param audience the target audience
     * @return the created particle visual
     */
    @NotNull ParticleVisual createParticle(@NotNull ParticleConfig config, @NotNull VisualAudience audience);

    /**
     * Creates a new block display visual with the given configuration, location, and audience.
     *
     * @param config   the block display configuration
     * @param location the location for the block display
     * @param audience the target audience
     * @return the created block display visual
     */
    @NotNull BlockDisplayVisual createBlockDisplay(
        @NotNull BlockDisplayConfig config,
        @NotNull RLocation location,
        @NotNull VisualAudience audience
    );

    /**
     * Creates a new glow outline visual with the given configuration and audience.
     *
     * @param config   the glow outline configuration
     * @param audience the target audience
     * @return the created glow outline visual
     */
    @NotNull GlowOutlineVisual createGlowOutline(@NotNull GlowOutlineConfig config, @NotNull VisualAudience audience);

    /**
     * Creates a new beacon beam visual with the given configuration and audience.
     *
     * @param config   the beacon beam configuration
     * @param audience the target audience
     * @return the created beacon beam visual
     */
    @NotNull BeaconBeamVisual createBeaconBeam(@NotNull BeaconBeamConfig config, @NotNull VisualAudience audience);

    /**
     * Creates a new block structure visual with the given configuration and audience.
     *
     * @param config   the block structure configuration
     * @param audience the target audience
     * @return the created block structure visual
     */
    @NotNull BlockStructureVisual createBlockStructure(
        @NotNull BlockStructureConfig config,
        @NotNull VisualAudience audience
    );
}
