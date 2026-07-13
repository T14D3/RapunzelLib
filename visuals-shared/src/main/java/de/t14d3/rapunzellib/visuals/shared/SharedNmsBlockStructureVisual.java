package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.objects.EntityIdAccessor;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.visuals.BlockStructureConfig;
import de.t14d3.rapunzellib.visuals.BlockStructureShape;
import de.t14d3.rapunzellib.visuals.BlockStructureVisual;
import de.t14d3.rapunzellib.visuals.ConcreteColorResolver;
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
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared NMS implementation of a block structure visual.
 * <p>
 * Renders a multi-face block structure using multiple {@code minecraft:block_display}
 * entities, one per face of the configured shape.
 */
public final class SharedNmsBlockStructureVisual extends SharedNmsVisual<BlockStructureConfig> implements BlockStructureVisual {
    private final List<FaceEntity> faces = new ArrayList<>();
    private RBlockType currentBlock;

    public SharedNmsBlockStructureVisual(
        @NotNull VisualId id,
        @NotNull BlockStructureConfig config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager
    ) {
        super(id, config, audience, manager);
        this.currentBlock = ConcreteColorResolver.resolve(config.color());
        buildFaces();
    }

    @Override
    public void updateColor(@NotNull TextColor color) {
        RBlockType newBlock = ConcreteColorResolver.resolve(color);
        this.currentBlock = newBlock;
        if (!shown) return;
        BlockState state = resolveBlockState(newBlock);
        sendDataToAll(List.of(BlockDisplayMetadata.blockStateData(state)));
    }

    private void buildFaces() {
        faces.clear();
        for (BlockStructureShape.Face face : config.shape().faces()) {
            int entityId = EntityIdAccessor.nextEntityId();
            faces.add(new FaceEntity(entityId, UUID.randomUUID(), face));
        }
    }

    @Override
    protected @NotNull RLocation visibilityCenter() {
        List<BlockStructureShape.Face> faceList = config.shape().faces();
        if (faceList.isEmpty()) {
            return super.visibilityCenter();
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (BlockStructureShape.Face f : faceList) {
            double px = f.center().x();
            double py = f.center().y();
            double pz = f.center().z();
            minX = Math.min(minX, px);
            minY = Math.min(minY, py);
            minZ = Math.min(minZ, pz);
            maxX = Math.max(maxX, px + f.scale().x());
            maxY = Math.max(maxY, py + f.scale().y());
            maxZ = Math.max(maxZ, pz + f.scale().z());
        }
        BlockStructureShape.Face first = faceList.get(0);
        return new RLocation(first.center().world(), (minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0, 0f, 0f);
    }

    @Override
    protected void spawnFor(@NotNull ServerPlayer player) {
        BlockState state = resolveBlockState(currentBlock);
        for (FaceEntity face : faces) {
            RLocation center = face.face.center();
            player.connection.send(new ClientboundAddEntityPacket(
                face.entityId, face.uuid,
                center.x(), center.y(), center.z(),
                0, 0,
                EntityType.BLOCK_DISPLAY,
                0,
                Vec3.ZERO,
                0
            ));

            de.t14d3.rapunzellib.visuals.Vector3f scale = face.face.scale();
            List<SynchedEntityData.DataValue<?>> data = new ArrayList<>();
            data.add(BlockDisplayMetadata.blockStateData(state));
            data.addAll(BlockDisplayMetadata.transformData(
                new Vector3f(0, 0, 0),
                new Vector3f(scale.x(), scale.y(), scale.z()),
                new Quaternionf(),
                new Quaternionf()
            ));
            data.add(BlockDisplayMetadata.sharedFlagsData(config.glow()));
            data.add(BlockDisplayMetadata.glowColorData(config.color().value()));
            player.connection.send(new ClientboundSetEntityDataPacket(face.entityId, data));
        }
    }

    @Override
    protected void destroyFor(@NotNull ServerPlayer player) {
        int[] ids = faces.stream().mapToInt(f -> f.entityId).toArray();
        if (ids.length > 0) {
            player.connection.send(new ClientboundRemoveEntitiesPacket(ids));
        }
    }

    private void sendDataToAll(@NotNull List<SynchedEntityData.DataValue<?>> data) {
        for (UUID uuid : Set.copyOf(currentViewers)) {
            de.t14d3.rapunzellib.objects.RPlayer player = de.t14d3.rapunzellib.objects.RPlayer.get(uuid).orElse(null);
            if (player == null) continue;
            ServerPlayer serverPlayer = tryUnwrap(player);
            if (serverPlayer == null) continue;
            for (FaceEntity face : faces) {
                serverPlayer.connection.send(new ClientboundSetEntityDataPacket(face.entityId, data));
            }
        }
    }

    private static BlockState resolveBlockState(@NotNull RBlockType blockType) {
        // #if VERSION >= 1.21.11
        Identifier id = Identifier.fromNamespaceAndPath(blockType.key().namespace(), blockType.key().path());
        // #else
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(blockType.key().namespace(), blockType.key().path());
        // #endif
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return block != null ? block.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    private record FaceEntity(int entityId, @NotNull UUID uuid, @NotNull BlockStructureShape.Face face) {
    }
}
