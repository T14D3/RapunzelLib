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

/**
 * Default implementation of {@link RCommandService}.
 * <p>
 * Manages command registrations, root nodes, and change listeners using a
 * thread-safe, synchronized approach. Supports both immediate and queued
 * registration/unregistration of command trees.
 * </p>
 */
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

    /**
     * Registers a root command node using its own name as the registration ID.
     *
     * @param root the root node to register
     * @return the registered command tree
     */
    @Override
    public @NotNull RegisteredCommandTree registerRoot(@NotNull RCommandNode<RCommandSource> root) {
        Objects.requireNonNull(root, "root");
        return registerRoot(root.getName(), root);
    }

    /**
     * Registers a root command node with a specific registration ID.
     *
     * @param registrationId the registration ID
     * @param root           the root node to register
     * @return the registered command tree
     */
    @Override
    public @NotNull RegisteredCommandTree registerRoot(
        @NotNull String registrationId,
        @NotNull RCommandNode<RCommandSource> root
    ) {
        return registerRoot(registrationId, root, false);
    }

    /**
     * Queues a root command node for registration using its own name as the registration ID.
     *
     * @param root the root node to queue
     * @return the registered command tree
     */
    @Override
    public @NotNull RegisteredCommandTree queueRoot(@NotNull RCommandNode<RCommandSource> root) {
        Objects.requireNonNull(root, "root");
        return queueRoot(root.getName(), root);
    }

    /**
     * Queues a root command node for registration with a specific registration ID.
     *
     * @param registrationId the unique registration identifier
     * @param root           the root node to queue
     * @return the registered command tree
     */
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

    /**
     * Registers a command tree with a given registration ID.
     *
     * @param registrationId the unique registration identifier
     * @param tree           the command tree to register
     * @return the registered command tree
     */
    @Override
    public @NotNull RegisteredCommandTree registerTree(
        @NotNull String registrationId,
        @NotNull RCommandTree<RCommandSource> tree
    ) {
        return registerTree(registrationId, tree, false);
    }

    /**
     * Queues a command tree for registration.
     *
     * @param registrationId the unique registration identifier
     * @param tree           the command tree to queue
     * @return the registered command tree
     */
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

    /**
     * Unregisters a previously registered command tree.
     *
     * @param registrationId the registration identifier to remove
     * @return true if the registration existed and was removed
     */
    @Override
    public boolean unregister(@NotNull String registrationId) {
        return unregister(registrationId, false);
    }

    /**
     * Queues unregistration of a command tree.
     *
     * @param registrationId the registration identifier to queue for removal
     * @return true if the registration existed and was queued for removal
     */
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

    /**
     * Checks if there are any queued changes pending.
     *
     * @return true if changes are queued
     */
    @Override
    public boolean hasQueuedChanges() {
        synchronized (lock) {
            return queuedChanges;
        }
    }

    /**
     * Flushes all queued changes, notifying listeners.
     *
     * @return true if there were queued changes to flush
     */
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

    /**
     * Finds a registered command tree by its registration ID.
     *
     * @param registrationId the registration identifier
     * @return the registered tree, or empty if not found
     */
    @Override
    public @NotNull Optional<RegisteredCommandTree> find(@NotNull String registrationId) {
        Objects.requireNonNull(registrationId, "registrationId");
        synchronized (lock) {
            return Optional.ofNullable(registrations.get(registrationId));
        }
    }

    /**
     * Finds a root command node by its root name.
     *
     * @param rootName the root command name
     * @return the root node, or empty if not found
     */
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

    /**
     * Builds a shared tree containing all registered roots.
     *
     * @return a new command tree with all roots
     */
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

    /**
     * Subscribes a listener for command service changes.
     *
     * @param listener the change listener
     * @return a subscription that can be closed to unregister the listener
     */
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
