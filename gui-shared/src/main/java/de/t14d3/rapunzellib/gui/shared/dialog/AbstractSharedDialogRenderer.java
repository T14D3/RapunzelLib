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

public abstract class AbstractSharedDialogRenderer implements GuiRenderer {
    private final String name;

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

    protected void closeDialog(@NotNull ServerPlayer player) {
    }

    protected boolean isAvailable() {
        return true;
    }

    protected boolean isAvailable(@NotNull ServerPlayer player) {
        return true;
    }

    protected abstract boolean openDialog(@NotNull ServerPlayer player, @NotNull SharedDialogRenderData dialog);

    protected abstract @Nullable ServerPlayer unwrap(@NotNull RPlayer player);
}
