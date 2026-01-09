package de.t14d3.rapunzellib.events.sponge;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.block.BlockBreakPost;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;
import de.t14d3.rapunzellib.events.block.BlockBreakSnapshot;
import de.t14d3.rapunzellib.events.block.BlockPlacePost;
import de.t14d3.rapunzellib.events.block.BlockPlacePre;
import de.t14d3.rapunzellib.events.block.BlockPlaceSnapshot;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.interact.UseBlockPost;
import de.t14d3.rapunzellib.events.interact.UseBlockPre;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
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
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.explosive.PrimeExplosiveEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
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
                String typeKey = Rapunzel.blocks().requireData(placed.state()).typeKey();

                BlockPlacePre pre = new BlockPlacePre(rPlayer, worldRef, pos, typeKey, event.isCancelled());
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
                    bus.dispatchAsync(new BlockBreakSnapshot(rPlayer.uuid(), block.world().ref(), block.pos(), block.typeKey(), true));
                }
            }
        }

        if (needsPlacePost || needsPlaceAsync) {
            for (BlockTransaction tx : event.transactions(Operations.PLACE.get()).toList()) {
                BlockSnapshot placed = tx.finalReplacement();
                RBlockPos pos = toPos(placed.position());
                String typeKey = Rapunzel.blocks().requireData(placed.state()).typeKey();

                if (needsPlacePost) bus.dispatchPost(new BlockPlacePost(rPlayer, worldRef, pos, typeKey, true));
                if (needsPlaceAsync) bus.dispatchAsync(new BlockPlaceSnapshot(rPlayer.uuid(), worldRef, pos, typeKey, true));
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
        RWorldRef worldRef = rWorld.ref();

        for (BlockTransactionReceipt receipt : event.receipts()) {
            if (receipt.operation().equals(Operations.BREAK.get())) {
                if (!needsBreakPost && !needsBreakAsync) continue;
                RBlockPos pos = toPos(receipt.originalBlock().position());
                RBlock block = Rapunzel.blocks().at(rWorld, pos);
                if (needsBreakPost) bus.dispatchPost(new BlockBreakPost(rPlayer, block, false));
                if (needsBreakAsync) {
                    bus.dispatchAsync(new BlockBreakSnapshot(rPlayer.uuid(), block.world().ref(), block.pos(), block.typeKey(), false));
                }
            } else if (receipt.operation().equals(Operations.PLACE.get())) {
                if (!needsPlacePost && !needsPlaceAsync) continue;
                BlockSnapshot placed = receipt.finalBlock();
                RBlockPos pos = toPos(placed.position());
                String typeKey = Rapunzel.blocks().requireData(placed.state()).typeKey();
                if (needsPlacePost) bus.dispatchPost(new BlockPlacePost(rPlayer, worldRef, pos, typeKey, false));
                if (needsPlaceAsync) bus.dispatchAsync(new BlockPlaceSnapshot(rPlayer.uuid(), worldRef, pos, typeKey, false));
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
            bus.dispatchAsync(new UseBlockSnapshot(rPlayer.uuid(), block.world().ref(), block.pos(), block.typeKey(), cancelled));
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onInteractEntity(InteractEntityEvent.Secondary event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(InteractEntityPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        Entity entity = event.entity();

        ServerLocation loc = entity.serverLocation();
        RWorldRef worldRef = Rapunzel.worlds().require(loc.world()).ref();
        RBlockPos pos = toPos(entity.blockPosition());
        String typeKey = entity.type().key(RegistryTypes.ENTITY_TYPE).asString();

        InteractEntityPre pre = new InteractEntityPre(rPlayer, worldRef, pos, typeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onAttackEntity(AttackEntityEvent event, @First ServerPlayer player) {
        if (!bus.hasPreListeners(AttackEntityPre.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        Entity entity = event.entity();

        ServerLocation loc = entity.serverLocation();
        RWorldRef worldRef = Rapunzel.worlds().require(loc.world()).ref();
        RBlockPos pos = toPos(entity.blockPosition());
        String typeKey = entity.type().key(RegistryTypes.ENTITY_TYPE).asString();

        AttackEntityPre pre = new AttackEntityPre(rPlayer, worldRef, pos, typeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onDamageEntity(DamageEntityEvent event) {
        if (!bus.hasPreListeners(EntityHurtPre.class)) return;

        Entity entity = event.entity();
        ServerLocation loc = entity.serverLocation();
        RWorldRef worldRef = Rapunzel.worlds().require(loc.world()).ref();
        RBlockPos pos = toPos(entity.blockPosition());
        String entityTypeKey = entity.type().key(RegistryTypes.ENTITY_TYPE).asString();
        String damageTypeKey = event.context()
            .get(EventContextKeys.DAMAGE_TYPE)
            .map(dt -> dt.key(RegistryTypes.DAMAGE_TYPE).asString())
            .orElse("unknown");

        EntityHurtPre pre = new EntityHurtPre(worldRef, pos, entityTypeKey, damageTypeKey, event.isCancelled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(value = Tristate.UNDEFINED)
    public void onSpawnEntity(SpawnEntityEvent.Pre event) {
        if (!bus.hasPreListeners(EntitySpawnPre.class)) return;

        String reason = event.context()
            .get(EventContextKeys.SPAWN_TYPE)
            .map(st -> st.key(RegistryTypes.SPAWN_TYPE).asString())
            .orElse("unknown");

        for (Entity entity : event.entities()) {
            ServerLocation loc = entity.serverLocation();
            RWorldRef worldRef = Rapunzel.worlds().require(loc.world()).ref();
            RBlockPos pos = toPos(entity.blockPosition());
            String entityTypeKey = entity.type().key(RegistryTypes.ENTITY_TYPE).asString();

            EntitySpawnPre pre = new EntitySpawnPre(worldRef, pos, entityTypeKey, reason, event.isCancelled());
            bus.dispatchPre(pre);
            if (pre.isDenied()) {
                event.setCancelled(true);
                return;
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
        Vector3i origin = event.explosion().location().blockPosition();

        String sourceTypeKey = event.explosion()
            .sourceExplosive()
            .map(src -> src.type().key(RegistryTypes.ENTITY_TYPE).asString())
            .orElse("unknown");

        List<RBlockPos> affected = new ArrayList<>();
        for (ServerLocation loc : event.affectedLocations()) {
            Vector3i p = loc.blockPosition();
            affected.add(new RBlockPos(p.x(), p.y(), p.z()));
        }

        ExplosionPre pre = new ExplosionPre(worldRef, new RBlockPos(origin.x(), origin.y(), origin.z()), sourceTypeKey, affected, event.isCancelled());
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
        RWorldRef worldRef = Rapunzel.worlds().require(world).ref();
        Vector3i pos = explosive.blockPosition();

        String typeKey = explosive.type().key(RegistryTypes.ENTITY_TYPE).asString();
        String cause = event.context()
            .get(EventContextKeys.SPAWN_TYPE)
            .map(st -> st.key(RegistryTypes.SPAWN_TYPE).asString())
            .orElse(event.cause().root().getClass().getSimpleName());

        RPlayer rPlayer = event.cause().first(ServerPlayer.class).map(p -> Rapunzel.players().require(p)).orElse(null);
        TntPrimePre pre = new TntPrimePre(worldRef, toPos(pos), typeKey, cause, rPlayer, event.isCancelled());
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

    private static RWorldRef worldRefFromKey(ResourceKey worldKey) {
        if (Sponge.isServerAvailable()) {
            return Sponge.server().worldManager().world(worldKey)
                .map(w -> Rapunzel.worlds().require(w).ref())
                .orElseGet(() -> new RWorldRef(worldKey.asString(), worldKey.asString()));
        }
        return new RWorldRef(worldKey.asString(), worldKey.asString());
    }
}
