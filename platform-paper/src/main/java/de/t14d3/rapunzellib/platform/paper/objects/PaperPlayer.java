package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class PaperPlayer extends RNativeHandle<Player> implements RPlayer {
    PaperPlayer(Player handle) {
        super(PlatformId.PAPER, Objects.requireNonNull(handle, "handle"));
    }

    void updateHandle(Player newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public Audience audience() {
        return handle();
    }

    @Override
    public UUID uuid() {
        return handle().getUniqueId();
    }

    @Override
    public String name() {
        return handle().getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle().hasPermission(permission);
    }

    @Override
    public Optional<RWorld> world() {
        Player player = handle();
        return Rapunzel.context().worlds()
            .wrap(player.getWorld())
            .or(() -> Optional.of(new PaperWorld(player.getWorld())));
    }

    @Override
    public Optional<RLocation> location() {
        Location loc = handle().getLocation();
        String key = loc.getWorld() != null ? loc.getWorld().getKey().toString() : null;
        String name = (loc.getWorld() != null) ? loc.getWorld().getName() : null;
        return Optional.of(new RLocation(new RWorldRef(name, key), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
    }

    @Override
    public boolean canTeleport() {
        return true;
    }

    @Override
    public void teleport(RLocation location) {
        Objects.requireNonNull(location, "location");
        RWorldRef worldRef = location.world();
        org.bukkit.World world = null;
        if (worldRef != null) {
            if (worldRef.name() != null) {
                world = Bukkit.getWorld(worldRef.name());
            }
            if (world == null && worldRef.key() != null) {
                NamespacedKey key = NamespacedKey.fromString(worldRef.key());
                if (key != null) {
                    world = Bukkit.getWorld(key);
                }
            }
        }

        if (world == null) {
            world = handle().getWorld();
        }

        Location target = new Location(world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
        handle().teleport(target);
    }

    @Override
    public Optional<String> currentServerName() {
        return Optional.of(Rapunzel.context().services().get(NetworkInfoService.class).networkServerName().join());
    }
}

