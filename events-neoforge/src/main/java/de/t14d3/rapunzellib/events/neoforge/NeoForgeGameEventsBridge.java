package de.t14d3.rapunzellib.events.neoforge;

import de.t14d3.rapunzellib.objects.RKey;
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
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.shared.SharedEntityDamageHooks;
import de.t14d3.rapunzellib.events.shared.SharedEntityInteractionHooks;
import de.t14d3.rapunzellib.events.shared.SharedEntitySpawnHooks;
import de.t14d3.rapunzellib.events.shared.SharedLifecycleEventHooks;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

final class NeoForgeGameEventsBridge implements GameEventBridge {
    private final GameEventBus bus;

    static @NotNull GameEventBridge install(@NotNull GameEventBus bus) {
        NeoForgeGameEventsBridge bridge = new NeoForgeGameEventsBridge(bus);
        bridge.register();
        return bridge;
    }

    NeoForgeGameEventsBridge(GameEventBus bus) {
        this.bus = bus;
    }

    void register() {
        SharedLifecycleEventHooks.initializeMixins(bus);
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public void close() {
        NeoForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        SharedLifecycleEventHooks.dispatchWorldLoadPost(bus, level);
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var chunk = event.getChunk();
        SharedLifecycleEventHooks.dispatchChunkUnloadPost(bus, level, chunk.getPos().x, chunk.getPos().z);
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SharedLifecycleEventHooks.dispatchPlayerQuitPost(bus, player);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        boolean needsPre = bus.hasPreListeners(BlockBreakPre.class);
        boolean needsPost = bus.hasPostListeners(BlockBreakPost.class);
        boolean needsAsync = bus.hasAsyncListeners(BlockBreakSnapshot.class);
        if (!needsPre && !needsPost && !needsAsync) return;

        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(level);
        BlockPos pos = event.getPos();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        boolean cancelled = event.isCanceled();

        if (needsPre) {
            RBlock block = Rapunzel.blocks().at(rWorld, rPos);
            BlockBreakPre pre = new BlockBreakPre(rPlayer, block, cancelled);
            bus.dispatchPre(pre);
            cancelled = pre.isDenied();
            if (cancelled) event.setCanceled(true);
        }

        if (cancelled) {
            if (needsPost) bus.dispatchPost(new BlockBreakPost(rPlayer, Rapunzel.blocks().at(rWorld, rPos), true));
            if (needsAsync) bus.dispatchAsync(new BlockBreakSnapshot(rPlayer.uuid(), rWorld.ref(), rPos, typeKey(event.getState()), true));
            return;
        }

        if (!needsPost && !needsAsync) return;
        UUID uuid = rPlayer.uuid();
        RKey typeKey = typeKey(event.getState());
        level.getServer().execute(() -> {
            if (needsPost) bus.dispatchPost(new BlockBreakPost(rPlayer, Rapunzel.blocks().at(rWorld, rPos), false));
            if (needsAsync) bus.dispatchAsync(new BlockBreakSnapshot(uuid, rWorld.ref(), rPos, typeKey, false));
        });
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        boolean needsPre = bus.hasPreListeners(BlockPlacePre.class);
        boolean needsPost = bus.hasPostListeners(BlockPlacePost.class);
        boolean needsAsync = bus.hasAsyncListeners(BlockPlaceSnapshot.class);
        if (!needsPre && !needsPost && !needsAsync) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RPlayer rPlayer = Rapunzel.players().require(player);
        // #if VERSION >= 1.21.11
        RKey worldKey = RKey.of(level.dimension().identifier().toString());
        // #else
        RKey worldKey = RKey.of(level.dimension().location().toString());
        // #endif
        RWorldRef worldRef = new RWorldRef(null, worldKey);
        RKey placedKey = RKey.of(BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).toString());
        RWorld rWorld = Rapunzel.worlds().require(level);
        RBlock placeBlock = Rapunzel.blocks().at(rWorld, rPos);
        boolean cancelled = event.isCanceled();

        if (needsPre) {
            BlockPlacePre pre = new BlockPlacePre(rPlayer, placeBlock, cancelled);
            bus.dispatchPre(pre);
            cancelled = pre.isDenied();
            if (cancelled) event.setCanceled(true);
        }

        if (cancelled) {
            if (needsPost) bus.dispatchPost(new BlockPlacePost(rPlayer, placeBlock, true));
            if (needsAsync) bus.dispatchAsync(new BlockPlaceSnapshot(rPlayer.uuid(), worldRef, rPos, placedKey, true));
            return;
        }

        if (!needsPost && !needsAsync) return;
        UUID uuid = rPlayer.uuid();
        level.getServer().execute(() -> {
            if (needsPost) bus.dispatchPost(new BlockPlacePost(rPlayer, placeBlock, false));
            if (needsAsync) bus.dispatchAsync(new BlockPlaceSnapshot(uuid, worldRef, rPos, placedKey, false));
        });
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!bus.hasPreListeners(InteractBlockPre.class)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(level);
        BlockPos pos = event.getPos();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RBlock block = Rapunzel.blocks().at(rWorld, rPos);

        InteractBlockPre.Hand mappedHand = mapHand(event.getHand());
        InteractBlockPre pre = new InteractBlockPre(
            rPlayer,
            block,
            InteractBlockPre.Action.LEFT_CLICK_BLOCK,
            mappedHand,
            event.isCanceled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!bus.hasPreListeners(InteractBlockPre.class)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(level);
        BlockPos pos = event.getPos();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RBlock block = Rapunzel.blocks().at(rWorld, rPos);

        InteractBlockPre.Hand mappedHand = mapHand(event.getHand());
        InteractBlockPre pre = new InteractBlockPre(
            rPlayer,
            block,
            InteractBlockPre.Action.RIGHT_CLICK_BLOCK,
            mappedHand,
            event.isCanceled()
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!bus.hasPreListeners(InteractEntityPre.class)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;

        if (SharedEntityInteractionHooks.dispatchInteractPre(bus, player, event.getTarget(), event.isCanceled())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!bus.hasPreListeners(AttackEntityPre.class)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (SharedEntityInteractionHooks.dispatchAttackPre(bus, player, event.getTarget(), event.isCanceled())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onInteractEntityPost(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;

        SharedEntityInteractionHooks.dispatchInteractPost(bus, player, event.getTarget(), event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onAttackEntityPost(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SharedEntityInteractionHooks.dispatchAttackPost(bus, player, event.getTarget(), event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onEntityJoinLevelPre(EntityJoinLevelEvent event) {
        if (!bus.hasPreListeners(EntitySpawnPre.class)) return;
        if (event.loadedFromDisk()) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;

        if (SharedEntitySpawnHooks.dispatchSpawnPre(bus, event.getEntity(), spawnReason(event), event.isCanceled())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        boolean needsPost = bus.hasPostListeners(EntitySpawnPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntitySpawnSnapshot.class);
        if (!needsPost && !needsAsync) return;
        if (event.loadedFromDisk()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        String reason = spawnReason(event);
        Entity entity = event.getEntity();

        if (event.isCanceled()) {
            SharedEntitySpawnHooks.dispatchCancelledSpawnSnapshot(bus, entity, reason);
            return;
        }

        level.getServer().execute(() -> SharedEntitySpawnHooks.dispatchSpawnOutcome(bus, entity, reason, false));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onEntityHurtCancelled(LivingIncomingDamageEvent event) {
        if (!event.isCanceled()) return;

        SharedEntityDamageHooks.dispatchHurtOutcome(bus, event.getEntity(), damageTypeKey(event.getSource()), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityHurtPost(LivingDamageEvent.Post event) {
        SharedEntityDamageHooks.dispatchHurtOutcome(bus, event.getEntity(), damageTypeKey(event.getSource()), false);
    }

    private static InteractBlockPre.Hand mapHand(InteractionHand hand) {
        if (hand == null) return InteractBlockPre.Hand.UNKNOWN;
        return switch (hand) {
            case MAIN_HAND -> InteractBlockPre.Hand.MAIN_HAND;
            case OFF_HAND -> InteractBlockPre.Hand.OFF_HAND;
        };
    }

    private static RKey typeKey(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null) return RKey.of("minecraft:air");
        return RKey.of(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static String spawnReason(EntityJoinLevelEvent event) {
        return "unknown";
    }

    private static String damageTypeKey(DamageSource source) {
        return source.type().msgId();
    }
}
