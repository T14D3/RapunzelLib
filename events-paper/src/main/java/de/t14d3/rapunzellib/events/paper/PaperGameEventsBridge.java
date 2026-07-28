package de.t14d3.rapunzellib.events.paper;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.block.*;
import de.t14d3.rapunzellib.events.entity.*;
import de.t14d3.rapunzellib.events.interact.UseBlockPost;
import de.t14d3.rapunzellib.events.interact.UseBlockPre;
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
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    public void onUseBlockPre(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!bus.hasPreListeners(UseBlockPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getClickedBlock());

        UseBlockPre pre = new UseBlockPre(player, block, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onUseBlockPost(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        boolean needsPost = bus.hasPostListeners(UseBlockPost.class);
        boolean needsAsync = bus.hasAsyncListeners(UseBlockSnapshot.class);
        if (!needsPost && !needsAsync) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getClickedBlock());
        boolean cancelled = event.isCancelled();

        if (needsPost) {
            bus.dispatchPost(new UseBlockPost(player, block, cancelled));
        }
        if (needsAsync) {
            bus.dispatchAsync(UseBlockSnapshot.capture(player.uuid(), block, cancelled));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractBlockPre(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Action action = event.getAction();
        InteractBlockPre.Action normalized = switch (action) {
            case LEFT_CLICK_BLOCK -> InteractBlockPre.Action.LEFT_CLICK_BLOCK;
            case RIGHT_CLICK_BLOCK -> InteractBlockPre.Action.RIGHT_CLICK_BLOCK;
            default -> null;
        };
        if (normalized == null) return;
        if (!bus.hasPreListeners(InteractBlockPre.class)) return;

        InteractBlockPre.Hand hand = switch (event.getHand()) {
            case HAND -> InteractBlockPre.Hand.MAIN_HAND;
            case OFF_HAND -> InteractBlockPre.Hand.OFF_HAND;
            case null, default -> InteractBlockPre.Hand.UNKNOWN;
        };

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        RBlock block = Rapunzel.blocks().require(event.getClickedBlock());

        InteractBlockPre pre = new InteractBlockPre(player, block, normalized, hand, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
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

        String damageType = event.getDamageSource().getDamageType().toString();
        var entity = Rapunzel.entities().require(event.getEntity());

        EntityHurtPre pre = new EntityHurtPre(entity, damageType, event.isCancelled());
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
    public void onEntityMove(EntityMoveEvent event) {
        if (!bus.hasPostListeners(EntityMovePost.class)) return;

        var entity = Rapunzel.entities().require(event.getEntity());

        bus.dispatchPost(new EntityMovePost(
                entity,
                fromBukkit(event.getFrom()),
                fromBukkit(event.getTo())
        ));
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
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!bus.hasPostListeners(EntityMovePost.class)) return;

        for (org.bukkit.entity.Entity passenger : event.getVehicle().getPassengers()) {
            var entity = Rapunzel.entities().require(passenger);

            bus.dispatchPost(new EntityMovePost(
                    entity,
                    fromBukkit(event.getFrom()),
                    fromBukkit(event.getTo())
            ));
        }
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
        // getChangedType() returns the Material (block type) that changed, triggering this physics update
        RKey changedTypeKey = RKey.of(event.getChangedType().getKey().toString());
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
        RBlockType changedType = RBlockType.require(RKey.of(event.getChangedType().getKey().toString()));
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

        String damageType = event.getDamageSource().getDamageType().toString();
        boolean cancelled = event.isCancelled();
        var entity = Rapunzel.entities().require(event.getEntity());

        if (needsPost) {
            bus.dispatchPost(EntityEventPayloads.hurtPost(entity, damageType, cancelled));
        }
        if (needsAsync) {
            bus.dispatchAsync(EntityEventPayloads.hurtSnapshot(entity, damageType, cancelled));
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

        // Ignore small movements (head rotation etc.) to reduce event spam
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        RPlayer player = Rapunzel.players().require(event.getPlayer());

        PlayerMovePre pre = new PlayerMovePre(player, fromBukkit(event.getFrom()), fromBukkit(event.getTo()), event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerMovePost(org.bukkit.event.player.PlayerMoveEvent event) {
        boolean needsPost = bus.hasPostListeners(PlayerMovePost.class);
        if (!needsPost) return;

        // Ignore small movements (head rotation etc.) to reduce event spam
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        RPlayer player = Rapunzel.players().require(event.getPlayer());

        bus.dispatchPost(new PlayerMovePost(player, fromBukkit(event.getFrom()), fromBukkit(event.getTo()), event.isCancelled()));
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
    public void onInventoryClickPre(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPreListeners(InventoryClickPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        int slot = event.getRawSlot();
        RInventory rInventory = InventoryFeatures.install().require(event.getInventory());
        InventoryClickType clickType = mapBukkitClick(event.getClick());

        InventoryClickPre pre = new InventoryClickPre(rPlayer, rInventory, slot, clickType, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClickPost(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!bus.hasPostListeners(InventoryClickPost.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        int slot = event.getRawSlot();
        RInventory rInventory = InventoryFeatures.install().require(event.getInventory());
        InventoryClickType clickType = mapBukkitClick(event.getClick());

        bus.dispatchPost(new InventoryClickPost(rPlayer, rInventory, slot, clickType, event.isCancelled()));
    }

    private static InventoryClickType mapBukkitClick(org.bukkit.event.inventory.ClickType bukkitClick) {
        if (bukkitClick == null) return InventoryClickType.UNKNOWN;
        return switch (bukkitClick) {
            case LEFT -> InventoryClickType.LEFT;
            case RIGHT -> InventoryClickType.RIGHT;
            case SHIFT_LEFT -> InventoryClickType.SHIFT_LEFT;
            case SHIFT_RIGHT -> InventoryClickType.SHIFT_RIGHT;
            case MIDDLE -> InventoryClickType.MIDDLE;
            case DOUBLE_CLICK -> InventoryClickType.DOUBLE_CLICK;
            case DROP -> InventoryClickType.DROP;
            case CONTROL_DROP -> InventoryClickType.CONTROL_DROP;
            case NUMBER_KEY -> InventoryClickType.NUMBER_KEY_1;
            case SWAP_OFFHAND -> InventoryClickType.SWAP_OFFHAND;
            default -> InventoryClickType.UNKNOWN;
        };
    }
}
