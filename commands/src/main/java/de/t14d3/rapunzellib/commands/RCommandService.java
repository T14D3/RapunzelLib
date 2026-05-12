package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing command registration and lifecycle.
 * <p>
 * Provides methods to register and unregister command roots and trees,
 * subscribe to change notifications, and build a shared command tree
 * for Brigadier integration. Supports both immediate and queued operations.
 * </p>
 */
public interface RCommandService {
    /**
     * A subscription that can be closed to unregister a change listener.
     */
    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Gets the platform identifier for this service.
     *
     * @return the platform ID
     */
    @NotNull PlatformId platformId();

    /**
     * Registers a root command node, using its name as the registration ID.
     *
     * @param root the root node to register
     * @return the registered command tree
     */
    @NotNull RegisteredCommandTree registerRoot(@NotNull RCommandNode<RCommandSource> root);

    /**
     * Registers a root command node with a specific registration ID.
     *
     * @param registrationId the unique registration identifier
     * @param root           the root node to register
     * @return the registered command tree
     */
    @NotNull RegisteredCommandTree registerRoot(@NotNull String registrationId, @NotNull RCommandNode<RCommandSource> root);

    /**
     * Queues a root command node for registration, using its name as the registration ID.
     *
     * @param root the root node to queue
     * @return the registered command tree
     */
    @NotNull RegisteredCommandTree queueRoot(@NotNull RCommandNode<RCommandSource> root);

    /**
     * Queues a root command node for registration with a specific registration ID.
     *
     * @param registrationId the unique registration identifier
     * @param root           the root node to queue
     * @return the registered command tree
     */
    @NotNull RegisteredCommandTree queueRoot(@NotNull String registrationId, @NotNull RCommandNode<RCommandSource> root);

    /**
     * Registers a command tree with a registration ID.
     *
     * @param registrationId the unique registration identifier
     * @param tree           the command tree to register
     * @return the registered command tree
     */
    @NotNull RegisteredCommandTree registerTree(@NotNull String registrationId, @NotNull RCommandTree<RCommandSource> tree);

    /**
     * Queues a command tree for registration.
     *
     * @param registrationId the unique registration identifier
     * @param tree           the command tree to queue
     * @return the registered command tree
     */
    @NotNull RegisteredCommandTree queueTree(@NotNull String registrationId, @NotNull RCommandTree<RCommandSource> tree);

    /**
     * Unregisters a previously registered command tree.
     *
     * @param registrationId the registration identifier to remove
     * @return true if the registration existed and was removed
     */
    boolean unregister(@NotNull String registrationId);

    /**
     * Queues unregistration of a command tree.
     *
     * @param registrationId the registration identifier to queue for removal
     * @return true if the registration existed and was queued for removal
     */
    boolean queueUnregister(@NotNull String registrationId);

    /**
     * Checks if there are any queued changes pending.
     *
     * @return true if changes are queued
     */
    boolean hasQueuedChanges();

    /**
     * Flushes all queued changes, notifying listeners.
     *
     * @return true if there were queued changes to flush
     */
    boolean flush();

    /**
     * Finds a registered command tree by its registration ID.
     *
     * @param registrationId the registration identifier
     * @return the registered tree, or empty if not found
     */
    @NotNull Optional<RegisteredCommandTree> find(@NotNull String registrationId);

    /**
     * Finds a root command node by its root name.
     *
     * @param rootName the root command name
     * @return the root node, or empty if not found
     */
    @NotNull Optional<RCommandNode<RCommandSource>> findRoot(@NotNull String rootName);

    /**
     * Gets a snapshot of all registered command trees.
     *
     * @return an unmodifiable list of registrations
     */
    @NotNull List<RegisteredCommandTree> registrations();

    /**
     * Gets a snapshot of all registered root nodes.
     *
     * @return an unmodifiable list of roots
     */
    @NotNull List<RCommandNode<RCommandSource>> roots();

    /**
     * Builds a shared tree containing all registered roots.
     *
     * @return a new command tree with all roots
     */
    @NotNull RCommandTree<RCommandSource> sharedTree();

    /**
     * Subscribes a listener for command service changes.
     *
     * @param listener the change listener
     * @return a subscription that can be closed to unregister the listener
     */
    default @NotNull Subscription subscribe(@NotNull Consumer<RCommandServiceChange> listener) {
        return () -> {
        };
    }
}
