package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A registered command tree with its associated metadata.
 * <p>
 * Records the unique registration ID, the full command tree, and the
 * root nodes extracted from that tree. The roots list is defensively copied.
 * </p>
 *
 * @param registrationId the unique registration identifier
 * @param tree           the command tree
 * @param roots          the root nodes of the tree
 */
public record RegisteredCommandTree(
    @NotNull String registrationId,
    @NotNull RCommandTree<RCommandSource> tree,
    @NotNull List<RCommandNode<RCommandSource>> roots
) {
    /**
     * Validates the record components.
     *
     * @throws NullPointerException     if any component is null
     * @throws IllegalArgumentException if registrationId is blank
     */
    public RegisteredCommandTree {
        Objects.requireNonNull(registrationId, "registrationId");
        Objects.requireNonNull(tree, "tree");
        roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
        if (registrationId.isBlank()) {
            throw new IllegalArgumentException("registrationId cannot be blank");
        }
    }
}
