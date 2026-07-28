package de.t14d3.rapunzellib.events.sponge;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.*;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.block.BlockBreakPost;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;
import de.t14d3.rapunzellib.events.block.BlockBreakSnapshot;
import de.t14d3.rapunzellib.events.block.BlockPlacePost;
import de.t14d3.rapunzellib.events.block.BlockPlacePre;
import de.t14d3.rapunzellib.events.block.BlockPlaceSnapshot;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityEventPayloads;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.interact.UseBlockPost;
import de.t14d3.rapunzellib.events.interact.UseBlockPre;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.ExplosionSourceKind;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RItemType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.transaction.BlockTransaction;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.explosive.fused.FusedExplosive;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.explosive.PrimeExplosiveEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class SpongeGameEventsBridge implements GameEventBridge {
    private final GameEventBus bus;

    SpongeGameEventsBridge(GameEventBus bus) {
        this.bus = Objects.requireNonNull(bus, "bus");
    }

    void register(Object owner) {
        Objects.requireNonNull(owner, "owner");

        PluginContainer plugin = (owner instanceof PluginContainer pc)
            ? pc
            : Sponge.pluginManager().fromInstance(owner).orElseThrow(() -> new IllegalArgumentException(
                "Sponge event bridge requires a plugin instance registered with Sponge (owner=" + owner.getClass().getName() + ")"
            ));

        Sponge.eventManager().registerListeners(plugin, this);
    }

    @Override
    public void close() {
        Sponge.eventManager().unregisterListeners(this);
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onChangeBlockAllPre(ChangeBlockEvent.All event, @First ServerPlayer player) {
        boolean needsBreakPre = bus.hasPreListeners(BlockBreakPre.class);
        boolean needsPlacePre = bus.hasPreListeners(BlockPlacePre.class);
        if (!needsBreakPre && !needsPlacePre) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(event.world());
        RWorldRef worldRef = rWorld.ref();

        if (needsBreakPre) {
            for (BlockTransaction tx : event.transactions(Operations.BREAK.get()).toList()) {
                RBlock block = Rapunzel.blocks().at(rWorld, toPos(tx.original().position()));
                BlockBreakPre pre = new BlockBreakPre(rPlayer, block, event.isCancelled());
                bus.dispatchPre(pre);
                if (pre.isDenied()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (needsPlacePre) {
            for (BlockTransaction tx : event.transactions(Operations.PLACE.get()).toList()) {
                BlockSnapshot placed = tx.finalReplacement();
                RBlockPos pos = toPos(placed.position());
                RBlock placeBlock = Rapunzel.blocks().at(rWorld, pos);

                BlockPlacePre pre = new BlockPlacePre(rPlayer, placeBlock, event.isCancelled());
                bus.dispatchPre(pre);
                if (pre.isDenied()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.TRUE)
    public void onChangeBlockAllCancelled(ChangeBlockEvent.All event, @First ServerPlayer player) {

        boolean needsBreakPost = bus.hasPostListeners(BlockBreakPost.class);
        boolean needsBreakAsync = bus.hasAsyncListeners(BlockBreakSnapshot.class);
        boolean needsPlacePost = bus.hasPostListeners(BlockPlacePost.class);
        boolean needsPlaceAsync = bus.hasAsyncListeners(BlockPlaceSnapshot.class);
        if (!needsBreakPost && !needsBreakAsync && !needsPlacePost && !needsPlaceAsync) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(event.world());
        RWorldRef worldRef = rWorld.ref();

        if (needsBreakPost || needsBreakAsync) {
            for (BlockTransaction tx : event.transactions(Operations.BREAK.get()).toList()) {
                RBlock block = Rapunzel.blocks().at(rWorld, toPos(tx.original().position()));
                if (needsBreakPost) bus.dispatchPost(new BlockBreakPost(rPlayer, block, true));
                if (needsBreakAsync) {
                    bus.dispatchAsync(BlockBreakSnapshot.capture(rPlayer.uuid(), block, true));
                }
            }
        }

        if (needsPlacePost || needsPlaceAsync) {
            for (BlockTransaction tx : event.transactions(Operations.PLACE.get()).toList()) {
                BlockSnapshot placed = tx.finalReplacement();
                RBlockPos pos = toPos(placed.position());
                RBlock placeBlock = Rapunzel.blocks().at(rWorld, pos);

                if (needsPlacePost) bus.dispatchPost(new BlockPlacePost(rPlayer, placeBlock, true));
                if (needsPlaceAsync) bus.dispatchAsync(new BlockPlaceSnapshot(rPlayer.uuid(), worldRef, pos, Rapunzel.blocks().requireData(placed.state()).typeKey(), true));
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onChangeBlockPost(ChangeBlockEvent.Post event, @First ServerPlayer player) {
        boolean needsBreakPost = bus.hasPostListeners(BlockBreakPost.class);
        boolean needsBreakAsync = bus.hasAsyncListeners(BlockBreakSnapshot.class);
        boolean needsPlacePost = bus.hasPostListeners(BlockPlacePost.class);
        boolean needsPlaceAsync = bus.hasAsyncListeners(BlockPlaceSnapshot.class);
        if (!needsBreakPost && !needsBreakAsync && !needsPlacePost && !needsPlaceAsync) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(event.world());

        for (BlockTransactionReceipt receipt : event.receipts()) {
            if (receipt.operation().equals(Operations.BREAK.get())) {
                if (!needsBreakPost && !needsBreakAsync) continue;
                RBlockPos pos = toPos(receipt.originalBlock().position());
                RBlock block = Rapunzel.blocks().at(rWorld, pos);
                if (needsBreakPost) bus.dispatchPost(new BlockBreakPost(rPlayer, block, false));
                if (needsBreakAsync) {
                    bus.dispatchAsync(BlockBreakSnapshot.capture(rPlayer.uuid(), block, false));
                }
            } else if (receipt.operation().equals(Operations.PLACE.get())) {
                if (!needsPlacePost && !needsPlaceAsync) continue;
                BlockSnapshot placed = receipt.finalBlock();
                RBlockPos pos = toPos(placed.position());
                RBlock block = Rapunzel.blocks().at(rWorld, pos);
                if (needsPlacePost) bus.dispatchPost(new BlockPlacePost(rPlayer, block, false));
                if (needsPlaceAsync) bus.dispatchAsync(BlockPlaceSnapshot.capture(rPlayer.uuid(), block, false));
            }
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onInteractBlockPrimary(InteractBlockEvent.Primary.Start event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(InteractBlockPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RBlock block = blockFromSnapshot(player.world(), event.block());

        InteractBlockPre pre = new InteractBlockPre(
            rPlayer,
            block,
            InteractBlockPre.Action.LEFT_CLICK_BLOCK,
            InteractBlockPre.Hand.MAIN_HAND
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onUseBlockPre(InteractBlockEvent.Secondary event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(UseBlockPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RBlock block = blockFromSnapshot(player.world(), event.block());

        UseBlockPre pre = new UseBlockPre(rPlayer, block, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onUseBlockPost(InteractBlockEvent.Secondary event, @First ServerPlayer player) {
        boolean needsPost = bus.hasPostListeners(UseBlockPost.class);
        boolean needsAsync = bus.hasAsyncListeners(UseBlockSnapshot.class);
        if (!needsPost && !needsAsync) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RBlock block = blockFromSnapshot(player.world(), event.block());
        boolean cancelled = event.isCancelled();

        if (needsPost) bus.dispatchPost(new UseBlockPost(rPlayer, block, cancelled));
        if (needsAsync) {
            bus.dispatchAsync(UseBlockSnapshot.capture(rPlayer.uuid(), block, cancelled));
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onInteractEntity(InteractEntityEvent.Secondary event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(InteractEntityPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        Entity entity = event.entity();

        InteractEntityPre pre = new InteractEntityPre(rPlayer, Rapunzel.entities().require(entity), event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onInteractEntityPost(InteractEntityEvent.Secondary event, @First ServerPlayer player) {
        if (!bus.hasPostListeners(InteractEntityPost.class)) return;

        bus.dispatchPost(EntityEventPayloads.interactPost(
            Rapunzel.players().require(player),
            Rapunzel.entities().require(event.entity()),
            event.isCancelled()
        ));
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onAttackEntity(AttackEntityEvent event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(AttackEntityPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        Entity entity = event.entity();

        AttackEntityPre pre = new AttackEntityPre(rPlayer, Rapunzel.entities().require(entity), event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onAttackEntityPost(AttackEntityEvent event, @First ServerPlayer player) {
        if (!bus.hasPostListeners(AttackEntityPost.class)) return;

        bus.dispatchPost(EntityEventPayloads.attackPost(
            Rapunzel.players().require(player),
            Rapunzel.entities().require(event.entity()),
            event.isCancelled()
        ));
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onDamageEntity(DamageEntityEvent event) {
        if (!bus.hasPreListeners(EntityHurtPre.class)) return;

        Entity entity = event.entity();
        String damageTypeKey = damageTypeKey(event);

        EntityHurtPre pre = new EntityHurtPre(Rapunzel.entities().require(entity), damageTypeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onDamageEntityPost(DamageEntityEvent event) {
        boolean needsPost = bus.hasPostListeners(EntityHurtPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntityHurtSnapshot.class);
        if (!needsPost && !needsAsync) return;

        String damageTypeKey = damageTypeKey(event);
        var entity = Rapunzel.entities().require(event.entity());
        boolean cancelled = event.isCancelled();

        if (needsPost) {
            bus.dispatchPost(EntityEventPayloads.hurtPost(entity, damageTypeKey, cancelled));
        }
        if (needsAsync) {
            bus.dispatchAsync(EntityEventPayloads.hurtSnapshot(entity, damageTypeKey, cancelled));
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onSpawnEntity(SpawnEntityEvent.Pre event) {
        if (!bus.hasPreListeners(EntitySpawnPre.class)) return;

        String reason = spawnReason(event);

        for (Entity entity : event.entities()) {
            ServerLocation loc = entity.serverLocation();
            RWorldRef worldRef = Rapunzel.worlds().require(loc.world()).ref();
            RLocation location = new RLocation(worldRef, loc.x(), loc.y(), loc.z());
            REntityType entityTypeKey = REntityType.require(RKey.of(entity.type().key(RegistryTypes.ENTITY_TYPE).asString()));

            EntitySpawnPre pre = new EntitySpawnPre(location, entityTypeKey, reason, event.isCancelled());
            bus.dispatchPre(pre);
            if (pre.isDenied()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.TRUE)
    public void onSpawnEntityCancelled(SpawnEntityEvent.Pre event) {
        if (!bus.hasAsyncListeners(EntitySpawnSnapshot.class)) return;

        String reason = spawnReason(event);
        for (Entity entity : event.entities()) {
            bus.dispatchAsync(EntityEventPayloads.spawnSnapshot(Rapunzel.entities().require(entity), reason, true));
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onSpawnEntityPost(SpawnEntityEvent event) {
        boolean needsPost = bus.hasPostListeners(EntitySpawnPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntitySpawnSnapshot.class);
        if (!needsPost && !needsAsync) return;

        String reason = spawnReason(event);
        boolean cancelled = event.isCancelled();
        for (Entity entity : event.entities()) {
            var rEntity = Rapunzel.entities().require(entity);
            if (needsPost && !cancelled) {
                bus.dispatchPost(EntityEventPayloads.spawnPost(rEntity, reason, false));
            }
            if (needsAsync) {
                bus.dispatchAsync(EntityEventPayloads.spawnSnapshot(rEntity, reason, cancelled));
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onWorldLoad(LoadWorldEvent event) {
        if (!bus.hasPostListeners(WorldLoadPost.class)) return;
        bus.dispatchPost(new WorldLoadPost(Rapunzel.worlds().require(event.world()).ref()));
    }

    @Listener(order = Order.LAST)
    public void onChunkUnload(ChunkEvent.Unload.Post event) {
        if (!bus.hasPostListeners(ChunkUnloadPost.class)) return;
        Vector3i chunkPos = event.chunkPosition();
        bus.dispatchPost(new ChunkUnloadPost(worldRefFromKey(event.worldKey()), chunkPos.x(), chunkPos.z()));
    }

    @Listener(order = Order.LAST)
    public void onPlayerDisconnect(ServerSideConnectionEvent.Disconnect event) {
        if (!bus.hasPostListeners(PlayerQuitPost.class)) return;
        Optional<org.spongepowered.api.profile.GameProfile> profile = event.profile();
        if (profile.isEmpty()) return;
        UUID uuid = profile.get().uuid();
        String name = profile.get().name().orElseGet(uuid::toString);
        bus.dispatchPost(new PlayerQuitPost(uuid, name));
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!bus.hasPreListeners(ExplosionPre.class)) return;

        ServerWorld world = event.world();
        RWorldRef worldRef = Rapunzel.worlds().require(world).ref();

        ServerLocation serverLocation = event.explosion().serverLocation();
        RLocation location = new RLocation(worldRef, serverLocation.x(), serverLocation.y(), serverLocation.z());

        var sourceOpt = event.explosion().sourceExplosive();
        RKey sourceTypeKey = sourceOpt
            .map(src -> {
                var key = src.type().key(RegistryTypes.ENTITY_TYPE);
                return RKey.of(key.namespace(), key.value());
            })
            .orElse(null);
        ExplosionSourceKind sourceKind = sourceOpt.isPresent() ? ExplosionSourceKind.ENTITY : ExplosionSourceKind.OTHER;

        List<RBlockPos> affected = new ArrayList<>();
        for (ServerLocation loc : event.affectedLocations()) {
            Vector3i p = loc.blockPosition();
            affected.add(new RBlockPos(p.x(), p.y(), p.z()));
        }

        ExplosionPre pre = new ExplosionPre(location, sourceTypeKey, sourceKind, affected, event.isCancelled());
        bus.dispatchPre(pre);

        if (pre.isDenied()) {
            event.setCancelled(true);
            return;
        }

        Set<RBlockPos> keep = new HashSet<>(pre.affectedBlocks());
        event.filterAffectedLocations(loc -> keep.contains(new RBlockPos(loc.blockX(), loc.blockY(), loc.blockZ())));
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onPrimeExplosive(PrimeExplosiveEvent.Pre event) {
        if (!bus.hasPreListeners(TntPrimePre.class)) return;

        FusedExplosive explosive = event.fusedExplosive();
        ServerLocation loc = explosive.serverLocation();
        ServerWorld world = loc.world();
        Vector3i pos = explosive.blockPosition();

        RWorld rWorld = Rapunzel.worlds().require(world);
        RBlock block = Rapunzel.blocks().at(rWorld, toPos(pos));

        String cause = event.context()
            .get(EventContextKeys.SPAWN_TYPE)
            .map(st -> st.key(RegistryTypes.SPAWN_TYPE).asString())
            .orElse(event.cause().root().getClass().getSimpleName());

        RPlayer rPlayer = event.cause().first(ServerPlayer.class).map(p -> Rapunzel.players().require(p)).orElse(null);
        TntPrimePre pre = new TntPrimePre(block, cause, rPlayer, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    public void onEntityTeleport(ChangeEntityWorldEvent.Reposition event) {
        if (!bus.hasPostListeners(EntityTeleportPost.class)) return;

        Entity entity = event.entity();
        var rEntity = Rapunzel.entities().require(entity);

        // Reposition inherits MoveEntityEvent, so positions are Vector3d
        Vector3d fromPos = event.originalPosition();
        Vector3d toPos = event.destinationPosition();

        RWorldRef worldRef = Rapunzel.worlds().require(event.destinationWorld()).ref();
        RLocation from = new RLocation(worldRef, fromPos.x(), fromPos.y(), fromPos.z());
        RLocation to = new RLocation(worldRef, toPos.x(), toPos.y(), toPos.z());

        bus.dispatchPost(new EntityTeleportPost(rEntity, from, to));
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onPlayerMovePre(MoveEntityEvent event) {
        if (!bus.hasPreListeners(PlayerMovePre.class)) return;
        if (!(event.entity() instanceof ServerPlayer player)) return;

        Vector3d fromPos = event.originalPosition();
        Vector3d toPos = event.destinationPosition();

        // Ignore rotation-only and tiny movements to reduce event spam
        if (Math.abs(fromPos.x() - toPos.x()) < 1.0
                && Math.abs(fromPos.y() - toPos.y()) < 1.0
                && Math.abs(fromPos.z() - toPos.z()) < 1.0) {
            return;
        }

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorldRef worldRef = Rapunzel.worlds().require(player.world()).ref();
        RLocation from = new RLocation(worldRef, fromPos.x(), fromPos.y(), fromPos.z());
        RLocation to = new RLocation(worldRef, toPos.x(), toPos.y(), toPos.z());

        PlayerMovePre pre = new PlayerMovePre(rPlayer, from, to, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.TRUE)
    public void onPlayerMovePost(MoveEntityEvent event) {
        if (!bus.hasPostListeners(PlayerMovePost.class)) return;
        if (!(event.entity() instanceof ServerPlayer player)) return;

        Vector3d fromPos = event.originalPosition();
        Vector3d toPos = event.destinationPosition();

        // Ignore tiny movements to reduce event spam
        if (Math.abs(fromPos.x() - toPos.x()) < 1.0
                && Math.abs(fromPos.y() - toPos.y()) < 1.0
                && Math.abs(fromPos.z() - toPos.z()) < 1.0) {
            return;
        }

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorldRef worldRef = Rapunzel.worlds().require(player.world()).ref();
        RLocation from = new RLocation(worldRef, fromPos.x(), fromPos.y(), fromPos.z());
        RLocation to = new RLocation(worldRef, toPos.x(), toPos.y(), toPos.z());

        bus.dispatchPost(new PlayerMovePost(rPlayer, from, to, event.isCancelled()));
    }

    @Listener(order = Order.LAST)
    @IsCancelled(value = Tristate.TRUE)
    public void onEntityMove(MoveEntityEvent event) {
        if (!bus.hasPostListeners(EntityMovePost.class)) return;
        // Player movement is published via onPlayerMovePost; skip here to avoid duplicates
        if (event.entity() instanceof ServerPlayer) return;

        Entity entity = event.entity();
        var rEntity = Rapunzel.entities().require(entity);

        Vector3d fromPos = event.originalPosition();
        Vector3d toPos = event.destinationPosition();

        // Only dispatch for significant movements (more than 0.5 block) to reduce spam
        if (Math.abs(fromPos.x() - toPos.x()) < 0.5
                && Math.abs(fromPos.y() - toPos.y()) < 0.5
                && Math.abs(fromPos.z() - toPos.z()) < 0.5) {
            return;
        }

        RWorldRef worldRef = Rapunzel.worlds().require(entity.world()).ref();
        RLocation from = new RLocation(worldRef, fromPos.x(), fromPos.y(), fromPos.z());
        RLocation to = new RLocation(worldRef, toPos.x(), toPos.y(), toPos.z());

        bus.dispatchPost(new EntityMovePost(rEntity, from, to));
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onInventoryOpenPre(InteractContainerEvent.Open event) {
        if (!(event.cause().first(ServerPlayer.class).orElse(null) instanceof ServerPlayer player)) return;
        if (!bus.hasPreListeners(InventoryOpenPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().wrap(event.container()).orElse(null);
        if (rInventory == null) return;

        InventoryOpenPre pre = new InventoryOpenPre(rPlayer, rInventory);
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    public void onInventoryOpenPost(InteractContainerEvent.Open event) {
        if (!(event.cause().first(ServerPlayer.class).orElse(null) instanceof ServerPlayer player)) return;
        if (!bus.hasPostListeners(InventoryOpenPost.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().wrap(event.container()).orElse(null);
        if (rInventory == null) return;

        bus.dispatchPost(new InventoryOpenPost(rPlayer, rInventory));
    }

    @Listener(order = Order.LAST)
    public void onInventoryClosePost(InteractContainerEvent.Close event) {
        if (!(event.cause().first(ServerPlayer.class).orElse(null) instanceof ServerPlayer player)) return;
        if (!bus.hasPostListeners(InventoryClosePost.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().wrap(event.container()).orElse(null);
        if (rInventory == null) return;

        bus.dispatchPost(new InventoryClosePost(rPlayer, rInventory));
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onInventoryClickPre(ClickContainerEvent event) {
        if (!(event.cause().first(ServerPlayer.class).orElse(null) instanceof ServerPlayer player)) return;
        if (!bus.hasPreListeners(InventoryClickPre.class)) return;

        Optional<Slot> slotOpt = event.slot();
        if (slotOpt.isEmpty()) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().wrap(event.container()).orElse(null);
        if (rInventory == null) return;

        int slot = slotIndex(event.container(), slotOpt.get());

        InventoryClickPre pre = new InventoryClickPre(rPlayer, rInventory, slot, InventoryClickType.LEFT, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    public void onInventoryClickPost(ClickContainerEvent event) {
        if (!(event.cause().first(ServerPlayer.class).orElse(null) instanceof ServerPlayer player)) return;
        if (!bus.hasPostListeners(InventoryClickPost.class)) return;

        Optional<Slot> slotOpt = event.slot();
        if (slotOpt.isEmpty()) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RInventory rInventory = InventoryFeatures.install().wrap(event.container()).orElse(null);
        if (rInventory == null) return;

        int slot = slotIndex(event.container(), slotOpt.get());

        bus.dispatchPost(new InventoryClickPost(rPlayer, rInventory, slot, InventoryClickType.LEFT, event.isCancelled()));
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onBucketFillPre(InteractBlockEvent.Secondary event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(BucketFillPre.class)) return;

        ItemType usedItemType = usedItemType(event);
        if (usedItemType == null || !isBucketItem(usedItemType)) return;

        // An empty bucket is "filling" only when the targeted block is a fluid source.
        if (!isEmptyBucket(usedItemType)) return;

        BlockSnapshot blockSnapshot = event.block();
        if (blockSnapshot.state().fluidState().isEmpty()) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RBlock block = blockFromSnapshot(player.world(), blockSnapshot);

        BucketFillPre pre = new BucketFillPre(rPlayer, block, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onBucketEmptyPre(InteractBlockEvent.Secondary event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(BucketEmptyPre.class)) return;

        ItemType usedItemType = usedItemType(event);
        if (usedItemType == null || !isBucketItem(usedItemType)) return;

        // A filled bucket is "emptying" when placing its fluid into the world.
        if (isEmptyBucket(usedItemType)) return;

        BlockSnapshot blockSnapshot = event.block();
        Vector3i placePos = blockSnapshot.position();

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(player.world());
        RWorldRef worldRef = rWorld.ref();
        RLocation location = new RLocation(worldRef, placePos.x(), placePos.y(), placePos.z());
        RKey itemTypeKey = RKey.of(usedItemType.key(RegistryTypes.ITEM_TYPE).toString());
        RItemType rapunzelItemType = RItemType.require(itemTypeKey);

        BucketEmptyPre pre = new BucketEmptyPre(rPlayer, location, rapunzelItemType, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onBucketEntityPre(InteractEntityEvent.Secondary event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(BucketEntityPre.class)) return;

        ItemType usedItemType = usedItemType(event);
        if (usedItemType == null || !isBucketItem(usedItemType)) return;
        // Only water buckets can scoop entities (axolotl, fish, etc.)
        if (!isEmptyBucket(usedItemType)) return;

        Entity entity = event.entity();
        // Only attempt for water-type entities that may be bucket-capturable.
        // This is a heuristic; Sponge doesn't grant direct access to vanilla bucketable flags.
        String entityKey = entity.type().key(RegistryTypes.ENTITY_TYPE).asString();
        if (!isBucketableEntity(entityKey)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        ServerLocation loc = entity.serverLocation();
        RWorldRef worldRef = Rapunzel.worlds().require(loc.world()).ref();
        RLocation location = new RLocation(worldRef, loc.x(), loc.y(), loc.z());
        var rEntity = Rapunzel.entities().require(entity);

        BucketEntityPre pre = new BucketEntityPre(rPlayer, location, rEntity, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    private static RBlock blockFromSnapshot(ServerWorld fallbackWorld, BlockSnapshot snapshot) {
        return Rapunzel.blocks().wrap(snapshot)
            .orElseGet(() -> {
                ServerWorld world = snapshot.location().map(ServerLocation::world).orElse(fallbackWorld);
                RWorld rWorld = Rapunzel.worlds().require(world);
                return Rapunzel.blocks().at(rWorld, toPos(snapshot.position()));
            });
    }

    private static RBlockPos toPos(Vector3i pos) {
        return new RBlockPos(pos.x(), pos.y(), pos.z());
    }

    private static int slotIndex(org.spongepowered.api.item.inventory.Container container, Slot slot) {
        // Best-effort: scan the container's slots for the matching slot identity.
        // This is O(n) per click but container sizes are bounded (<= 54 typically).
        java.util.List<Slot> slots = container.slots();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).equals(slot)) return i;
        }
        return -1;
    }

    private static ItemType usedItemType(org.spongepowered.api.event.Event event) {
        Optional<ItemStackSnapshot> snapshot = event.context().get(EventContextKeys.USED_ITEM);
        if (snapshot.isPresent()) {
            ItemStack stack = snapshot.get().createStack();
            return stack.type();
        }
        return null;
    }

    private static boolean isBucketItem(ItemType itemType) {
        String key = itemType.key(RegistryTypes.ITEM_TYPE).asString();
        return key.endsWith("_bucket");
    }

    private static boolean isEmptyBucket(ItemType itemType) {
        String key = itemType.key(RegistryTypes.ITEM_TYPE).asString();
        return "minecraft:bucket".equals(key);
    }

    private static boolean isBucketableEntity(String entityKey) {
        // Heuristic: only water-bucket-capturable entities are listed here.
        // Sponge lacks a public "bucketable" flag, so we keep this conservative.
        return "minecraft:axolotl".equals(entityKey)
            || "minecraft:tropical_fish".equals(entityKey)
            || "minecraft:cod".equals(entityKey)
            || "minecraft:salmon".equals(entityKey)
            || "minecraft:pufferfish".equals(entityKey)
            || "minecraft:tadpole".equals(entityKey);
    }

    private static String damageTypeKey(DamageEntityEvent event) {
        return event.context()
            .get(EventContextKeys.DAMAGE_TYPE)
            .map(dt -> dt.key(RegistryTypes.DAMAGE_TYPE).asString())
            .orElse("unknown");
    }

    private static String spawnReason(SpawnEntityEvent event) {
        return event.context()
            .get(EventContextKeys.SPAWN_TYPE)
            .map(st -> st.key(RegistryTypes.SPAWN_TYPE).asString())
            .orElse("unknown");
    }

    private static RWorldRef worldRefFromKey(ResourceKey worldKey) {
        if (Sponge.isServerAvailable()) {
            return Sponge.server().worldManager().world(worldKey)
                .map(w -> Rapunzel.worlds().require(w).ref())
                .orElseGet(() -> new RWorldRef(worldKey.asString(), worldKey.asString()));
        }
        return new RWorldRef(worldKey.asString(), worldKey.asString());
    }
}
