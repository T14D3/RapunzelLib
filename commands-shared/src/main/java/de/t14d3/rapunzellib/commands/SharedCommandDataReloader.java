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

final class SharedCommandDataReloader {
    private SharedCommandDataReloader() {
    }

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
