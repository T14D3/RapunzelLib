package de.t14d3.rapunzellib.events.shared;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityDeathPost;
import de.t14d3.rapunzellib.events.entity.EntityDeathPre;
import de.t14d3.rapunzellib.nbt.shared.SharedAdventureComponentCodec;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared hooks for dispatching {@link EntityDeathPre} / {@link EntityDeathPost}
 * from the vanilla death methods ({@code LivingEntity.die} and the
 * {@code ServerPlayer.die} override, which does not delegate to the superclass
 * method in 26.1.2 - both need a mixin).
 *
 * <p>Payload mirrors the Paper bridge: the dying entity, the killer (the
 * damage source's causing entity), the damage type key, the death position and
 * the player-only death message (from the combat tracker).</p>
 *
 * <p>Death-progress tracking: {@code @At("RETURN")} injections fire at
 * <em>every</em> return instruction, including early returns taken when the
 * entity was already dead or when a NeoForge {@code LivingDeathEvent}
 * cancellation aborts the method. The in-progress marker (weakly keyed by
 * entity) plus the vanilla {@code dead} flag (passed in by the mixin, which
 * has direct field access) let the Post handler distinguish a completed death
 * from those aborted paths: Post fires only when the Pre dispatched AND, for
 * non-player entities, the death was actually committed ({@code dead == true};
 * {@code ServerPlayer.die} never sets {@code dead} - the player entity stays
 * alive for the respawn screen - so the marker alone decides for players).</p>
 */
public final class SharedEntityDeathHooks {
    private static final Map<Entity, Boolean> DEATH_IN_PROGRESS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private SharedEntityDeathHooks() {
    }

    /**
     * Dispatches {@link EntityDeathPre} for a dying entity.
     *
     * @param bus    the event bus
     * @param entity the dying entity (a player or a regular living entity)
     * @param source the death source
     * @return {@code true} when the death was denied and the caller must
     *         cancel the {@code die} invocation
     */
    public static boolean dispatchDeathPre(
        @NotNull GameEventBus bus,
        @NotNull LivingEntity entity,
        @NotNull DamageSource source
    ) {
        if (!bus.hasPreListeners(EntityDeathPre.class)) return false;
        if (!(entity.level() instanceof ServerLevel)) return false;

        REntity rEntity = Rapunzel.entities().require(entity);
        if (rEntity == null) return false;

        EntityDeathPre pre = new EntityDeathPre(
            rEntity,
            killerOf(source),
            damageTypeKey(source),
            positionOf(entity),
            deathMessageOf(entity)
        );
        DEATH_IN_PROGRESS.put(entity, Boolean.TRUE);
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            // The die() call is cancelled at HEAD, so the RETURN injection
            // never runs; clear the marker so a later die() call starts clean.
            DEATH_IN_PROGRESS.remove(entity);
            return true;
        }
        return false;
    }

    /**
     * Dispatches {@link EntityDeathPost} after the death processing completed.
     *
     * @param dead the vanilla {@code dead} flag read by the caller mixin at
     *             the return point ({@code true} only once a non-player death
     *             was actually committed by {@code LivingEntity.die})
     */
    public static void dispatchDeathPost(
        @NotNull GameEventBus bus,
        @NotNull LivingEntity entity,
        @NotNull DamageSource source,
        boolean dead
    ) {
        if (!bus.hasPostListeners(EntityDeathPost.class)) return;
        if (!(entity.level() instanceof ServerLevel)) return;

        // Without the marker the early returns (already-dead entry) would
        // produce a spurious Post for a death that never happened; without the
        // dead flag a NeoForge LivingDeathEvent cancellation (early return
        // before the death is committed) would too. Players never set `dead`
        // (ServerPlayer.die keeps the entity alive for the respawn screen).
        if (!DEATH_IN_PROGRESS.containsKey(entity)) return;
        if (!(entity instanceof ServerPlayer) && !dead) {
            DEATH_IN_PROGRESS.remove(entity);
            return;
        }

        DEATH_IN_PROGRESS.remove(entity);

        REntity rEntity = Rapunzel.entities().require(entity);
        if (rEntity == null) return;

        bus.dispatchPost(new EntityDeathPost(
            rEntity,
            killerOf(source),
            damageTypeKey(source),
            positionOf(entity),
            deathMessageOf(entity),
            false
        ));
    }

    /** The killer = the damage source's causing entity (Paper's getCausingEntity). */
    private static @Nullable REntity killerOf(@NotNull DamageSource source) {
        Entity causing = source.getEntity();
        if (causing == null) return null;
        return Rapunzel.entities().require(causing);
    }

    private static String damageTypeKey(@NotNull DamageSource source) {
        // #if VERSION >= 26
        return source.typeHolder().unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
        // #else
        return "minecraft:" + source.type().msgId();
        // #endif
    }

    private static RLocation positionOf(@NotNull LivingEntity entity) {
        // #if VERSION >= 1.21.11
        String worldId = ((ServerLevel) entity.level()).dimension().identifier().toString();
        // #else
        String worldId = ((ServerLevel) entity.level()).dimension().location().toString();
        // #endif
        return new RLocation(new RWorldRef(null, worldId), entity.getX(), entity.getY(), entity.getZ());
    }

    /**
     * Player-only death message (the chat broadcast), mirroring Paper's
     * {@code PlayerDeathEvent.deathMessage()}; {@code null} for non-player
     * deaths. Converted from the vanilla component via
     * {@link SharedAdventureComponentCodec}.
     */
    private static @Nullable Component deathMessageOf(@NotNull LivingEntity entity) {
        if (!(entity instanceof ServerPlayer)) return null;
        return SharedAdventureComponentCodec.toAdventure(entity.getCombatTracker().getDeathMessage());
    }
}
