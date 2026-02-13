package de.t14d3.rapunzellib.gui.paper.dialog;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.element.*;
import de.t14d3.rapunzellib.objects.RPlayer;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderer for Minecraft 1.21.6+ Dialog-based GUIs.
 * Uses Paper's native Dialog API for rich form-like interfaces.
 */
public final class DialogRenderer implements GuiRenderer {

    private static final DialogRenderer INSTANCE = new DialogRenderer();
    private static final Set<GuiCapability> CAPABILITIES = GuiRendererSelectionSupport.dialogCapabilities();

    private final Map<UUID, Dialog> activeDialogs = new ConcurrentHashMap<>();

    private DialogRenderer() {
    }

    public static DialogRenderer instance() {
        return INSTANCE;
    }

    @Override
    public @NotNull String name() {
        return "dialog";
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
        Player bukkitPlayer = resolveBukkitPlayer(player);
        if (bukkitPlayer == null) {
            return;
        }

        Component title = gui.title() != null ? gui.title() : Component.text("Dialog");
        
        DialogBuilder builder = new DialogBuilder(gui, context);
        DialogBase dialogBase = builder.build(title);
        
        if (dialogBase == null) {
            return;
        }

        // Use multiAction type if we have buttons, otherwise notice
        final var dialogType = builder.hasButtons() 
            ? DialogType.multiAction(builder.getActionButtons(), null, 1)
            : DialogType.notice();

        Dialog dialog = Dialog.create(factory -> {
            DialogRegistryEntry.Builder dialogBuilder = factory.empty();
            dialogBuilder.base(dialogBase);
            dialogBuilder.type(dialogType);
        });
        
        activeDialogs.put(player.uuid(), dialog);
        bukkitPlayer.showDialog(dialog);
    }

    @Override
    public void close(@NotNull Gui gui, @NotNull RPlayer player) {
        activeDialogs.remove(player.uuid());
    }

    @Nullable
    public Dialog getActiveDialog(@NotNull UUID playerId) {
        return activeDialogs.get(playerId);
    }

    public void removeActiveDialog(@NotNull UUID playerId) {
        activeDialogs.remove(playerId);
    }

    @Nullable
    private Player resolveBukkitPlayer(@NotNull RPlayer player) {
        return player.tryHandle(Player.class).orElseGet(() -> Bukkit.getPlayer(player.uuid()));
    }
}
