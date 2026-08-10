package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Pre-event fired before a player interacts with a block or with air.
 *
 * <p>This is the single merged interact event covering left and right
 * clicks on blocks and on air (the former {@code UseBlockPre} /
 * {@code InteractBlockPre} split is gone), plus physical "step" interactions
 * with blocks such as pressure plates and tripwires (Paper's
 * {@code Action.PHYSICAL} / {@code BlockPhysicsEvent} counterpart).</p>
 *
 * <p>This event is cancellable. If denied, the interaction will not occur.
 * The payload carries the player, the interaction {@link Action}
 * ({@code USE} for right-clicks, {@code ATTACK} for left-clicks, {@code STEP}
 * for physical contact), and a {@link #hasBlock()} flag: when the player
 * clicked a block, {@link #block()} is present together with the clicked
 * {@link #face()}; when the player clicked air, both are absent. {@code STEP}
 * interactions always carry a block and no face.</p>
 */
public final class InteractBlockPre extends BaseCancellablePreEvent {

    public enum Action {
        /** Right-click on a block or air ({@code USE} item interaction). */
        USE,
        /** Left-click on a block or air (attack/dig intent). */
        ATTACK,
        /** Physical contact, e.g. stepping on a pressure plate or tripwire. */
        STEP,
    }

    private final RPlayer player;
    private final Action action;
    private final RBlock block;
    private final String face;

    /**
     * Creates an air interaction event (no block, no face).
     */
    public InteractBlockPre(RPlayer player, Action action) {
        this(player, action, null, null, false);
    }

    /**
     * Creates an air interaction event with the initial cancelled state.
     */
    public InteractBlockPre(RPlayer player, Action action, boolean isCancelled) {
        this(player, action, null, null, isCancelled);
    }

    /**
     * Creates a block interaction event without face info.
     */
    public InteractBlockPre(RPlayer player, Action action, RBlock block) {
        this(player, action, block, null, false);
    }

    /**
     * Creates a block interaction event.
     *
     * @param player the interacting player
     * @param action the interaction action ({@link Action#USE}, {@link Action#ATTACK} or {@link Action#STEP})
     * @param block  the clicked block, or {@code null} when the player clicked air
     * @param face   the clicked block face (platform face name), or {@code null}
     */
    public InteractBlockPre(RPlayer player, Action action, @Nullable RBlock block, @Nullable String face) {
        this(player, action, block, face, false);
    }

    /**
     * Creates a block interaction event with the initial cancelled state.
     */
    public InteractBlockPre(RPlayer player, Action action, @Nullable RBlock block, @Nullable String face, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.action = Objects.requireNonNull(action, "action");
        this.block = block;
        this.face = face;
        setCancelled(isCancelled);
    }

    public RPlayer player() {
        return player;
    }

    public Action action() {
        return action;
    }

    /**
     * Whether the player clicked a block (as opposed to air).
     *
     * @return true when {@link #block()} is present
     */
    public boolean hasBlock() {
        return block != null;
    }

    /**
     * Returns the clicked block, or {@code null} when the player clicked air.
     *
     * @return the live block, or null
     */
    public @Nullable RBlock block() {
        return block;
    }

    /**
     * Returns the clicked block as an optional.
     *
     * @return the live block, or empty when the player clicked air
     */
    public Optional<RBlock> blockIfPresent() {
        return Optional.ofNullable(block);
    }

    /**
     * Returns the clicked block face (platform face name, e.g. "UP"), or
     * {@code null} when the player clicked air.
     *
     * @return the face name, or null
     */
    public @Nullable String face() {
        return face;
    }

    /**
     * Returns the clicked block face as an optional.
     *
     * @return the face name, or empty when the player clicked air
     */
    public Optional<String> faceIfPresent() {
        return Optional.ofNullable(face);
    }

    /**
     * Returns the world of the clicked block, if a block was clicked.
     */
    public Optional<RWorldRef> world() {
        return blockIfPresent().map(b -> b.world().ref());
    }

    /**
     * Returns the position of the clicked block, if a block was clicked.
     */
    public Optional<RBlockPos> pos() {
        return blockIfPresent().map(RBlock::pos);
    }

    /**
     * Returns the type key of the clicked block, if a block was clicked.
     */
    public Optional<RKey> blockTypeKey() {
        return blockIfPresent().map(RBlock::typeKey);
    }
}
