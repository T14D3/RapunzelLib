package de.t14d3.rapunzellib.objects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides access to the Minecraft internal entity ID counter.
 *
 * <p>This is a low-level utility for assigning entity IDs outside of normal
 * Minecraft channels, used primarily on Mojang-mapped platforms.</p>
 */
public final class EntityIdAccessor {

    private EntityIdAccessor() {
    }

    private static AtomicInteger counter;

    static {
        try {
            // #if VERSION >= 26.2
            Field entityId = ServerLevel.class.getDeclaredField("ENTITY_COUNTER");
            // #else
            Field entityId = Entity.class.getDeclaredField("ENTITY_COUNTER");
            // #endif
            entityId.setAccessible(true);
            if (entityId.get(null) instanceof AtomicInteger atomicInteger) {
                counter = atomicInteger;
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            counter = new AtomicInteger(1_000_000);
            throw new RuntimeException(e);
        }

    }

    /**
     * Returns the next available entity ID.
     *
     * @return the next entity ID
     */
    public static int nextEntityId() {
        return counter.incrementAndGet();
    }
}
