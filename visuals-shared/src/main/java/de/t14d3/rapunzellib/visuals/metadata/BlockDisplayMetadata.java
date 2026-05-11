package de.t14d3.rapunzellib.visuals.metadata;

import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Extracted from Minecraft classes at build time.
 * Field indices are read directly from EntityDataAccessor static fields.
 */
public final class BlockDisplayMetadata {

    // ── Field indices ────────────────────────────────────────

    public static final int DATA_SHARED_FLAGS_ID = 0;
    public static final int DATA_TRANSLATION_ID = 11;
    public static final int DATA_SCALE_ID = 12;
    public static final int DATA_LEFT_ROTATION_ID = 13;
    public static final int DATA_RIGHT_ROTATION_ID = 14;
    public static final int DATA_GLOW_COLOR_ID = 22;
    public static final int DATA_BLOCK_STATE_ID = 23;

    // ── Byte flags ─────────────────────────────────────────

    public static final byte FLAG_ONFIRE = (byte) (1 << 0);
    public static final byte FLAG_SHIFT_KEY_DOWN = (byte) (1 << 1);
    public static final byte FLAG_SPRINTING = (byte) (1 << 3);
    public static final byte FLAG_SWIMMING = (byte) (1 << 4);
    public static final byte FLAG_INVISIBLE = (byte) (1 << 5);
    public static final byte FLAG_GLOWING = (byte) (1 << 6);
    public static final byte FLAG_FALL_FLYING = (byte) (1 << 7);

    // ── Convenience wrappers ───────────────────────────────────

    public static byte sharedFlags(boolean glowing) {
        return glowing ? FLAG_GLOWING : 0;
    }

    public static SynchedEntityData.DataValue<?> sharedFlagsData(boolean glowing) {
        return new SynchedEntityData.DataValue<>(
            DATA_SHARED_FLAGS_ID, EntityDataSerializers.BYTE, sharedFlags(glowing)
        );
    }

    public static SynchedEntityData.DataValue<?> blockStateData(BlockState state) {
        return new SynchedEntityData.DataValue<>(
            DATA_BLOCK_STATE_ID, EntityDataSerializers.BLOCK_STATE, state
        );
    }

    public static SynchedEntityData.DataValue<?> glowColorData(int color) {
        return new SynchedEntityData.DataValue<>(
            DATA_GLOW_COLOR_ID, EntityDataSerializers.INT, color
        );
    }

    public static List<SynchedEntityData.DataValue<?>> transformData(
            Vector3f translation, Vector3f scale,
            Quaternionf leftRotation, Quaternionf rightRotation) {
        return List.of(
            new SynchedEntityData.DataValue<>(DATA_TRANSLATION_ID, EntityDataSerializers.VECTOR3, translation),
            new SynchedEntityData.DataValue<>(DATA_SCALE_ID, EntityDataSerializers.VECTOR3, scale),
            new SynchedEntityData.DataValue<>(DATA_LEFT_ROTATION_ID, EntityDataSerializers.QUATERNION, leftRotation),
            new SynchedEntityData.DataValue<>(DATA_RIGHT_ROTATION_ID, EntityDataSerializers.QUATERNION, rightRotation)
        );
    }

    private BlockDisplayMetadata() {
    }
}
