package de.t14d3.rapunzellib.events.shared;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityEventPayloads;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Shared hooks for dispatching entity interaction and attack events.
 * <p>
 * Supports pre-cancellation checks and post-event notifications for
 * both interactions and attacks through the {@link GameEventBus}.
 */
public final class SharedEntityInteractionHooks {
    private SharedEntityInteractionHooks() {
    }

    /**
     * Dispatches a pre-interact event and returns whether it was denied.
     *
     * @param bus       the game event bus
     * @param player    the interacting player
     * @param target    the target entity
     * @param cancelled the current cancelled state
     * @return {@code true} if the interaction should be denied
     */
    public static boolean dispatchInteractPre(
        @NotNull GameEventBus bus,
        @NotNull ServerPlayer player,
        @NotNull Entity target,
        boolean cancelled
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        if (!bus.hasPreListeners(InteractEntityPre.class)) {
            return cancelled;
        }

        InteractEntityPre pre = new InteractEntityPre(
            Rapunzel.players().require(player),
            Rapunzel.entities().require(target),
            cancelled
        );
        bus.dispatchPre(pre);
        return pre.isDenied();
    }

    /**
     * Dispatches a post-interact event.
     *
     * @param bus       the game event bus
     * @param player    the interacting player
     * @param target    the target entity
     * @param cancelled whether the interaction was cancelled
     */
    public static void dispatchInteractPost(
        @NotNull GameEventBus bus,
        @NotNull ServerPlayer player,
        @NotNull Entity target,
        boolean cancelled
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        if (!bus.hasPostListeners(InteractEntityPost.class)) {
            return;
        }

        bus.dispatchPost(EntityEventPayloads.interactPost(
            Rapunzel.players().require(player),
            Rapunzel.entities().require(target),
            cancelled
        ));
    }

    /**
     * Dispatches a pre-attack event and returns whether it was denied.
     *
     * @param bus       the game event bus
     * @param player    the attacking player
     * @param target    the target entity
     * @param cancelled the current cancelled state
     * @return {@code true} if the attack should be denied
     */
    public static boolean dispatchAttackPre(
        @NotNull GameEventBus bus,
        @NotNull ServerPlayer player,
        @NotNull Entity target,
        boolean cancelled
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        if (!bus.hasPreListeners(AttackEntityPre.class)) {
            return cancelled;
        }

        AttackEntityPre pre = new AttackEntityPre(
            Rapunzel.players().require(player),
            Rapunzel.entities().require(target),
            cancelled
        );
        bus.dispatchPre(pre);
        return pre.isDenied();
    }

    /**
     * Dispatches a post-attack event.
     *
     * @param bus       the game event bus
     * @param player    the attacking player
     * @param target    the target entity
     * @param cancelled whether the attack was cancelled
     */
    public static void dispatchAttackPost(
        @NotNull GameEventBus bus,
        @NotNull ServerPlayer player,
        @NotNull Entity target,
        boolean cancelled
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        if (!bus.hasPostListeners(AttackEntityPost.class)) {
            return;
        }

        bus.dispatchPost(EntityEventPayloads.attackPost(
            Rapunzel.players().require(player),
            Rapunzel.entities().require(target),
            cancelled
        ));
    }
}
