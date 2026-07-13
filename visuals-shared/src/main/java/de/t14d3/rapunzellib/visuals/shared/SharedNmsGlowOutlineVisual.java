package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.objects.EntityIdAccessor;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.visuals.GlowOutlineConfig;
import de.t14d3.rapunzellib.visuals.GlowOutlineVisual;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import de.t14d3.rapunzellib.visuals.metadata.BlockDisplayMetadata;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared NMS implementation of a glow outline visual.
 * <p>
 * Renders a glowing outline around a set of block positions using
 * {@code minecraft:block_display} entities with a slight scale increase
 * and negative translation to create an outline effect.
 */
public final class SharedNmsGlowOutlineVisual extends SharedNmsVisual<GlowOutlineConfig> implements GlowOutlineVisual {

    private final Map<RBlockPos, Integer> entityIds = new HashMap<>();
    private final Map<RBlockPos, UUID> entityUuids = new HashMap<>();

    public SharedNmsGlowOutlineVisual(
        @NotNull VisualId id,
        @NotNull GlowOutlineConfig config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager
    ) {
        super(id, config, audience, manager);
    }

    @Override
    protected void spawnFor(@NotNull ServerPlayer player) {
        for (RBlockPos pos : config.blocks()) {
            int entityId = entityIds.computeIfAbsent(pos, ignored -> EntityIdAccessor.nextEntityId());
            UUID entityUuid = entityUuids.computeIfAbsent(pos, ignored -> UUID.randomUUID());
            Vec3 center = new Vec3(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
            player.connection.send(new ClientboundAddEntityPacket(
                entityId, entityUuid,
                center.x, center.y, center.z,
                0, 0,
                EntityType.BLOCK_DISPLAY,
                0,
                Vec3.ZERO,
                0
            ));

            BlockState state = resolveBlockState(config.outlineBlock());
            List<SynchedEntityData.DataValue<?>> data = new java.util.ArrayList<>();
            data.add(BlockDisplayMetadata.blockStateData(state));
            data.addAll(BlockDisplayMetadata.transformData(
                new Vector3f(-0.49F, -0.49F, -0.49F),
                new Vector3f(1.02F, 1.02F, 1.02F),
                new Quaternionf(),
                new Quaternionf()
            ));
            data.add(BlockDisplayMetadata.sharedFlagsData(true));
            data.add(BlockDisplayMetadata.glowColorData(config.color().value()));
            player.connection.send(new ClientboundSetEntityDataPacket(entityId, data));
        }
    }

    @Override
    protected void destroyFor(@NotNull ServerPlayer player) {
        int[] ids = entityIds.values().stream().mapToInt(Integer::intValue).toArray();
        if (ids.length > 0) {
            player.connection.send(new ClientboundRemoveEntitiesPacket(ids));
        }
    }

    private static BlockState resolveBlockState(@NotNull RBlockType blockType) {
        // #if VERSION >= 1.21.11
        Identifier id = Identifier.fromNamespaceAndPath(blockType.key().namespace(), blockType.key().path());
        // #else
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(blockType.key().namespace(), blockType.key().path());
        // #endif
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return block != null ? block.defaultBlockState() : Blocks.GLASS.defaultBlockState();
    }
}
