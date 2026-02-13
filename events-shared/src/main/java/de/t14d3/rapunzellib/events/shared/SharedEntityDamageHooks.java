package de.t14d3.rapunzellib.events.shared;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityEventPayloads;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SharedEntityDamageHooks {
    private SharedEntityDamageHooks() {
    }

    public static void dispatchHurtOutcome(
        @NotNull GameEventBus bus,
        @NotNull Entity entity,
        @NotNull String damageTypeKey,
        boolean cancelled
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(damageTypeKey, "damageTypeKey");
        boolean needsPost = bus.hasPostListeners(EntityHurtPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntityHurtSnapshot.class);
        if (!needsPost && !needsAsync) {
            return;
        }

        var rEntity = Rapunzel.entities().require(entity);
        if (needsPost) {
            bus.dispatchPost(EntityEventPayloads.hurtPost(rEntity, damageTypeKey, cancelled));
        }
        if (needsAsync) {
            bus.dispatchAsync(EntityEventPayloads.hurtSnapshot(rEntity, damageTypeKey, cancelled));
        }
    }
}
