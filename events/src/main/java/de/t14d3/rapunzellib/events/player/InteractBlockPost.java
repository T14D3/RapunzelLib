package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Post-event fired after a player has interacted with a block or with air.
 *
 * <p>Carries the same payload as {@link InteractBlockPre}: the player, the
 * click {@link InteractBlockPre.Action}, and - when the player clicked a
 * block - the {@link #block()} and {@link #face()}.</p>
 *
 * @param player    the interacting player
 * @param action    the click action
 * @param block     the clicked block, or {@code null} when the player clicked air
 * @param face      the clicked block face name, or {@code null} when the player clicked air
 * @param cancelled whether the interaction was cancelled
 */
public record InteractBlockPost(
    RPlayer player,
    InteractBlockPre.Action action,
    @Nullable RBlock block,
    @Nullable String face,
    boolean cancelled
) implements GamePostEvent {

    /**
     * Whether the player clicked a block (as opposed to air).
     *
     * @return true when {@link #block()} is present
     */
    public boolean hasBlock() {
        return block != null;
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
