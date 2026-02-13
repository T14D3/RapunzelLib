package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record RegisteredCommandTree(
    @NotNull String registrationId,
    @NotNull RCommandTree<RCommandSource> tree,
    @NotNull List<RCommandNode<RCommandSource>> roots
) {
    public RegisteredCommandTree {
        Objects.requireNonNull(registrationId, "registrationId");
        Objects.requireNonNull(tree, "tree");
        roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
        if (registrationId.isBlank()) {
            throw new IllegalArgumentException("registrationId cannot be blank");
        }
    }
}
