package de.t14d3.rapunzellib.gui.fabric.dialog;

import de.t14d3.rapunzellib.gui.shared.dialog.AbstractSharedDialogRenderer;
import de.t14d3.rapunzellib.gui.shared.dialog.SharedDialogRenderData;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DialogGuiRenderer extends AbstractSharedDialogRenderer {

    public static final DialogGuiRenderer INSTANCE = new DialogGuiRenderer();

    private DialogGuiRenderer() {
        super("fabric-dialog");
    }

    public boolean available(@NotNull RPlayer player) {
        return availableFor(player);
    }

    @Override
    protected boolean openDialog(@NotNull ServerPlayer serverPlayer, @NotNull SharedDialogRenderData dialog) {
        return DialogPacketHandler.sendDialog(serverPlayer, dialog.payload());
    }

    @Override
    protected boolean isAvailable(@NotNull ServerPlayer player) {
        return DialogPacketHandler.canSendDialog(player);
    }

    @Override
    protected void closeDialog(@NotNull ServerPlayer serverPlayer) {
        DialogPacketHandler.clearPending(serverPlayer.getUUID());
        serverPlayer.closeContainer();
    }

    @Nullable
    @Override
    protected ServerPlayer unwrap(@NotNull RPlayer player) {
        return player.tryHandle(ServerPlayer.class).orElse(null);
    }
}
