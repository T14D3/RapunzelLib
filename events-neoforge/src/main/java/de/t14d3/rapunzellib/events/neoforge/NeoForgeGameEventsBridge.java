package de.t14d3.rapunzellib.events.neoforge;

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
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.UUID;

final class NeoForgeGameEventsBridge implements GameEventBridge {
    private final GameEventBus bus;

    NeoForgeGameEventsBridge(GameEventBus bus) {
        this.bus = bus;
    }

    void register() {
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public void close() {
        NeoForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!bus.hasPostListeners(WorldLoadPost.class)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        String id = level.dimension().location().toString();
        bus.dispatchPost(new WorldLoadPost(new RWorldRef(id, id)));
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!bus.hasPostListeners(ChunkUnloadPost.class)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var chunk = event.getChunk();
        String id = level.dimension().location().toString();
        bus.dispatchPost(new ChunkUnloadPost(new RWorldRef(id, id), chunk.getPos().x, chunk.getPos().z));
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!bus.hasPostListeners(PlayerQuitPost.class)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        bus.dispatchPost(new PlayerQuitPost(player.getUUID(), player.getName().getString()));
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
        String typeKey = typeKey(event.getState());
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
        String worldId = level.dimension().location().toString();
        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        String placedKey = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).toString();
        boolean cancelled = event.isCanceled();

        if (needsPre) {
            BlockPlacePre pre = new BlockPlacePre(rPlayer, worldRef, rPos, placedKey, cancelled);
            bus.dispatchPre(pre);
            cancelled = pre.isDenied();
            if (cancelled) event.setCanceled(true);
        }

        if (cancelled) {
            if (needsPost) bus.dispatchPost(new BlockPlacePost(rPlayer, worldRef, rPos, placedKey, true));
            if (needsAsync) bus.dispatchAsync(new BlockPlaceSnapshot(rPlayer.uuid(), worldRef, rPos, placedKey, true));
            return;
        }

        if (!needsPost && !needsAsync) return;
        UUID uuid = rPlayer.uuid();
        level.getServer().execute(() -> {
            if (needsPost) bus.dispatchPost(new BlockPlacePost(rPlayer, worldRef, rPos, placedKey, false));
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
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        var target = event.getTarget();

        RPlayer rPlayer = Rapunzel.players().require(player);
        String worldId = level.dimension().location().toString();
        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        BlockPos pos = target.blockPosition();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();

        InteractEntityPre pre = new InteractEntityPre(rPlayer, worldRef, rPos, typeKey, event.isCanceled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!bus.hasPreListeners(AttackEntityPre.class)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var target = event.getTarget();

        RPlayer rPlayer = Rapunzel.players().require(player);
        String worldId = player.level().dimension().location().toString();
        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        BlockPos pos = target.blockPosition();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();

        AttackEntityPre pre = new AttackEntityPre(rPlayer, worldRef, rPos, typeKey, event.isCanceled());
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setCanceled(true);
        }
    }

    private static InteractBlockPre.Hand mapHand(InteractionHand hand) {
        if (hand == null) return InteractBlockPre.Hand.UNKNOWN;
        return switch (hand) {
            case MAIN_HAND -> InteractBlockPre.Hand.MAIN_HAND;
            case OFF_HAND -> InteractBlockPre.Hand.OFF_HAND;
        };
    }

    private static String typeKey(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null) return "minecraft:air";
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
}
