package de.t14d3.rapunzellib.objects;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

public final class EntityIdAccessor {

    private EntityIdAccessor() {
    }

    private static AtomicInteger counter;

    static {
        try {
            Field entityId = Entity.class.getDeclaredField("ENTITY_COUNTER");
            entityId.setAccessible(true);
            if (entityId.get(null) instanceof AtomicInteger atomicInteger) {
                counter = atomicInteger;
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            counter = new AtomicInteger(1_000_000);
            throw new RuntimeException(e);
        }

    }

    public static int nextEntityId() {
        return counter.incrementAndGet();
    }
}
