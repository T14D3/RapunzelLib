package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.context.RapunzelContext;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Support utility for installing shared command feature plumbing.
 * Currently exposes the runtime command registration helper used by the
 * platform-specific {@code CommandFeatureInstaller} implementations.
 */
public final class SharedCommandFeatureInstallerSupport {
    private SharedCommandFeatureInstallerSupport() {
    }

    public static @NotNull SharedRuntimeCommandRegistrationSupport installRuntimeCommandRegistrationSupport(
        @NotNull RapunzelContext context
    ) {
        Objects.requireNonNull(context, "context");

        return context.getOrCreate(
            SharedRuntimeCommandRegistrationSupport.class,
            () -> new SharedRuntimeCommandRegistrationSupport(
                context.services().get(RCommandService.class),
                context.scheduler(),
                context.services().get(MinecraftServer.class)
            )
        );
    }
}
