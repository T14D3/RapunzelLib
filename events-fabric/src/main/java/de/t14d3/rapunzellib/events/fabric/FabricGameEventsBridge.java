package de.t14d3.rapunzellib.events.fabric;

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
import de.t14d3.rapunzellib.events.interact.UseBlockPost;
import de.t14d3.rapunzellib.events.interact.UseBlockPre;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

final class FabricGameEventsBridge implements GameEventBridge {
    private final GameEventBus bus;

    FabricGameEventsBridge(GameEventBus bus) {
        this.bus = bus;
    }

    void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            if (!(world instanceof ServerLevel serverLevel)) return true;
            return onBlockBreakPre(serverLevel, serverPlayer, pos);
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!(world instanceof ServerLevel serverLevel)) return;
            onBlockBreakPost(serverLevel, serverPlayer, pos);
        });
        UseBlockCallback.EVENT.register(this::onUseBlock);
        AttackBlockCallback.EVENT.register(this::onAttackBlock);
        UseEntityCallback.EVENT.register(this::onUseEntity);
        AttackEntityCallback.EVENT.register(this::onAttackEntity);

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!bus.hasPostListeners(WorldLoadPost.class)) return;
            String id = world.dimension().location().toString();
            bus.dispatchPost(new WorldLoadPost(new RWorldRef(id, id)));
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (!bus.hasPostListeners(ChunkUnloadPost.class)) return;
            String id = world.dimension().location().toString();
            bus.dispatchPost(new ChunkUnloadPost(new RWorldRef(id, id), chunk.getPos().x, chunk.getPos().z));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!bus.hasPostListeners(PlayerQuitPost.class)) return;
            ServerPlayer player = handler.getPlayer();
            if (player == null) return;
            bus.dispatchPost(new PlayerQuitPost(player.getUUID(), player.getName().getString()));
        });
    }

    @Override
    public void close() {
        // Fabric callbacks do not support unregistration in a straightforward way; treat as process-lifetime hooks.
    }

    private boolean onBlockBreakPre(ServerLevel world, ServerPlayer player, BlockPos pos) {
        boolean needsPre = bus.hasPreListeners(BlockBreakPre.class);
        boolean needsPost = bus.hasPostListeners(BlockBreakPost.class);
        boolean needsAsync = bus.hasAsyncListeners(BlockBreakSnapshot.class);
        if (!needsPre && !needsPost && !needsAsync) return true;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(world);
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RBlock block = Rapunzel.blocks().at(rWorld, rPos);

        boolean cancelled;
        if (needsPre) {
            BlockBreakPre pre = new BlockBreakPre(rPlayer, block);
            bus.dispatchPre(pre);
            cancelled = pre.isDenied();
        } else {
            cancelled = false;
        }

        if (cancelled) {
            if (needsPost) bus.dispatchPost(new BlockBreakPost(rPlayer, block, true));
            if (needsAsync) bus.dispatchAsync(new BlockBreakSnapshot(rPlayer.uuid(), block.world().ref(), block.pos(), block.typeKey(), true));
            return false;
        }
        return true;
    }

    private void onBlockBreakPost(ServerLevel world, ServerPlayer player, BlockPos pos) {
        boolean needsPost = bus.hasPostListeners(BlockBreakPost.class);
        boolean needsAsync = bus.hasAsyncListeners(BlockBreakSnapshot.class);
        if (!needsPost && !needsAsync) return;

        RPlayer rPlayer = Rapunzel.players().require(player);
        RWorld rWorld = Rapunzel.worlds().require(world);
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RBlock block = Rapunzel.blocks().at(rWorld, rPos);

        if (needsPost) {
            bus.dispatchPost(new BlockBreakPost(rPlayer, block, false));
        }
        if (needsAsync) {
            bus.dispatchAsync(new BlockBreakSnapshot(rPlayer.uuid(), block.world().ref(), block.pos(), block.typeKey(), false));
        }
    }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        boolean needsInteractPre = bus.hasPreListeners(InteractBlockPre.class);
        boolean needsUsePre = bus.hasPreListeners(UseBlockPre.class);
        boolean needsUsePost = bus.hasPostListeners(UseBlockPost.class);
        boolean needsUseAsync = bus.hasAsyncListeners(UseBlockSnapshot.class);
        boolean needsPlacePre = bus.hasPreListeners(BlockPlacePre.class);
        boolean needsPlacePost = bus.hasPostListeners(BlockPlacePost.class);
        boolean needsPlaceAsync = bus.hasAsyncListeners(BlockPlaceSnapshot.class);

        if (!needsInteractPre && !needsUsePre && !needsUsePost && !needsUseAsync && !needsPlacePre && !needsPlacePost && !needsPlaceAsync) {
            return InteractionResult.PASS;
        }

        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);
        RWorld rWorld = Rapunzel.worlds().require(serverLevel);

        BlockPos clickedPos = hit.getBlockPos();
        RBlockPos rClickedPos = new RBlockPos(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
        RBlock clickedBlock = Rapunzel.blocks().at(rWorld, rClickedPos);

        boolean cancelled = false;

        if (needsInteractPre) {
            InteractBlockPre.Hand mappedHand = switch (hand) {
                case MAIN_HAND -> InteractBlockPre.Hand.MAIN_HAND;
                case OFF_HAND -> InteractBlockPre.Hand.OFF_HAND;
            };
            InteractBlockPre pre = new InteractBlockPre(
                    rPlayer,
                    clickedBlock,
                    InteractBlockPre.Action.RIGHT_CLICK_BLOCK,
                    mappedHand
            );
            bus.dispatchPre(pre);
            cancelled = pre.isDenied();
        }

        if (cancelled) {
            return InteractionResult.FAIL;
        }

        if (needsPlacePre || needsPlacePost || needsPlaceAsync) {
        ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof BlockItem blockItem) {
                Direction face = hit.getDirection();
                BlockPos placePos = clickedPos.relative(face);

                String placeKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString();
                RWorldRef worldRef = clickedBlock.world().ref();
                RBlockPos rPlacePos = new RBlockPos(placePos.getX(), placePos.getY(), placePos.getZ());

                if (needsPlacePre) {
                    BlockPlacePre pre = new BlockPlacePre(rPlayer, worldRef, rPlacePos, placeKey);
                    bus.dispatchPre(pre);
                    cancelled = pre.isDenied();
                }

                if (cancelled) {
                    if (needsPlacePost) bus.dispatchPost(new BlockPlacePost(rPlayer, worldRef, rPlacePos, placeKey, true));
                    if (needsPlaceAsync) bus.dispatchAsync(new BlockPlaceSnapshot(rPlayer.uuid(), worldRef, rPlacePos, placeKey, true));
                    return InteractionResult.FAIL;
                }

                if (needsPlacePost) bus.dispatchPost(new BlockPlacePost(rPlayer, worldRef, rPlacePos, placeKey, false));
                if (needsPlaceAsync) bus.dispatchAsync(new BlockPlaceSnapshot(rPlayer.uuid(), worldRef, rPlacePos, placeKey, false));
            }
        }

        if (needsUsePre) {
            UseBlockPre pre = new UseBlockPre(rPlayer, clickedBlock);
            bus.dispatchPre(pre);
            cancelled = cancelled || pre.isDenied();
        }

        if (cancelled) {
            if (needsUsePost) bus.dispatchPost(new UseBlockPost(rPlayer, clickedBlock, true));
            if (needsUseAsync) bus.dispatchAsync(new UseBlockSnapshot(rPlayer.uuid(), clickedBlock.world().ref(), clickedBlock.pos(), clickedBlock.typeKey(), true));
            return InteractionResult.FAIL;
        }

        if (needsUsePost) bus.dispatchPost(new UseBlockPost(rPlayer, clickedBlock, false));
        if (needsUseAsync) bus.dispatchAsync(new UseBlockSnapshot(rPlayer.uuid(), clickedBlock.world().ref(), clickedBlock.pos(), clickedBlock.typeKey(), false));

        return InteractionResult.PASS;
    }

    private InteractionResult onAttackBlock(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!bus.hasPreListeners(InteractBlockPre.class)) return InteractionResult.PASS;

        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);
        RWorld rWorld = Rapunzel.worlds().require(serverLevel);
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RBlock block = Rapunzel.blocks().at(rWorld, rPos);

        InteractBlockPre.Hand mappedHand = switch (hand) {
            case MAIN_HAND -> InteractBlockPre.Hand.MAIN_HAND;
            case OFF_HAND -> InteractBlockPre.Hand.OFF_HAND;
        };

        InteractBlockPre pre = new InteractBlockPre(rPlayer, block, InteractBlockPre.Action.LEFT_CLICK_BLOCK, mappedHand);
        bus.dispatchPre(pre);
        return pre.isDenied() ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    private InteractionResult onUseEntity(Player player, Level world, InteractionHand hand, net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.EntityHitResult hit) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!bus.hasPreListeners(InteractEntityPre.class)) return InteractionResult.PASS;

        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);
        String worldId = serverLevel.dimension().location().toString();
        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        BlockPos pos = entity.blockPosition();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

        InteractEntityPre pre = new InteractEntityPre(rPlayer, worldRef, rPos, typeKey);
        bus.dispatchPre(pre);
        return pre.isDenied() ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    private InteractionResult onAttackEntity(Player player, Level world, InteractionHand hand, net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.EntityHitResult hit) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!bus.hasPreListeners(AttackEntityPre.class)) return InteractionResult.PASS;

        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);
        String worldId = serverLevel.dimension().location().toString();
        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        BlockPos pos = entity.blockPosition();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

        AttackEntityPre pre = new AttackEntityPre(rPlayer, worldRef, rPos, typeKey);
        bus.dispatchPre(pre);
        return pre.isDenied() ? InteractionResult.FAIL : InteractionResult.PASS;
    }
}
