package de.t14d3.rapunzellib.gui.sponge.dialog;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModelBuilder;
import de.t14d3.rapunzellib.gui.element.*;
import de.t14d3.rapunzellib.gui.layout.GridLayout;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.*;

public final class DialogRenderer implements GuiRenderer {

    private static final String NAME = "sponge-dialog";
    private static final Set<GuiCapability> CAPABILITIES = GuiRendererSelectionSupport.dialogCapabilities();

    private final Map<UUID, ActiveDialog> activeDialogs = new HashMap<>();
    private volatile boolean dialogApiAvailable = false;

    public DialogRenderer() {
        checkDialogApiAvailability();
    }

    private void checkDialogApiAvailability() {
        try {
            Class.forName("org.spongepowered.api.ui.dialog.Dialog");
            dialogApiAvailable = true;
        } catch (ClassNotFoundException e) {
            dialogApiAvailable = false;
        }
    }

    public boolean isAvailable() {
        return dialogApiAvailable;
    }

    @Override
    public @NotNull String name() {
        return NAME;
    }

    @Override
    public @NotNull Set<GuiCapability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public boolean supports(@NotNull GuiCapability capability) {
        return CAPABILITIES.contains(capability);
    }

    @Override
    public void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
        if (!dialogApiAvailable) {
            throw new UnsupportedOperationException("Dialog API is not available in this Sponge version");
        }

        ServerPlayer spongePlayer = resolvePlayer(player);
        if (spongePlayer == null) {
            return;
        }

        close(gui, player);

        DialogBuilder builder = new DialogBuilder();
        GuiDialogModel model = GuiDialogModelBuilder.build(gui, Component.text("Dialog"));
        Object dialog = builder.build(model, context);

        if (dialog != null) {
            ActiveDialog active = new ActiveDialog(gui, dialog, context, model);
            activeDialogs.put(player.uuid(), active);
            DialogHandler.openDialog(spongePlayer, dialog);
        }
    }

    @Override
    public void close(@NotNull Gui gui, @NotNull RPlayer player) {
        ActiveDialog active = activeDialogs.remove(player.uuid());
        if (active != null) {
            ServerPlayer spongePlayer = resolvePlayer(player);
            if (spongePlayer != null) {
                DialogHandler.closeDialog(spongePlayer);
            }
        }
    }

    @org.jetbrains.annotations.Nullable
    ActiveDialog getActiveDialog(@NotNull UUID playerId) {
        return activeDialogs.get(playerId);
    }

    void removeActiveDialog(@NotNull UUID playerId) {
        activeDialogs.remove(playerId);
    }

    @org.jetbrains.annotations.Nullable
    private ServerPlayer resolvePlayer(@NotNull RPlayer player) {
        return Sponge.server().player(player.uuid()).orElse(null);
    }

    static final class ActiveDialog {
        final Gui gui;
        final Object dialog;
        final RenderContext context;
        final GuiDialogModel model;

        ActiveDialog(Gui gui, Object dialog, RenderContext context, GuiDialogModel model) {
            this.gui = gui;
            this.dialog = dialog;
            this.context = context;
            this.model = model;
        }
    }
}
