package de.t14d3.rapunzellib.visuals.metadata;

import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Extracted metadata constants and convenience wrappers for Minecraft's
 * {@code BlockDisplay} entity data accessors.
 * <p>
 * Field indices are read directly from {@code EntityDataAccessor} static fields
 * at build time and provide indexed access to shared flags, transformation,
 * glow color, and block state data.
 */
public final class BlockDisplayMetadata {

    // ── Field indices ────────────────────────────────────────

    /** Index for shared flags (byte). */
    public static final int DATA_SHARED_FLAGS_ID = 0;
    /** Index for translation vector (Vector3f). */
    public static final int DATA_TRANSLATION_ID = 11;
    /** Index for scale vector (Vector3f). */
    public static final int DATA_SCALE_ID = 12;
    /** Index for left rotation (Quaternionf). */
    public static final int DATA_LEFT_ROTATION_ID = 13;
    /** Index for right rotation (Quaternionf). */
    public static final int DATA_RIGHT_ROTATION_ID = 14;
    /** Index for glow color (int). */
    public static final int DATA_GLOW_COLOR_ID = 22;
    /** Index for block state. */
    public static final int DATA_BLOCK_STATE_ID = 23;

    // ── Byte flags ─────────────────────────────────────────

    /** Flag bit for entity on fire. */
    public static final byte FLAG_ONFIRE = (byte) (1 << 0);
    /** Flag bit for shift key down. */
    public static final byte FLAG_SHIFT_KEY_DOWN = (byte) (1 << 1);
    /** Flag bit for sprinting. */
    public static final byte FLAG_SPRINTING = (byte) (1 << 3);
    /** Flag bit for swimming. */
    public static final byte FLAG_SWIMMING = (byte) (1 << 4);
    /** Flag bit for invisible. */
    public static final byte FLAG_INVISIBLE = (byte) (1 << 5);
    /** Flag bit for glowing. */
    public static final byte FLAG_GLOWING = (byte) (1 << 6);
    /** Flag bit for fall flying. */
    public static final byte FLAG_FALL_FLYING = (byte) (1 << 7);

    // ── Convenience wrappers ───────────────────────────────────

    /**
     * Creates the shared flags byte with the glowing flag optionally set.
     *
     * @param glowing whether the entity should glow
     * @return the flags byte
     */
    public static byte sharedFlags(boolean glowing) {
        return glowing ? FLAG_GLOWING : 0;
    }

    /**
     * Creates a {@link SynchedEntityData.DataValue} for the shared flags field.
     *
     * @param glowing whether the entity should glow
     * @return the data value
     */
    public static SynchedEntityData.DataValue<?> sharedFlagsData(boolean glowing) {
        return new SynchedEntityData.DataValue<>(
            DATA_SHARED_FLAGS_ID, EntityDataSerializers.BYTE, sharedFlags(glowing)
        );
    }

    /**
     * Creates a {@link SynchedEntityData.DataValue} for the block state field.
     *
     * @param state the block state
     * @return the data value
     */
    public static SynchedEntityData.DataValue<?> blockStateData(BlockState state) {
        return new SynchedEntityData.DataValue<>(
            DATA_BLOCK_STATE_ID, EntityDataSerializers.BLOCK_STATE, state
        );
    }

    /**
     * Creates a {@link SynchedEntityData.DataValue} for the glow color field.
     *
     * @param color the ARGB color
     * @return the data value
     */
    public static SynchedEntityData.DataValue<?> glowColorData(int color) {
        return new SynchedEntityData.DataValue<>(
            DATA_GLOW_COLOR_ID, EntityDataSerializers.INT, color
        );
    }

    /**
     * Creates a list of {@link SynchedEntityData.DataValue} for all transformation fields.
     *
     * @param translation  the translation vector
     * @param scale        the scale vector
     * @param leftRotation  the left rotation quaternion
     * @param rightRotation the right rotation quaternion
     * @return the list of data values
     */
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
