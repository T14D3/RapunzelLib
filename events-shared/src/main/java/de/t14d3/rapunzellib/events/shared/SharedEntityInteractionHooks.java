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
 *
 * <p>Contains the per-invocation "interact denied" flag used by the Fabric
 * bridge + {@code InteractEntityMixin} to guarantee exactly one
 * {@link InteractEntityPost} per physical interaction with the real outcome
 * ({@code cancelled}). The flag is thread-local because all server-side
 * interaction dispatch happens on the server main thread.</p>
 */
public final class SharedEntityInteractionHooks {
    private static final ThreadLocal<Boolean> INTERACT_DENIED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SharedEntityInteractionHooks() {
    }

    /**
     * Marks the current interaction as denied (set by the Fabric bridge's
     * pre-dispatch when a listener denies the interaction).
     */
    public static void markInteractDenied() {
        INTERACT_DENIED.set(Boolean.TRUE);
    }

    /**
     * Reads and clears the denied flag for the current invocation.
     *
     * @return {@code true} if the current interaction was denied by the pre-dispatch
     */
    public static boolean consumeInteractDenied() {
        boolean denied = INTERACT_DENIED.get();
        INTERACT_DENIED.set(Boolean.FALSE);
        return denied;
    }

    /**
     * Clears the denied flag for the current invocation.
     *
     * <p>Called by the Fabric bridge at the start of every pre-dispatch so a
     * stale flag left behind by a denied interaction that was short-circuited
     * before {@code Player.interactOn} (and thus never consumed by the mixin)
     * cannot leak into the next interaction.</p>
     */
    public static void clearInteractDenied() {
        INTERACT_DENIED.remove();
    }

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
