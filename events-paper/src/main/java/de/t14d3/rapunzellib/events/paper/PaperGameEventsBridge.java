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
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
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

@SuppressWarnings("deprecation")
final class PaperGameEventsBridge implements Listener, GameEventBridge {
    private final JavaPlugin plugin;
    private final GameEventBus bus;

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

        BlockPlacePre pre = new BlockPlacePre(player, block.world().ref(), block.pos(), block.typeKey(), event.isCancelled());
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

        RWorldRef world = block.world().ref();
        RBlockPos pos = block.pos();
        RKey typeKey = block.typeKey();

        if (needsPost) {
            bus.dispatchPost(new BlockPlacePost(player, world, pos, typeKey, cancelled));
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
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;

        Location loc = event.getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RKey typeKey = RKey.of(event.getEntityType().getKey().toString());
        String reason = event.getSpawnReason().name();

        EntitySpawnPre pre = new EntitySpawnPre(world, pos, typeKey, reason, event.isCancelled());
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
        bus.dispatchPost(new WorldLoadPost(new RWorldRef(event.getWorld().getName(), event.getWorld().getKey().toString())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChunkUnloadPost(ChunkUnloadEvent event) {
        if (!bus.hasPostListeners(ChunkUnloadPost.class)) return;
        bus.dispatchPost(new ChunkUnloadPost(
                new RWorldRef(event.getWorld().getName(), event.getWorld().getKey().toString()),
                event.getChunk().getX(),
                event.getChunk().getZ()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerQuitPost(PlayerQuitEvent event) {
        if (bus.hasPostListeners(PlayerQuitPost.class)) {
            bus.dispatchPost(new PlayerQuitPost(event.getPlayer().getUniqueId(), event.getPlayer().getName()));
        }
        if (bus.hasPostListeners(PlayerQuitPre.class)) {
            bus.dispatchPost(new PlayerQuitPre(event.getPlayer().getUniqueId(), event.getPlayer().getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityMove(EntityMoveEvent event) {
        if (!bus.hasPostListeners(EntityMovePost.class)) return;

        var entity = Rapunzel.entities().require(event.getEntity());
        Location from = event.getFrom();
        Location to = event.getTo();
        RWorldRef world = new RWorldRef(from.getWorld().getName(), from.getWorld().getKey().toString());
        RLocation fromLoc = new RLocation(world, from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        RLocation toLoc = new RLocation(world, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());

        bus.dispatchPost(new EntityMovePost(entity.uuid(), entity.typeKey(), fromLoc, toLoc));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!bus.hasPostListeners(EntityTeleportPost.class)) return;

        var entity = Rapunzel.entities().require(event.getEntity());
        Location from = event.getFrom();
        Location to = event.getTo();
        RWorldRef world = new RWorldRef(from.getWorld().getName(), from.getWorld().getKey().toString());
        RLocation fromLoc = new RLocation(world, from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        RLocation toLoc = new RLocation(world, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());

        bus.dispatchPost(new EntityTeleportPost(entity.uuid(), entity.typeKey(), fromLoc, toLoc));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!bus.hasPostListeners(EntityMovePost.class)) return;

        for (org.bukkit.entity.Entity passenger : event.getVehicle().getPassengers()) {
            var entity = Rapunzel.entities().require(passenger);
            Location from = event.getFrom();
            Location to = event.getTo();
            RWorldRef world = new RWorldRef(from.getWorld().getName(), from.getWorld().getKey().toString());
            RLocation fromLoc = new RLocation(world, from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
            RLocation toLoc = new RLocation(world, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());

            bus.dispatchPost(new EntityMovePost(entity.uuid(), entity.typeKey(), fromLoc, toLoc));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTntPrimePre(TNTPrimeEvent event) {
        if (!bus.hasPreListeners(TntPrimePre.class)) return;

        Location loc = event.getBlock().getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RKey blockTypeKey = RKey.of(event.getBlock().getType().getKey().toString());
        String cause = event.getCause().name();
        RPlayer player = event.getPrimingEntity() instanceof org.bukkit.entity.Player p ? Rapunzel.players().require(p) : null;

        TntPrimePre pre = new TntPrimePre(world, pos, blockTypeKey, cause, player, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplodePre(EntityExplodeEvent event) {
        if (!bus.hasPreListeners(ExplosionPre.class)) return;

        Location loc = event.getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos origin = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        String sourceTypeKey = event.getEntityType().getKey().toString();

        List<RBlockPos> affected = new ArrayList<>();
        for (var b : event.blockList()) {
            affected.add(new RBlockPos(b.getX(), b.getY(), b.getZ()));
        }

        ExplosionPre pre = new ExplosionPre(world, origin, sourceTypeKey, affected, event.isCancelled());
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

        Location loc = event.getBlock().getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos origin = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        String sourceTypeKey = event.getExplodedBlockState().getType().getKey().toString();

        List<RBlockPos> affected = new ArrayList<>();
        for (var b : event.blockList()) {
            affected.add(new RBlockPos(b.getX(), b.getY(), b.getZ()));
        }

        ExplosionPre pre = new ExplosionPre(world, origin, sourceTypeKey, affected, event.isCancelled());
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
        Location loc = event.getBlockClicked().getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RKey blockTypeKey = RKey.of(event.getBlockClicked().getType().getKey().toString());

        BucketFillPre pre = new BucketFillPre(player, world, pos, blockTypeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketEmptyPre(PlayerBucketEmptyEvent event) {
        if (!bus.hasPreListeners(BucketEmptyPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        Location loc = event.getBlockClicked().getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        String bucketTypeKey = event.getBucket().getKey().toString();

        BucketEmptyPre pre = new BucketEmptyPre(player, world, pos, bucketTypeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketEntityPre(PlayerBucketEntityEvent event) {
        if (!bus.hasPreListeners(BucketEntityPre.class)) return;

        RPlayer player = Rapunzel.players().require(event.getPlayer());
        Location loc = event.getEntity().getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RKey entityTypeKey = RKey.of(event.getEntity().getType().getKey().toString());

        BucketEntityPre pre = new BucketEntityPre(player, world, pos, entityTypeKey, event.isCancelled());
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
        Location loc = event.getEntity().getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RKey typeKey = RKey.of(event.getEntity().getType().getKey().toString());

        BlockPlacePre pre = new BlockPlacePre(player, world, pos, typeKey, event.isCancelled());
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
        Location loc = event.getEntity().getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RKey typeKey = RKey.of(event.getEntityType().getKey().toString());

        BlockPlacePre pre = new BlockPlacePre(player, world, pos, typeKey, event.isCancelled());
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPhysicsPost(BlockPhysicsEvent event) {
        if (!bus.hasPostListeners(BlockPhysicsPost.class)) return;

        Location loc = event.getBlock().getLocation();
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RKey blockTypeKey = RKey.of(event.getBlock().getType().getKey().toString());
        int changedTypeId = event.getChangedType().getId();

        bus.dispatchPost(new BlockPhysicsPost(world, pos, blockTypeKey, changedTypeId, event.isCancelled()));
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
        Location from = event.getFrom();
        Location to = event.getTo();

        RWorldRef world = new RWorldRef(from.getWorld().getName(), from.getWorld().getKey().toString());
        RLocation fromLoc = new RLocation(world, from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        RLocation toLoc = new RLocation(world, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());

        PlayerMovePre pre = new PlayerMovePre(player, fromLoc, toLoc, event.isCancelled());
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

        UUID uuid = event.getPlayer().getUniqueId();
        String name = event.getPlayer().getName();
        Location from = event.getFrom();
        Location to = event.getTo();

        RWorldRef world = new RWorldRef(from.getWorld().getName(), from.getWorld().getKey().toString());
        RLocation fromLoc = new RLocation(world, from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        RLocation toLoc = new RLocation(world, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());

        bus.dispatchPost(new PlayerMovePost(uuid, name, fromLoc, toLoc));
    }
}
