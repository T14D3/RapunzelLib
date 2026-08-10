package de.t14d3.rapunzellib.events.paper;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.block.*;
import de.t14d3.rapunzellib.events.entity.*;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.player.*;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.ExplosionSourceKind;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RGameMode;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.WrapperStore;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import de.t14d3.rapunzellib.events.inventory.InventoryActionType;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryTransferPre;
import de.t14d3.rapunzellib.events.inventory.InventoryTransferPre.TransferSource;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.nbt.item.RItem;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@SuppressWarnings("DefaultAnnotationParam")
final class PaperGameEventsBridge implements Listener, GameEventBridge {
    private final JavaPlugin plugin;
    private final GameEventBus bus;

    /**
     * Lazily resolved {@link WrapperStore}, providing cached
     * {@link RWorldRef} and {@link RLocation} resolution for Bukkit handles.
     */
    private WrapperStore wrapperStore;

    PaperGameEventsBridge(JavaPlugin plugin, GameEventBus bus) {
        this.plugin = plugin;
        this.bus = bus;
    }

    void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
    }

    /** Returns the registered {@link WrapperStore}, resolved lazily on first use. */
    private WrapperStore wrapperStore() {
        if (wrapperStore == null) {
            wrapperStore = WrapperStore.current();
            if (wrapperStore == null) {
                throw new IllegalStateException("No WrapperStore registered in the active context");
            }
        }
        return wrapperStore;
    }

    /** Returns a cached {@link RWorldRef} for the given Bukkit {@link World}. */
    private RWorldRef worldRef(World world) {
        return wrapperStore().worldRef(world).orElseThrow(() ->
            new IllegalArgumentException("Unsupported native world type: " + world));
    }

    private RLocation fromBukkit(Location location) {
        return wrapperStore().location(location).orElseThrow(() ->
            new IllegalArgumentException("Unsupported native location type: " + location));
    }

    /** Convenience overload extracting the world from a {@link Location}. */
    private RWorldRef worldRef(Location loc) {
        return worldRef(loc.getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreakPre(BlockBreakEvent event) {
        if (!bus.hasPreListeners(BlockBreakPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getBlock());

        BlockBreakPre pre = new BlockBreakPre(player, block, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreakPost(BlockBreakEvent event) {
        boolean needsPost = bus.hasPostListeners(BlockBreakPost.class);
        boolean needsAsync = bus.hasAsyncListeners(BlockBreakSnapshot.class);
        if (!needsPost && !needsAsync) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getBlock());
        boolean cancelled = event.isCancelled();

        if (needsPost) {
            bus.dispatchPost(new BlockBreakPost(player, block, cancelled));
        }
        if (needsAsync) {
            bus.dispatchAsync(BlockBreakSnapshot.capture(player.uuid(), block, cancelled));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlacePre(BlockPlaceEvent event) {
        if (!bus.hasPreListeners(BlockPlacePre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getBlockPlaced());

        BlockPlacePre pre = new BlockPlacePre(player, block, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPlacePost(BlockPlaceEvent event) {
        boolean needsPost = bus.hasPostListeners(BlockPlacePost.class);
        boolean needsAsync = bus.hasAsyncListeners(BlockPlaceSnapshot.class);   
        if (!needsPost && !needsAsync) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getBlockPlaced());
        boolean cancelled = event.isCancelled();

        if (needsPost) {
            bus.dispatchPost(new BlockPlacePost(player, block, cancelled));
        }
        if (needsAsync) {
            bus.dispatchAsync(BlockPlaceSnapshot.capture(player.uuid(), block, cancelled));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractBlockPre(PlayerInteractEvent event) {
        InteractBlockPre.Action action = switch (event.getAction()) {
            case LEFT_CLICK_BLOCK, LEFT_CLICK_AIR -> InteractBlockPre.Action.ATTACK;
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> InteractBlockPre.Action.USE;
            // Physical contact (pressure plates, tripwires, ...): the clicked
            // block is the plate/wire and cancellation is honored by Paper
            // (the plate/wire does not activate).
            case PHYSICAL -> InteractBlockPre.Action.STEP;
            default -> null;
        };
        if (action == null) return;
        if (!bus.hasPreListeners(InteractBlockPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = event.getClickedBlock() != null ? Rapunzel.blocks().require(event.getClickedBlock()) : null;
        // PHYSICAL events carry no block face.
        String face = action == InteractBlockPre.Action.STEP
                || event.getClickedBlock() == null || event.getBlockFace() == null
                ? null : event.getBlockFace().name();

        InteractBlockPre pre = new InteractBlockPre(player, action, block, face, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteractBlockPost(PlayerInteractEvent event) {
        InteractBlockPre.Action action = switch (event.getAction()) {
            case LEFT_CLICK_BLOCK, LEFT_CLICK_AIR -> InteractBlockPre.Action.ATTACK;
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> InteractBlockPre.Action.USE;
            case PHYSICAL -> InteractBlockPre.Action.STEP;
            default -> null;
        };
        if (action == null) return;
        boolean needsPost = bus.hasPostListeners(InteractBlockPost.class);
        boolean needsAsync = bus.hasAsyncListeners(UseBlockSnapshot.class);
        if (!needsPost && !needsAsync) return;
        // The legacy async block-use snapshot only applies to right-clicks on a block.
        if (event.getClickedBlock() == null || action != InteractBlockPre.Action.USE) {
            needsAsync = false;
        }
        if (!needsPost && !needsAsync) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = event.getClickedBlock() != null ? Rapunzel.blocks().require(event.getClickedBlock()) : null;
        // PHYSICAL events carry no block face.
        String face = action == InteractBlockPre.Action.STEP
                || event.getClickedBlock() == null || event.getBlockFace() == null
                ? null : event.getBlockFace().name();
        boolean cancelled = event.isCancelled();

        if (needsPost) {
            bus.dispatchPost(new InteractBlockPost(player, action, block, face, cancelled));
        }
        if (needsAsync && block != null) {
            bus.dispatchAsync(UseBlockSnapshot.capture(player.uuid(), block, cancelled));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractEntityPre(PlayerInteractEntityEvent event) {    
        if (!bus.hasPreListeners(InteractEntityPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        var entity = Rapunzel.entities().require(event.getRightClicked());

        InteractEntityPre pre = new InteractEntityPre(player, entity, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAttackEntityPre(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof org.bukkit.entity.Player damager)) return;
        if (!bus.hasPreListeners(AttackEntityPre.class)) return;

        RPlayer player = Rapunzel.players().require(damager);
        var entity = Rapunzel.entities().require(event.getEntity());

        AttackEntityPre pre = new AttackEntityPre(player, entity, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntitySpawnPre(CreatureSpawnEvent event) {
        if (!bus.hasPreListeners(EntitySpawnPre.class)) return;

        REntityType type = REntityType.require(RKey.of(event.getEntityType().getKey().namespace(), event.getEntityType().getKey().value()));
        String reason = event.getSpawnReason().name();

        EntitySpawnPre pre = new EntitySpawnPre(fromBukkit(event.getLocation()), type, reason, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    /**
     * Handles {@link EntitySpawnEvent} for entity types that do not fire
     * {@link CreatureSpawnEvent} (e.g., spawn eggs in Paper 1.21.4+).
     * Skips events already handled by {@link #onEntitySpawnPre(CreatureSpawnEvent)}.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntitySpawnPreGeneric(EntitySpawnEvent event) {
        // Skip if this is a CreatureSpawnEvent (already handled by onEntitySpawnPre)
        if (event instanceof CreatureSpawnEvent) return;
        if (!bus.hasPreListeners(EntitySpawnPre.class)) return;

        EntitySpawnPre pre = new EntitySpawnPre(
                fromBukkit(event.getLocation()),
                REntityType.require(RKey.of(
                        event.getEntityType().getKey().namespace(),
                        event.getEntityType().getKey().value()
                )),
                "UNKNOWN",
                event.isCancelled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityHurtPre(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof org.bukkit.entity.Player) {
            return;
        }
        if (!bus.hasPreListeners(EntityHurtPre.class)) return;

        String damageType = event.getDamageSource().getDamageType().getKey().toString();
        var entity = Rapunzel.entities().require(event.getEntity());

        // Non-player entity-sourced damage carries the damager (arrow, mob,
        // TNT, ...); block/environmental damage has none. Player damagers are
        // skipped above (AttackEntityPre covers those).
        REntity damager = event instanceof EntityDamageByEntityEvent byEntity
                ? Rapunzel.entities().require(byEntity.getDamager())
                : null;

        EntityHurtPre pre = new EntityHurtPre(entity, damageType, damager, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onWorldLoadPost(WorldLoadEvent event) {
        if (!bus.hasPostListeners(WorldLoadPost.class)) return;
        bus.dispatchPost(new WorldLoadPost(worldRef(event.getWorld())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChunkUnloadPost(ChunkUnloadEvent event) {
        if (!bus.hasPostListeners(ChunkUnloadPost.class)) return;
        bus.dispatchPost(new ChunkUnloadPost(
                worldRef(event.getWorld()),
                event.getChunk().getX(),
                event.getChunk().getZ()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerQuitPost(PlayerQuitEvent event) {
        if (bus.hasPostListeners(PlayerQuitPost.class)) {
            bus.dispatchPost(new PlayerQuitPost(event.getPlayer().getUniqueId(), event.getPlayer().getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerJoinPost(PlayerJoinEvent event) {
        if (bus.hasPostListeners(PlayerJoinPost.class)) {
            bus.dispatchPost(new PlayerJoinPost(event.getPlayer().getUniqueId(), event.getPlayer().getName()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerMessagePre(AsyncChatEvent event) {
        if (!bus.hasPreListeners(PlayerMessagePre.class)) return;

        String content = PlainTextComponentSerializer.plainText().serialize(event.message());
        PlayerMessagePre pre = new PlayerMessagePre(
            Rapunzel.players().require(event.getPlayer()),
            content,
            false,
            event.isCancelled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerMessagePost(AsyncChatEvent event) {
        if (!bus.hasPostListeners(PlayerMessagePost.class)) return;

        String content = PlainTextComponentSerializer.plainText().serialize(event.message());
        bus.dispatchPost(new PlayerMessagePost(
            Rapunzel.players().require(event.getPlayer()),
            content,
            false,
            event.isCancelled()
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerCommandPre(PlayerCommandPreprocessEvent event) {
        if (!bus.hasPreListeners(PlayerMessagePre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        String content = event.getMessage();
        PlayerMessagePre pre = new PlayerMessagePre(player, content, true, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerCommandPost(PlayerCommandPreprocessEvent event) {
        if (!bus.hasPostListeners(PlayerMessagePost.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        bus.dispatchPost(new PlayerMessagePost(player, event.getMessage(), true, event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerLoginPre(PlayerLoginEvent event) {
        if (!bus.hasPreListeners(PlayerLoginPre.class)) return;

        // Dispatched from PlayerLoginEvent (main thread, live player) rather
        // than AsyncPlayerPreLoginEvent so the payload can carry the player:
        // permission-gated login policies (maintenance mode etc.) need it.
        org.bukkit.entity.Player bukkitPlayer = event.getPlayer();
        PlayerLoginPre pre = new PlayerLoginPre(
            bukkitPlayer.getName(),
            bukkitPlayer.getUniqueId(),
            Rapunzel.players().require(bukkitPlayer),
            event.getResult() == PlayerLoginEvent.Result.KICK_OTHER
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                pre.denyReason().orElse(Component.text("Disconnected"))
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityMove(EntityMoveEvent event) {
        if (!bus.hasPostListeners(EntityMovePost.class)) return;

        var entity = Rapunzel.entities().require(event.getEntity());

        bus.dispatchPost(new EntityMovePost(
                entity,
                fromBukkit(event.getFrom()),
                fromBukkit(event.getTo())
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityTeleportPre(EntityTeleportEvent event) {
        if (!bus.hasPreListeners(EntityTeleportPre.class)) return;

        var entity = Rapunzel.entities().require(event.getEntity());

        EntityTeleportPre pre = new EntityTeleportPre(
                entity,
                fromBukkit(event.getFrom()),
                fromBukkit(event.getTo()),
                EntityTeleportCause.UNKNOWN,
                event.isCancelled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerTeleportPre(PlayerTeleportEvent event) {
        // Player teleports fire PlayerTeleportEvent on Paper, never
        // EntityTeleportEvent (PlayerTeleportEvent does not even extend it in
        // recent versions), so player teleports must be dispatched here as
        // well - same asymmetry the EntityTeleportPost bridge documents.
        if (!bus.hasPreListeners(EntityTeleportPre.class)) return;

        var entity = Rapunzel.entities().require(event.getPlayer());

        EntityTeleportPre pre = new EntityTeleportPre(
                entity,
                fromBukkit(event.getFrom()),
                fromBukkit(event.getTo()),
                mapTeleportCause(event.getCause()),
                event.isCancelled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    private static EntityTeleportCause mapTeleportCause(PlayerTeleportEvent.TeleportCause cause) {
        if (cause == null) return EntityTeleportCause.UNKNOWN;
        // Name-based mapping: newer TeleportCause constants (CONSUMABLE_EFFECT,
        // DISMOUNT, EXIT_BED) do not exist on every multiversion target, so a
        // compile-time switch would break the 1.21.x builds.
        for (EntityTeleportCause mapped : EntityTeleportCause.values()) {
            if (mapped.name().equals(cause.name())) {
                return mapped;
            }
        }
        return EntityTeleportCause.UNKNOWN;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!bus.hasPostListeners(EntityTeleportPost.class)) return;

        var entity = Rapunzel.entities().require(event.getEntity());

        bus.dispatchPost(new EntityTeleportPost(
                entity,
                fromBukkit(event.getFrom()),
                fromBukkit(event.getTo())
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Player teleports fire PlayerTeleportEvent on Paper, never
        // EntityTeleportEvent (PlayerTeleportEvent does not even extend it in
        // recent versions), so player teleports must be dispatched here as
        // well - otherwise EntityTeleportPost subscribers would never see them.
        if (!bus.hasPostListeners(EntityTeleportPost.class)) return;

        var entity = Rapunzel.entities().require(event.getPlayer());

        bus.dispatchPost(new EntityTeleportPost(
                entity,
                fromBukkit(event.getFrom()),
                fromBukkit(event.getTo())
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!bus.hasPostListeners(EntityMovePost.class)) return;

        // Dispatch for the moving vehicle itself, not its passengers.
        var entity = Rapunzel.entities().require(event.getVehicle());

        bus.dispatchPost(new EntityMovePost(
                entity,
                fromBukkit(event.getFrom()),
                fromBukkit(event.getTo())
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTntPrimePre(TNTPrimeEvent event) {
        if (!bus.hasPreListeners(TntPrimePre.class)) return;

        RBlock block = Rapunzel.blocks().require(event.getBlock());
        String cause = event.getCause().name();
        RPlayer player = event.getPrimingEntity() instanceof org.bukkit.entity.Player p ? Rapunzel.players().require(p) : null;

        TntPrimePre pre = new TntPrimePre(block, cause, player, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonExtendPre(BlockPistonExtendEvent event) {
        if (!bus.hasPreListeners(PistonMovePre.class)) return;
        dispatchPistonMove(event.getBlock(), event.getBlocks(), PistonMovePre.Action.EXTEND,
                event.isCancelled(), cancelled -> event.setCancelled(cancelled));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonRetractPre(BlockPistonRetractEvent event) {
        if (!bus.hasPreListeners(PistonMovePre.class)) return;
        dispatchPistonMove(event.getBlock(), event.getBlocks(), PistonMovePre.Action.RETRACT,
                event.isCancelled(), cancelled -> event.setCancelled(cancelled));
    }

    private void dispatchPistonMove(org.bukkit.block.Block piston,
                                    List<org.bukkit.block.Block> moved,
                                    PistonMovePre.Action action,
                                    boolean wasCancelled,
                                    java.util.function.Consumer<Boolean> cancel) {
        if (!(piston.getBlockData() instanceof org.bukkit.block.data.Directional directional)) return;
        org.bukkit.block.BlockFace facing = directional.getFacing();
        // Extend pushes blocks in the piston's facing; a sticky retract pulls them
        // back toward the piston (opposite of the facing).
        org.bukkit.block.BlockFace move = action == PistonMovePre.Action.EXTEND
                ? facing : facing.getOppositeFace();

        de.t14d3.rapunzellib.objects.RWorldRef world = new de.t14d3.rapunzellib.objects.RWorldRef(
                piston.getWorld().getName(), piston.getWorld().getKey().toString());
        List<RBlockPos> sources = new ArrayList<>(moved.size());
        List<RBlockPos> destinations = new ArrayList<>(moved.size());
        for (org.bukkit.block.Block b : moved) {
            sources.add(new RBlockPos(b.getX(), b.getY(), b.getZ()));
            destinations.add(new RBlockPos(
                    b.getX() + move.getModX(),
                    b.getY() + move.getModY(),
                    b.getZ() + move.getModZ()));
        }
        PistonMovePre pre = new PistonMovePre(world, sources, destinations, action, wasCancelled);
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            cancel.accept(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplodePre(EntityExplodeEvent event) {
        if (!bus.hasPreListeners(ExplosionPre.class)) return;

        String sourceTypeKey = event.getEntityType().getKey().toString();
        RLocation origin = fromBukkit(event.getLocation());

        List<RBlockPos> affected = new ArrayList<>();
        for (var b : event.blockList()) {
            affected.add(new RBlockPos(b.getX(), b.getY(), b.getZ()));
        }

        ExplosionPre pre = new ExplosionPre(origin, sourceTypeKey, ExplosionSourceKind.ENTITY, affected, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
            return;
        }

        Set<String> allowed = new HashSet<>();
        for (RBlockPos p : pre.affectedBlocks()) {
            allowed.add(p.x() + "," + p.y() + "," + p.z());
        }
        event.blockList().removeIf(b -> !allowed.contains(b.getX() + "," + b.getY() + "," + b.getZ()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockExplodePre(BlockExplodeEvent event) {
        if (!bus.hasPreListeners(ExplosionPre.class)) return;

        RLocation origin = fromBukkit(event.getBlock().getLocation());
        String sourceTypeKey = event.getExplodedBlockState().getType().getKey().toString();

        List<RBlockPos> affected = new ArrayList<>();
        for (var b : event.blockList()) {
            affected.add(new RBlockPos(b.getX(), b.getY(), b.getZ()));
        }

        ExplosionPre pre = new ExplosionPre(origin, sourceTypeKey, ExplosionSourceKind.BLOCK, affected, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
            return;
        }

        Set<String> allowed = new HashSet<>();
        for (RBlockPos p : pre.affectedBlocks()) {
            allowed.add(p.x() + "," + p.y() + "," + p.z());
        }
        event.blockList().removeIf(b -> !allowed.contains(b.getX() + "," + b.getY() + "," + b.getZ()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketFillPre(PlayerBucketFillEvent event) {
        if (!bus.hasPreListeners(BucketFillPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getBlockClicked());

        BucketFillPre pre = new BucketFillPre(player, block, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketEmptyPre(PlayerBucketEmptyEvent event) {
        if (!bus.hasPreListeners(BucketEmptyPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RItemType type = RItemType.require(RKey.of(event.getBucket().key().namespace(), event.getBucket().key().value()));

        BucketEmptyPre pre = new BucketEmptyPre(player, fromBukkit(event.getBlock().getLocation()), type, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketEntityPre(PlayerBucketEntityEvent event) {
        if (!bus.hasPreListeners(BucketEntityPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        var entity = Rapunzel.entities().require(event.getEntity());

        BucketEntityPre pre = new BucketEntityPre(player, fromBukkit(event.getEntity().getLocation()), entity, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHangingPlacePre(HangingPlaceEvent event) {
        if (!bus.hasPreListeners(BlockPlacePre.class)) return;
        if (event.getPlayer() == null) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getEntity().getLocation().getBlock());

        BlockPlacePre pre = new BlockPlacePre(player, block, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHangingBreakPre(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof org.bukkit.entity.Player remover)) return;
        if (!bus.hasPreListeners(AttackEntityPre.class)) return;

        RPlayer player = Rapunzel.players().require(remover);
        var entity = Rapunzel.entities().require(event.getEntity());

        AttackEntityPre pre = new AttackEntityPre(player, entity, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityPlacePre(EntityPlaceEvent event) {
        if (!bus.hasPreListeners(BlockPlacePre.class)) return;
        if (event.getPlayer() == null) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getEntity().getLocation().getBlock());

        BlockPlacePre pre = new BlockPlacePre(player, block, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onArmorStandManipulatePre(PlayerArmorStandManipulateEvent event) {
        if (!bus.hasPreListeners(InteractEntityPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        var entity = Rapunzel.entities().require(event.getRightClicked());

        InteractEntityPre pre = new InteractEntityPre(player, entity, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPhysicsPre(BlockPhysicsEvent event) {
        if (!bus.hasPreListeners(BlockPhysicsPre.class)) return;

        RBlock block = Rapunzel.blocks().require(event.getBlock());
        // The trigger block: Paper 26.x fires BlockPhysicsEvent from
        // NeighborUpdater.executeUpdate with the changed/source block as
        // getSourceBlock() (getChangedType() holds the state of the block
        // being updated itself). Matches the shared mixin hook, which reports
        // the changed block as changedType.
        RBlock changed = Rapunzel.blocks().require(event.getSourceBlock());
        RKey changedTypeKey = changed.typeKey();
        BlockPhysicsPre pre = new BlockPhysicsPre(block, changedTypeKey, event.isCancelled());
        bus.dispatchPre(pre);

        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPhysicsPost(BlockPhysicsEvent event) {
        if (!bus.hasPostListeners(BlockPhysicsPost.class)) return;

        RBlock block = Rapunzel.blocks().require(event.getBlock());
        RBlock changed = Rapunzel.blocks().require(event.getSourceBlock());
        RBlockType changedType = RBlockType.require(changed.typeKey());
        bus.dispatchPost(new BlockPhysicsPost(block, changedType, event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockFormPre(BlockFormEvent event) {
        if (!bus.hasPreListeners(BlockFormPre.class)) return;

        RBlock block = Rapunzel.blocks().require(event.getBlock());
        RKey newBlockTypeKey = RKey.of(event.getNewState().getType().getKey().toString());

        BlockFormPre pre = new BlockFormPre(block, newBlockTypeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockDestroyPre(BlockDestroyEvent event) {
        if (!bus.hasPreListeners(BlockDestroyPre.class)) return;

        RBlock block = Rapunzel.blocks().require(event.getBlock());
        RKey replacementTypeKey = RKey.of(event.getNewState().getMaterial().getKey().toString());

        BlockDestroyPre pre = new BlockDestroyPre(block, replacementTypeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBurnPre(BlockBurnEvent event) {
        if (!bus.hasPreListeners(BlockDestroyPre.class)) return;

        RBlock block = Rapunzel.blocks().require(event.getBlock());
        // Fire always replaces the burned block with air (or the fluid
        // state if waterlogged, but the DESTROY flag only cares about the
        // block being destroyed, not the replacement).
        RKey replacementTypeKey = RKey.of("minecraft:air");

        BlockDestroyPre pre = new BlockDestroyPre(block, replacementTypeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockSpreadPre(BlockSpreadEvent event) {
        if (!bus.hasPreListeners(BlockSpreadPre.class)) return;

        RBlock block = Rapunzel.blocks().require(event.getBlock());
        RBlock source = Rapunzel.blocks().require(event.getSource());

        BlockSpreadPre pre = new BlockSpreadPre(block, source, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteractEntityPost(PlayerInteractEntityEvent event) {
        if (!bus.hasPostListeners(InteractEntityPost.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        var entity = Rapunzel.entities().require(event.getRightClicked());

        bus.dispatchPost(EntityEventPayloads.interactPost(player, entity, event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onAttackEntityPost(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof org.bukkit.entity.Player damager)) return;
        if (!bus.hasPostListeners(AttackEntityPost.class)) return;

        RPlayer player = Rapunzel.players().require(damager);
        var entity = Rapunzel.entities().require(event.getEntity());

        bus.dispatchPost(EntityEventPayloads.attackPost(player, entity, event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntitySpawnPost(CreatureSpawnEvent event) {
        boolean needsPost = bus.hasPostListeners(EntitySpawnPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntitySpawnSnapshot.class);
        if (!needsPost && !needsAsync) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;

        String reason = event.getSpawnReason().name();
        boolean cancelled = event.isCancelled();
        var entity = Rapunzel.entities().require(event.getEntity());

        if (needsPost && !cancelled) {
            bus.dispatchPost(EntityEventPayloads.spawnPost(entity, reason, false));
        }
        if (needsAsync) {
            bus.dispatchAsync(EntityEventPayloads.spawnSnapshot(entity, reason, cancelled));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityHurtPost(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof org.bukkit.entity.Player) {
            return;
        }
        boolean needsPost = bus.hasPostListeners(EntityHurtPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntityHurtSnapshot.class);
        if (!needsPost && !needsAsync) return;

        String damageType = event.getDamageSource().getDamageType().getKey().toString();
        boolean cancelled = event.isCancelled();
        var entity = Rapunzel.entities().require(event.getEntity());

        if (needsPost) {
            bus.dispatchPost(EntityEventPayloads.hurtPost(entity, damageType, cancelled));
        }
        if (needsAsync) {
            bus.dispatchAsync(EntityEventPayloads.hurtSnapshot(entity, damageType, cancelled));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDeathPre(PlayerDeathEvent event) {
        if (!bus.hasPreListeners(EntityDeathPre.class)) return;

        var player = Rapunzel.entities().require(event.getEntity());
        var killer = event.getDamageSource().getCausingEntity() != null
            ? Rapunzel.entities().require(event.getDamageSource().getCausingEntity())
            : null;
        String cause = event.getDamageSource().getDamageType().getKey().toString();
        RLocation position = fromBukkit(event.getEntity().getLocation());

        EntityDeathPre pre = new EntityDeathPre(player, killer, cause, position, event.deathMessage(), event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDeathPreGeneric(EntityDeathEvent event) {
        // Player deaths are handled by onEntityDeathPre (cancellable via PlayerDeathEvent).
        if (event instanceof PlayerDeathEvent) return;
        if (!bus.hasPreListeners(EntityDeathPre.class)) return;

        var entity = Rapunzel.entities().require(event.getEntity());
        var killer = event.getDamageSource().getCausingEntity() != null
            ? Rapunzel.entities().require(event.getDamageSource().getCausingEntity())
            : null;
        String cause = event.getDamageSource().getDamageType().getKey().toString();
        RLocation position = fromBukkit(event.getEntity().getLocation());

        // Denial cannot be honored here: EntityDeathEvent is not cancellable.
        // See EntityDeathPre javadoc for the platform caveat.
        bus.dispatchPre(new EntityDeathPre(entity, killer, cause, position, null, event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDeathPost(PlayerDeathEvent event) {
        if (!bus.hasPostListeners(EntityDeathPost.class)) return;

        var player = Rapunzel.entities().require(event.getEntity());
        var killer = event.getDamageSource().getCausingEntity() != null
            ? Rapunzel.entities().require(event.getDamageSource().getCausingEntity())
            : null;
        String cause = event.getDamageSource().getDamageType().getKey().toString();
        RLocation position = fromBukkit(event.getEntity().getLocation());

        bus.dispatchPost(new EntityDeathPost(player, killer, cause, position, event.deathMessage(), event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDeathPostGeneric(EntityDeathEvent event) {
        if (event instanceof PlayerDeathEvent) return;
        if (!bus.hasPostListeners(EntityDeathPost.class)) return;

        var entity = Rapunzel.entities().require(event.getEntity());
        var killer = event.getDamageSource().getCausingEntity() != null
            ? Rapunzel.entities().require(event.getDamageSource().getCausingEntity())
            : null;
        String cause = event.getDamageSource().getDamageType().getKey().toString();
        RLocation position = fromBukkit(event.getEntity().getLocation());

        bus.dispatchPost(new EntityDeathPost(entity, killer, cause, position, null, event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityTamePost(EntityTameEvent event) {
        if (!bus.hasPostListeners(EntityTamePost.class)) return;

        var tamed = Rapunzel.entities().require(event.getEntity());
        // The tamer is a player during the tame interaction; other AnimalTamer
        // implementations (e.g. offline players) cannot be resolved to an RPlayer.
        if (event.getOwner() instanceof org.bukkit.entity.Player owner) {
            bus.dispatchPost(new EntityTamePost(Rapunzel.players().require(owner), tamed));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onArmorStandManipulatePost(PlayerArmorStandManipulateEvent event) {
        if (!bus.hasPostListeners(InteractEntityPost.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        var entity = Rapunzel.entities().require(event.getRightClicked());

        bus.dispatchPost(EntityEventPayloads.interactPost(player, entity, event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerMovePre(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!bus.hasPreListeners(PlayerMovePre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RLocation from = fromBukkit(event.getFrom());
        RLocation to = fromBukkit(event.getTo());

        // Library-scope throttle (events.player.move config): min-distance
        // since the last dispatched move + per-player rate limit. Replaces the
        // former block-difference filter.
        if (!PlayerMoveThrottle.shouldDispatch(player.uuid(), from, to)) return;

        PlayerMovePre pre = new PlayerMovePre(player, from, to, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerMovePost(org.bukkit.event.player.PlayerMoveEvent event) {
        boolean needsPost = bus.hasPostListeners(PlayerMovePost.class);
        if (!needsPost) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RLocation from = fromBukkit(event.getFrom());
        RLocation to = fromBukkit(event.getTo());

        // Only dispatch a post for the move whose pre passed the throttle,
        // keeping pre/post paired exactly.
        if (!PlayerMoveThrottle.wasAccepted(player.uuid(), from, to)) return;

        bus.dispatchPost(new PlayerMovePost(player, from, to, event.isCancelled()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryOpenPre(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPreListeners(InventoryOpenPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().require(event.getInventory());

        InventoryOpenPre pre = new InventoryOpenPre(rPlayer, rInventory);
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryOpenPost(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPostListeners(InventoryOpenPost.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().require(event.getInventory());

        bus.dispatchPost(new InventoryOpenPost(rPlayer, rInventory));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClosePost(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPostListeners(InventoryClosePost.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().require(event.getInventory());

        bus.dispatchPost(new InventoryClosePost(rPlayer, rInventory));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryActionPre(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPreListeners(InventoryActionPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        int slot = event.getRawSlot();
        // Full combined menu view (top container + player inventory section):
        // raw slots index the full menu, identical to getRawSlot() semantics.
        RInventory rInventory = InventoryFeatures.install().require(event.getView());
        InventoryActionType actionType = mapBukkitAction(event.getClick());

        // currentItem = full-menu wrap's item at the raw slot
        // (menu.getSlot(rawSlot).getItem()), guarded by 0 <= rawSlot < size.
        // The wrap covers the FULL menu including the player's own CRAFTING
        // view (26.2: top is a CraftingInventory with 5 slots, main/hotbar
        // raw slots 9+ fall outside any top-inventory wrap).
        Integer hotbarButton = event.getHotbarButton() >= 0 ? event.getHotbarButton() : null;
        InventoryActionPre pre = new InventoryActionPre(
            rPlayer,
            rInventory,
            List.of(slot),
            actionType,
            wrapItemStack(event.getCursor()),
            slotItem(rInventory, slot),
            hotbarButton,
            event.isCancelled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryActionPost(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPostListeners(InventoryActionPost.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        int slot = event.getRawSlot();
        RInventory rInventory = InventoryFeatures.install().require(event.getView());
        InventoryActionType actionType = mapBukkitAction(event.getClick());

        // Same currentItem source as the pre-dispatch: the full-menu wrap at
        // the raw slot (Bukkit's getCurrentItem() equivalent).
        Integer hotbarButton = event.getHotbarButton() >= 0 ? event.getHotbarButton() : null;
        bus.dispatchPost(new InventoryActionPost(
            rPlayer,
            rInventory,
            List.of(slot),
            actionType,
            wrapItemStack(event.getCursor()),
            slotItem(rInventory, slot),
            hotbarButton,
            event.isCancelled()
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDragPre(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPreListeners(InventoryActionPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().require(event.getView());
        List<Integer> slots = event.getRawSlots().stream().sorted().toList();

        InventoryActionPre pre = new InventoryActionPre(
            rPlayer,
            rInventory,
            slots,
            InventoryActionType.DRAG,
            wrapItemStack(event.getOldCursor()),
            firstSlotItem(rInventory, slots),
            event.isCancelled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryDragPost(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPostListeners(InventoryActionPost.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().require(event.getView());
        List<Integer> slots = event.getRawSlots().stream().sorted().toList();

        bus.dispatchPost(new InventoryActionPost(
            rPlayer,
            rInventory,
            slots,
            InventoryActionType.DRAG,
            wrapItemStack(event.getOldCursor()),
            firstSlotItem(rInventory, slots),
            null,
            event.isCancelled()
        ));
    }

    /**
     * Dispatches {@link InventoryTransferPre} from
     * {@link InventoryMoveItemEvent}, which Paper fires for every hopper /
     * minecart-hopper / dropper inventory-to-inventory transfer in both
     * directions (the payload's sourcePos is always the inventory the item
     * leaves - {@code getSource()}).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryTransferPre(InventoryMoveItemEvent event) {
        if (!bus.hasPreListeners(InventoryTransferPre.class)) return;

        org.bukkit.inventory.Inventory source = event.getSource();
        org.bukkit.inventory.Inventory destination = event.getDestination();
        org.bukkit.inventory.Inventory initiator = event.getInitiator();

        RWorldRef world = inventoryWorld(source, destination, initiator);
        if (world == null) return;

        RItem item = wrapItemStack(event.getItem());
        if (item == null) return;

        InventoryTransferPre pre = new InventoryTransferPre(
            world,
            holderPos(source),
            holderPos(destination),
            item,
            event.getItem().getAmount(),
            transferSource(initiator),
            event.isCancelled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    /**
     * Resolves the block position of a block-backed inventory (hoppers,
     * droppers, chests, ...) from its holder, or null when the holder is not a
     * block (hopper minecart - null per the position contract) or carries no
     * position (unresolvable holders).
     */
    private static RBlockPos holderPos(org.bukkit.inventory.Inventory inventory) {
        if (inventory == null) return null;
        org.bukkit.inventory.InventoryHolder holder = inventory.getHolder();
        if (holder == null) return null;
        org.bukkit.Location loc;
        if (holder instanceof org.bukkit.block.BlockState state) {
            loc = state.getLocation();
        } else if (holder instanceof org.bukkit.block.DoubleChest chest) {
            loc = chest.getLocation();
        } else {
            return null;
        }
        return loc == null || loc.getWorld() == null ? null
                : new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * Maps the moving carrier to the shared {@link TransferSource}: the
     * initiator is always the hopper / minecart hopper / dropper that starts
     * the move (Bukkit's {@code getInitiator()}).
     */
    private static TransferSource transferSource(org.bukkit.inventory.Inventory initiator) {
        if (initiator == null) return TransferSource.HOPPER;
        if (initiator.getHolder() instanceof org.bukkit.entity.minecart.HopperMinecart) {
            return TransferSource.HOPPER_MINECART;
        }
        if (initiator.getType() == org.bukkit.event.inventory.InventoryType.DROPPER) {
            return TransferSource.DROPPER;
        }
        return TransferSource.HOPPER;
    }

    /** Resolves the transfer world from the holders of the involved inventories (source first). */
    private RWorldRef inventoryWorld(
        org.bukkit.inventory.Inventory source,
        org.bukkit.inventory.Inventory destination,
        org.bukkit.inventory.Inventory initiator
    ) {
        org.bukkit.World world = holderWorld(source);
        if (world == null) world = holderWorld(destination);
        if (world == null) world = holderWorld(initiator);
        return world == null ? null : worldRef(world);
    }

    private static org.bukkit.World holderWorld(org.bukkit.inventory.Inventory inventory) {
        if (inventory == null) return null;
        org.bukkit.inventory.InventoryHolder holder = inventory.getHolder();
        if (holder instanceof org.bukkit.block.BlockState state) return state.getWorld();
        if (holder instanceof org.bukkit.block.DoubleChest chest) return chest.getWorld();
        if (holder instanceof org.bukkit.entity.Entity entity) return entity.getWorld();
        return null;
    }

    /** Returns the item in a single raw slot of the full-menu wrap, or null when out of bounds. */
    private static RItem slotItem(RInventory inventory, int slot) {
        if (slot < 0 || slot >= inventory.size()) return null;
        return inventory.item(slot).orElse(null);
    }

    /** Returns the item in the first in-bounds raw slot, or null. */
    private static RItem firstSlotItem(RInventory inventory, List<Integer> slots) {
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.size()) {
                return inventory.item(slot).orElse(null);
            }
        }
        return null;
    }

    /** Wraps a Bukkit item stack as an {@link RItem}, or null when unavailable. */
    private static RItem wrapItemStack(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        try {
            return NbtFeatures.itemStackAdapter(org.bukkit.inventory.ItemStack.class).snapshot(stack);
        } catch (RuntimeException ignored) {
            // Item-stack adapters may not be installed; cursor/current refs are nullable.
            return null;
        }
    }

    private static InventoryActionType mapBukkitAction(org.bukkit.event.inventory.ClickType bukkitClick) {
        if (bukkitClick == null) return InventoryActionType.UNKNOWN;
        return switch (bukkitClick) {
            case LEFT -> InventoryActionType.LEFT;
            case RIGHT -> InventoryActionType.RIGHT;
            case SHIFT_LEFT -> InventoryActionType.SHIFT_LEFT;
            case SHIFT_RIGHT -> InventoryActionType.SHIFT_RIGHT;
            case MIDDLE -> InventoryActionType.MIDDLE;
            case DOUBLE_CLICK -> InventoryActionType.DOUBLE_CLICK;
            case DROP -> InventoryActionType.DROP;
            case CONTROL_DROP -> InventoryActionType.CONTROL_DROP;
            case NUMBER_KEY -> InventoryActionType.NUMBER_KEY;
            case CREATIVE -> InventoryActionType.CREATIVE;
            case SWAP_OFFHAND -> InventoryActionType.SWAP_OFFHAND;
            default -> InventoryActionType.UNKNOWN;
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (!bus.hasPostListeners(PlayerStatePost.class)) return;
        RPlayer player = Rapunzel.players().require(event.getPlayer());
        bus.dispatchPost(new PlayerStatePost(
            player,
            snapshot(event.getPlayer(), RGameMode.valueOf(event.getNewGameMode().name()), null, null, null, null),
            Set.of(PlayerStatePost.StateField.GAMEMODE)
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!bus.hasPostListeners(PlayerStatePost.class)) return;
        RPlayer player = Rapunzel.players().require(event.getPlayer());
        bus.dispatchPost(new PlayerStatePost(
            player,
            snapshot(event.getPlayer(), null, event.isSneaking(), null, null, null),
            Set.of(PlayerStatePost.StateField.SNEAKING)
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!bus.hasPostListeners(PlayerStatePost.class)) return;
        RPlayer player = Rapunzel.players().require(event.getPlayer());
        bus.dispatchPost(new PlayerStatePost(
            player,
            snapshot(event.getPlayer(), null, null, event.isFlying(), null, null),
            Set.of(PlayerStatePost.StateField.FLYING)
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!bus.hasPostListeners(PlayerStatePost.class)) return;
        RPlayer player = Rapunzel.players().require(event.getPlayer());
        bus.dispatchPost(new PlayerStatePost(
            player,
            snapshot(event.getPlayer(), null, null, null, event.isSprinting(), null),
            Set.of(PlayerStatePost.StateField.SPRINTING)
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onVehicleExit(org.bukkit.event.vehicle.VehicleExitEvent event) {
        if (!bus.hasPostListeners(PlayerStatePost.class)) return;
        if (!(event.getExited() instanceof org.bukkit.entity.Player bukkitPlayer)) return;
        RPlayer player = Rapunzel.players().require(bukkitPlayer);
        // Paper clears the passenger's vehicle reference before firing
        // VehicleExitEvent, so the live read at dispatch already reports
        // getVehicle() == null.
        bus.dispatchPost(new PlayerStatePost(
            player,
            snapshot(bukkitPlayer, null, null, null, null, bukkitPlayer.getVehicle() != null),
            Set.of(PlayerStatePost.StateField.RIDING)
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onVehicleEnter(org.bukkit.event.vehicle.VehicleEnterEvent event) {
        if (!bus.hasPostListeners(PlayerStatePost.class)) return;
        if (!(event.getEntered() instanceof org.bukkit.entity.Player bukkitPlayer)) return;
        RPlayer player = Rapunzel.players().require(bukkitPlayer);
        bus.dispatchPost(new PlayerStatePost(
            player,
            snapshot(bukkitPlayer, null, null, null, null, true),
            Set.of(PlayerStatePost.StateField.RIDING)
        ));
    }

    /**
     * Builds a full player-state snapshot; null overrides keep the player's
     * live value, non-null overrides carry the changed-field value from the
     * source event.
     */
    private static PlayerStatePost.PlayerStateSnapshot snapshot(
        org.bukkit.entity.Player player,
        RGameMode gamemode,
        Boolean sneaking,
        Boolean flying,
        Boolean sprinting,
        Boolean riding
    ) {
        return new PlayerStatePost.PlayerStateSnapshot(
            gamemode != null ? gamemode : RGameMode.valueOf(player.getGameMode().name()),
            sneaking != null ? sneaking : player.isSneaking(),
            flying != null ? flying : player.isFlying(),
            sprinting != null ? sprinting : player.isSprinting(),
            riding != null ? riding : player.getVehicle() != null
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockTransformPre(BlockFadeEvent event) {
        // BlockFadeEvent is the Bukkit surface for block transformations
        // (e.g. copper oxidation); it carries the target state.
        if (!bus.hasPreListeners(BlockTransformPre.class)) return;

        RBlock block = Rapunzel.blocks().require(event.getBlock());
        RKey newTypeKey = RKey.of(event.getNewState().getType().getKey().toString());

        BlockTransformPre pre = new BlockTransformPre(block, newTypeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }
}
