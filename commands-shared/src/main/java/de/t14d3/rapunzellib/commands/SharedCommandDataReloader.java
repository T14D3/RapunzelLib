package de.t14d3.rapunzellib.commands;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Utility for triggering a Minecraft data-pack reload on a {@link MinecraftServer}.
 * <p>
 * Attempts to use the Paper/CraftBukkit reload helper first, falling back to
 * the vanilla {@link ReloadCommand#reloadPacks(Collection,
 * net.minecraft.commands.CommandSourceStack)} path.
 */
final class SharedCommandDataReloader {
    private SharedCommandDataReloader() {
    }

    /**
     * Reloads all data packs on the given server.
     *
     * @param server the Minecraft server to reload
     */
    static void reload(@NotNull MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        try {
            ReloadCommand.class.getMethod("reload", MinecraftServer.class).invoke(null, server);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Non-Paper runtimes do not provide the CraftBukkit helper.
        }

        PackRepository packRepository = server.getPackRepository();
        WorldData worldData = server.getWorldData();
        Collection<String> selectedIds = packRepository.getSelectedIds();
        Collection<String> discoveredIds = discoverNewPacks(packRepository, worldData, selectedIds);
        ReloadCommand.reloadPacks(discoveredIds, server.createCommandSourceStack());
    }

    /**
     * Discovers newly available packs not in the disabled set.
     *
     * @param packRepository the pack repository
     * @param worldData      the world data
     * @param selectedIds    the currently selected pack IDs
     * @return the combined set of selected and newly discovered pack IDs
     */
    private static @NotNull Collection<String> discoverNewPacks(
        @NotNull PackRepository packRepository,
        @NotNull WorldData worldData,
        @NotNull Collection<String> selectedIds
    ) {
        packRepository.reload();

        Set<String> discoveredIds = new LinkedHashSet<>(selectedIds);
        Collection<String> disabledIds = worldData.getDataConfiguration().dataPacks().getDisabled();
        for (Pack availablePack : packRepository.getAvailablePacks()) {
            String packId = availablePack.getId();
            if (!disabledIds.contains(packId)) {
                discoveredIds.add(packId);
            }
        }
        return List.copyOf(discoveredIds);
    }

}
