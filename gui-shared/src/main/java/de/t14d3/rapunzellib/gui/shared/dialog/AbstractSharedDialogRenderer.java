package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Abstract base for platform-specific dialog renderers.
 * <p>
 * Handles dialog capability advertisement, rendering lifecycle (open, close),
 * and session registration through {@link SharedDialogSessions}.
 */
public abstract class AbstractSharedDialogRenderer implements GuiRenderer {
    private final String name;

    /**
     * Creates a named dialog renderer.
     *
     * @param name the renderer name
     */
    protected AbstractSharedDialogRenderer(@NotNull String name) {
        this.name = name;
    }

    @Override
    public final @NotNull String name() {
        return name;
    }

    @Override
    public final @NotNull Set<GuiCapability> capabilities() {
        return isAvailable() ? GuiRendererSelectionSupport.dialogCapabilities() : Set.of();
    }

    /**
     * Checks whether this renderer is available for a specific player.
     *
     * @param player the player
     * @return {@code true} if available
     */
    public final boolean availableFor(@NotNull RPlayer player) {
        if (!isAvailable()) {
            return false;
        }
        ServerPlayer serverPlayer = unwrap(player);
        return serverPlayer != null && isAvailable(serverPlayer);
    }

    @Override
    public final boolean supports(@NotNull GuiCapability capability) {
        return capabilities().contains(capability);
    }

    @Override
    public final void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
        ServerPlayer serverPlayer = unwrap(player);
        if (serverPlayer == null) {
            return;
        }
        if (!isAvailable() || !isAvailable(serverPlayer)) {
            throw new UnsupportedOperationException(name + " is not available");
        }

        SharedDialogRenderData dialog = SharedDialogRenderSupport.prepare(gui, context, Component.text("Dialog"));
        if (!openDialog(serverPlayer, dialog)) {
            SharedDialogSessions.clear(serverPlayer.getUUID());
            return;
        }

        SharedDialogSessions.register(
            serverPlayer.getUUID(),
            new SharedDialogSessions.PendingDialog(context.player(), context.state(), dialog.model())
        );
    }

    @Override
    public final void close(@NotNull Gui gui, @NotNull RPlayer player) {
        ServerPlayer serverPlayer = unwrap(player);
        if (serverPlayer == null) {
            return;
        }

        SharedDialogSessions.clear(serverPlayer.getUUID());
        closeDialog(serverPlayer);
    }

    /**
     * Hook to close the dialog for a native player. Subclasses may override.
     *
     * @param player the native player
     */
    protected void closeDialog(@NotNull ServerPlayer player) {
    }

    /**
     * Checks whether this renderer is globally available.
     *
     * @return {@code true} if available
     */
    protected boolean isAvailable() {
        return true;
    }

    /**
     * Checks whether this renderer is available for a specific native player.
     *
     * @param player the native player
     * @return {@code true} if available
     */
    protected boolean isAvailable(@NotNull ServerPlayer player) {
        return true;
    }

    /**
     * Opens a dialog for the native player.
     *
     * @param player the native player
     * @param dialog the dialog render data
     * @return {@code true} if the dialog was successfully opened
     */
    protected abstract boolean openDialog(@NotNull ServerPlayer player, @NotNull SharedDialogRenderData dialog);

    /**
     * Unwraps a Rapunzel player to a native server player.
     *
     * @param player the Rapunzel player
     * @return the native server player, or {@code null}
     */
    protected abstract @Nullable ServerPlayer unwrap(@NotNull RPlayer player);
}
