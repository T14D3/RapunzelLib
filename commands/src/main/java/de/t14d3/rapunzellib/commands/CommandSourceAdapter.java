package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

/**
 * Adapts a platform-specific command source into RapunzelLib's {@link RCommandSource}.
 * <p>
 * Implementations provide the bridge between platform command frameworks (Bukkit,
 * Sponge, etc.) and the Rapunzel command system by wrapping native source objects
 * into the common {@link RCommandSource} interface.
 * </p>
 */
public interface CommandSourceAdapter {
    /**
     * Gets the platform identifier for this adapter.
     *
     * @return the platform ID
     */
    @NotNull PlatformId platformId();

    /**
     * Checks if this adapter supports the given source object.
     *
     * @param source the platform source object
     * @return true if this adapter can wrap the source
     */
    boolean supports(@NotNull Object source);

    /**
     * Wraps a platform source object into an {@link RCommandSource}.
     *
     * @param source the platform source object
     * @return the wrapped command source
     */
    @NotNull RCommandSource wrap(@NotNull Object source);
}
