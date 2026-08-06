package de.t14d3.rapunzellib.gui.neoforge.dialog;

import de.t14d3.rapunzellib.gui.shared.dialog.AbstractSharedDialogRenderer;
import de.t14d3.rapunzellib.gui.shared.dialog.SharedDialogRenderData;
import de.t14d3.rapunzellib.gui.shared.dialog.SharedDialogSubmission;
import de.t14d3.rapunzellib.gui.shared.dialog.SharedDialogSessions;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * NeoForge dialog renderer backed by the vanilla custom-payload channel.
 * <p>
 * Dialogs are transported as {@link DialogOpenPayload} packets, so no client-side
 * mod is required; vanilla clients natively support these custom payloads.
 */
public final class DialogRenderer extends AbstractSharedDialogRenderer {
    private static final DialogRenderer INSTANCE = new DialogRenderer();

    public static @NotNull DialogRenderer instance() {
        return INSTANCE;
    }

    private DialogRenderer() {
        super("neoforge-dialog");
    }

    /**
     * Checks whether the packet transport is available. The transport is always
     * available server-side, so this is {@code true} unconditionally.
     *
     * @return {@code true}
     */
    public boolean available() {
        return true;
    }

    @Override
    protected boolean isAvailable() {
        return available();
    }

    @Override
    protected boolean isAvailable(@NotNull ServerPlayer player) {
        return DialogPacketHandler.canSendDialog(player);
    }

    @Override
    protected boolean openDialog(@NotNull ServerPlayer serverPlayer, @NotNull SharedDialogRenderData dialog) {
        return DialogPacketHandler.sendDialog(serverPlayer, dialog.payload());
    }

    @Override
    protected void closeDialog(@NotNull ServerPlayer serverPlayer) {
        DialogPacketHandler.clearPending(serverPlayer.getUUID());
        serverPlayer.closeContainer();
    }

    @Override
    protected @Nullable ServerPlayer unwrap(@NotNull RPlayer player) {
        return player.tryHandle(ServerPlayer.class).orElse(null);
    }

    static void handleSubmit(UUID playerId, SharedDialogSubmission values) {
        SharedDialogSessions.submit(playerId, values);
    }
}
