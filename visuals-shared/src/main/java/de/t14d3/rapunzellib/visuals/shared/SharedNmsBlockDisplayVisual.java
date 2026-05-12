package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.objects.EntityIdAccessor;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.visuals.BlockDisplayConfig;
import de.t14d3.rapunzellib.visuals.BlockDisplayVisual;
import de.t14d3.rapunzellib.visuals.DisplayTransform;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared NMS implementation of a block display entity visual.
 * <p>
 * Spawns a {@code minecraft:block_display} entity for viewers and
 * supports dynamic updates to the displayed block, transform, and glow color.
 */
public final class SharedNmsBlockDisplayVisual extends SharedNmsVisual<BlockDisplayConfig> implements BlockDisplayVisual {

    private final RLocation location;
    private final int entityId;
    private final UUID entityUuid;
    private RBlockType currentBlock;
    private DisplayTransform currentTransform;

    /**
     * Creates a new block display visual.
     *
     * @param id       the visual ID
     * @param config   the block display config
     * @param audience the visual audience
     * @param manager  the visual manager
     * @param location the world location
     */
    public SharedNmsBlockDisplayVisual(
        @NotNull VisualId id,
        @NotNull BlockDisplayConfig config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager,
        @NotNull RLocation location
    ) {
        super(id, config, audience, manager);
        this.location = location;
        this.entityId = EntityIdAccessor.nextEntityId();
        this.entityUuid = UUID.randomUUID();
        this.currentBlock = config.block();
        this.currentTransform = config.transform();
    }

    @Override
    public void updateTransform(@NotNull DisplayTransform transform) {
        this.currentTransform = transform;
        if (!shown) return;
        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>(BlockDisplayMetadata.transformData(
            toNms(transform.translation()), toNms(transform.scale()),
            toNms(transform.leftRotation()), toNms(transform.rightRotation())
        ));
        data.add(BlockDisplayMetadata.sharedFlagsData(config.glow()));
        data.add(BlockDisplayMetadata.glowColorData(config.color().value()));
        sendData(data);
    }

    @Override
    public void updateBlock(@NotNull RBlockType block) {
        this.currentBlock = block;
        if (!shown) return;
        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>();
        data.add(BlockDisplayMetadata.blockStateData(resolveBlockState(block)));
        data.add(BlockDisplayMetadata.sharedFlagsData(config.glow()));
        data.add(BlockDisplayMetadata.glowColorData(config.color().value()));
        sendData(data);
    }

    @Override
    public void updateColor(@NotNull net.kyori.adventure.text.format.TextColor color) {
        if (!shown) return;
        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>();
        data.add(BlockDisplayMetadata.sharedFlagsData(config.glow()));
        data.add(BlockDisplayMetadata.glowColorData(config.color().value()));
        sendData(data);
    }

    @Override
    protected @NotNull RLocation visibilityCenter() {
        return location;
    }

    @Override
    protected void spawnFor(@NotNull ServerPlayer player) {
        player.connection.send(new ClientboundAddEntityPacket(
            entityId,
            entityUuid,
            location.x(), location.y(), location.z(),
            0, 0,
            EntityType.BLOCK_DISPLAY,
            0,
            Vec3.ZERO,
            0
        ));

        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>();
        data.add(BlockDisplayMetadata.blockStateData(resolveBlockState(currentBlock)));
        data.addAll(BlockDisplayMetadata.transformData(
            toNms(currentTransform.translation()), toNms(currentTransform.scale()),
            toNms(currentTransform.leftRotation()), toNms(currentTransform.rightRotation())
        ));
        data.add(BlockDisplayMetadata.sharedFlagsData(config.glow()));
        data.add(BlockDisplayMetadata.glowColorData(config.color().value()));
        player.connection.send(new ClientboundSetEntityDataPacket(entityId, data));
    }

    @Override
    protected void destroyFor(@NotNull ServerPlayer player) {
        player.connection.send(new ClientboundRemoveEntitiesPacket(entityId));
    }

    /**
     * Sends entity data update packets to all current viewers.
     *
     * @param data the data values to send
     */
    private void sendData(@NotNull List<SynchedEntityData.DataValue<?>> data) {
        for (UUID uuid : Set.copyOf(currentViewers)) {
            de.t14d3.rapunzellib.objects.RPlayer player = de.t14d3.rapunzellib.objects.RPlayer.get(uuid).orElse(null);
            if (player == null) continue;
            ServerPlayer serverPlayer = tryUnwrap(player);
            if (serverPlayer == null) continue;
            serverPlayer.connection.send(new ClientboundSetEntityDataPacket(entityId, data));
        }
    }

    /**
     * Resolves a block state from an RBlockType.
     *
     * @param blockType the block type
     * @return the block state, or air if not found
     */
    private static BlockState resolveBlockState(@NotNull RBlockType blockType) {
        // #if VERSION >= 1.21.11
        Identifier id = Identifier.fromNamespaceAndPath(blockType.key().namespace(), blockType.key().path());
        // #else
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(blockType.key().namespace(), blockType.key().path());
        // #endif
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return block != null ? block.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    /**
     * Converts a Rapunzel Vector3f to a JOML Vector3f.
     *
     * @param vector the Rapunzel vector
     * @return the JOML vector
     */
    private static Vector3f toNms(de.t14d3.rapunzellib.visuals.Vector3f vector) {
        return new Vector3f(vector.x(), vector.y(), vector.z());
    }

    /**
     * Converts a Rapunzel Quaternionf to a JOML Quaternionf.
     *
     * @param quaternion the Rapunzel quaternion
     * @return the JOML quaternion
     */
    private static Quaternionf toNms(de.t14d3.rapunzellib.visuals.Quaternionf quaternion) {
        return new Quaternionf(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
    }
}
