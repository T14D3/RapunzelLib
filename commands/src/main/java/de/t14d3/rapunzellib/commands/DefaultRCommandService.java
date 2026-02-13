package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

final class DefaultRCommandService implements RCommandService {
    private final PlatformId platformId;
    private final Map<String, RegisteredCommandTree> registrations = new LinkedHashMap<>();
    private final Map<String, RCommandNode<RCommandSource>> roots = new LinkedHashMap<>();
    private final List<Consumer<RCommandServiceChange>> listeners = new ArrayList<>();
    private final Object lock = new Object();
    private boolean queuedChanges;

    DefaultRCommandService(@NotNull PlatformId platformId) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    @Override
    public @NotNull PlatformId platformId() {
        return platformId;
    }

    @Override
    public @NotNull RegisteredCommandTree registerRoot(@NotNull RCommandNode<RCommandSource> root) {
        Objects.requireNonNull(root, "root");
        return registerRoot(root.getName(), root);
    }

    @Override
    public @NotNull RegisteredCommandTree registerRoot(
        @NotNull String registrationId,
        @NotNull RCommandNode<RCommandSource> root
    ) {
        return registerRoot(registrationId, root, false);
    }

    @Override
    public @NotNull RegisteredCommandTree queueRoot(@NotNull RCommandNode<RCommandSource> root) {
        Objects.requireNonNull(root, "root");
        return queueRoot(root.getName(), root);
    }

    @Override
    public @NotNull RegisteredCommandTree queueRoot(
        @NotNull String registrationId,
        @NotNull RCommandNode<RCommandSource> root
    ) {
        return registerRoot(registrationId, root, true);
    }

    private @NotNull RegisteredCommandTree registerRoot(
        @NotNull String registrationId,
        @NotNull RCommandNode<RCommandSource> root,
        boolean queued
    ) {
        Objects.requireNonNull(root, "root");
        RCommandTree<RCommandSource> tree = new RCommandTree<>();
        tree.register(root);
        return registerTree(registrationId, tree, queued);
    }

    @Override
    public @NotNull RegisteredCommandTree registerTree(
        @NotNull String registrationId,
        @NotNull RCommandTree<RCommandSource> tree
    ) {
        return registerTree(registrationId, tree, false);
    }

    @Override
    public @NotNull RegisteredCommandTree queueTree(
        @NotNull String registrationId,
        @NotNull RCommandTree<RCommandSource> tree
    ) {
        return registerTree(registrationId, tree, true);
    }

    private @NotNull RegisteredCommandTree registerTree(
        @NotNull String registrationId,
        @NotNull RCommandTree<RCommandSource> tree,
        boolean queued
    ) {
        Objects.requireNonNull(registrationId, "registrationId");
        Objects.requireNonNull(tree, "tree");
        if (registrationId.isBlank()) {
            throw new IllegalArgumentException("registrationId cannot be blank");
        }

        List<RCommandNode<RCommandSource>> registrationRoots = List.copyOf(tree.getRoots());
        RegisteredCommandTree registration = new RegisteredCommandTree(registrationId, tree, registrationRoots);

        synchronized (lock) {
            if (registrations.containsKey(registrationId)) {
                throw new IllegalArgumentException("Command registration already exists: " + registrationId);
            }

            for (RCommandNode<RCommandSource> root : registrationRoots) {
                requireRoot(root);
                String rootName = root.getName();
                if (roots.containsKey(rootName)) {
                    throw new IllegalArgumentException("Command root already registered: " + rootName);
                }
            }

            registrations.put(registrationId, registration);
            for (RCommandNode<RCommandSource> root : registrationRoots) {
                roots.put(root.getName(), root);
            }
            queuedChanges = queued;
        }

        notifyListeners(new RCommandServiceChange(RCommandServiceChange.Type.REGISTERED, registration, queued));
        return registration;
    }

    @Override
    public boolean unregister(@NotNull String registrationId) {
        return unregister(registrationId, false);
    }

    @Override
    public boolean queueUnregister(@NotNull String registrationId) {
        return unregister(registrationId, true);
    }

    private boolean unregister(@NotNull String registrationId, boolean queued) {
        Objects.requireNonNull(registrationId, "registrationId");
        RegisteredCommandTree removed;
        synchronized (lock) {
            removed = registrations.remove(registrationId);
            if (removed == null) {
                return false;
            }
            for (RCommandNode<RCommandSource> root : removed.roots()) {
                roots.remove(root.getName());
            }
            queuedChanges = queued;
        }
        notifyListeners(new RCommandServiceChange(RCommandServiceChange.Type.UNREGISTERED, removed, queued));
        return true;
    }

    @Override
    public boolean hasQueuedChanges() {
        synchronized (lock) {
            return queuedChanges;
        }
    }

    @Override
    public boolean flush() {
        synchronized (lock) {
            if (!queuedChanges) {
                return false;
            }
            queuedChanges = false;
        }
        notifyListeners(new RCommandServiceChange(RCommandServiceChange.Type.FLUSH_REQUESTED, null, false));
        return true;
    }

    @Override
    public @NotNull Optional<RegisteredCommandTree> find(@NotNull String registrationId) {
        Objects.requireNonNull(registrationId, "registrationId");
        synchronized (lock) {
            return Optional.ofNullable(registrations.get(registrationId));
        }
    }

    @Override
    public @NotNull Optional<RCommandNode<RCommandSource>> findRoot(@NotNull String rootName) {
        Objects.requireNonNull(rootName, "rootName");
        synchronized (lock) {
            return Optional.ofNullable(roots.get(rootName));
        }
    }

    @Override
    public @NotNull List<RegisteredCommandTree> registrations() {
        synchronized (lock) {
            return List.copyOf(registrations.values());
        }
    }

    @Override
    public @NotNull List<RCommandNode<RCommandSource>> roots() {
        synchronized (lock) {
            return List.copyOf(roots.values());
        }
    }

    @Override
    public @NotNull RCommandTree<RCommandSource> sharedTree() {
        List<RCommandNode<RCommandSource>> sharedRoots;
        synchronized (lock) {
            sharedRoots = new ArrayList<>(roots.values());
        }

        RCommandTree<RCommandSource> shared = new RCommandTree<>();
        for (RCommandNode<RCommandSource> root : sharedRoots) {
            shared.register(root);
        }
        return shared;
    }

    @Override
    public @NotNull Subscription subscribe(@NotNull Consumer<RCommandServiceChange> listener) {
        Objects.requireNonNull(listener, "listener");
        synchronized (lock) {
            listeners.add(listener);
        }
        return () -> {
            synchronized (lock) {
                listeners.remove(listener);
            }
        };
    }

    private static void requireRoot(@NotNull RCommandNode<RCommandSource> root) {
        if (!root.isRoot()) {
            throw new IllegalArgumentException("Cannot register non-root node: " + root.getPath());
        }
    }

    private void notifyListeners(@NotNull RCommandServiceChange change) {
        List<Consumer<RCommandServiceChange>> snapshot;
        synchronized (lock) {
            snapshot = List.copyOf(listeners);
        }
        for (Consumer<RCommandServiceChange> listener : snapshot) {
            listener.accept(change);
        }
    }
}
