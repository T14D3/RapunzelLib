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

public final class DialogRenderer extends AbstractSharedDialogRenderer {
    private static final DialogRenderer INSTANCE = new DialogRenderer();

    public static @NotNull DialogRenderer instance() {
        return INSTANCE;
    }

    private DialogRenderer() {
        super("neoforge-dialog");
    }

    public boolean available() {
        return false;
    }

    @Override
    protected boolean isAvailable() {
        return available();
    }

    @Override
    protected boolean openDialog(@NotNull ServerPlayer serverPlayer, @NotNull SharedDialogRenderData dialog) {
        return false;
    }

    @Override
    protected @Nullable ServerPlayer unwrap(@NotNull RPlayer player) {
        return player.tryHandle(ServerPlayer.class).orElse(null);
    }

    static void handleSubmit(UUID playerId, SharedDialogSubmission values) {
        SharedDialogSessions.submit(playerId, values);
    }
}
