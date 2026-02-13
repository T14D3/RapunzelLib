package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface RCommandService {
    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    @NotNull PlatformId platformId();

    @NotNull RegisteredCommandTree registerRoot(@NotNull RCommandNode<RCommandSource> root);

    @NotNull RegisteredCommandTree registerRoot(@NotNull String registrationId, @NotNull RCommandNode<RCommandSource> root);

    @NotNull RegisteredCommandTree queueRoot(@NotNull RCommandNode<RCommandSource> root);

    @NotNull RegisteredCommandTree queueRoot(@NotNull String registrationId, @NotNull RCommandNode<RCommandSource> root);

    @NotNull RegisteredCommandTree registerTree(@NotNull String registrationId, @NotNull RCommandTree<RCommandSource> tree);

    @NotNull RegisteredCommandTree queueTree(@NotNull String registrationId, @NotNull RCommandTree<RCommandSource> tree);

    boolean unregister(@NotNull String registrationId);

    boolean queueUnregister(@NotNull String registrationId);

    boolean hasQueuedChanges();

    boolean flush();

    @NotNull Optional<RegisteredCommandTree> find(@NotNull String registrationId);

    @NotNull Optional<RCommandNode<RCommandSource>> findRoot(@NotNull String rootName);

    @NotNull List<RegisteredCommandTree> registrations();

    @NotNull List<RCommandNode<RCommandSource>> roots();

    @NotNull RCommandTree<RCommandSource> sharedTree();

    default @NotNull Subscription subscribe(@NotNull Consumer<RCommandServiceChange> listener) {
        return () -> {
        };
    }
}
