package de.t14d3.rapunzellib.commands.core;

import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fluent builder for creating commands with a convenient chainable API.
 * <p>
 * This builder provides a convenient way to construct commands using method chaining,
 * making command definitions more readable and maintainable.
 * </p>
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * RCommandBuilder.create("teleport")
 *     .description("Teleport to a location")
 *     .requiresPermission("zones.teleport")
 *     .alias("tp")
 *     .executes((source, args) -> {
 *         Optional<String> location = args.getString("location");
 *         if (location.isPresent()) {
 *             // Teleport logic here
 *             return RCommandResult.SUCCESS;
 *         }
 *         return RCommandResult.FAILURE;
 *     })
 *     .build();
 * }</pre>
 * 
 * @param <S> the command source type, typically {@link RCommandSource}
 */
public class RCommandBuilder<S extends RCommandSource> {

    private final String name;
    private RCommandExecutor<S> executor;
    private RCommandSuggestionProvider<S> suggestionProvider;
    private Predicate<S> requirement;
    private String permission;
    private String description;
    private final List<String> aliases;
    private RCommandNode<S> redirect;

    /**
     * Creates a new command builder for the given command name.
     * 
     * @param name the name of the command
     * @return a new command builder
     */
    @NotNull
    public static <S extends RCommandSource> RCommandBuilder<S> create(@NotNull String name) {
        return new RCommandBuilder<>(name);
    }

    private RCommandBuilder(@NotNull String name) {
        this.name = name;
        this.aliases = new ArrayList<>();
        this.requirement = source -> true;
    }

    /**
     * Sets the command executor.
     * 
     * @param executor the executor to execute when this command is run
     * @return this builder for chaining
     */
    @NotNull
    public RCommandBuilder<S> executes(@NotNull RCommandExecutor<S> executor) {
        this.executor = executor;
        return this;
    }

    @NotNull
    public RCommandBuilder<S> suggests(@NotNull RCommandSuggestionProvider<S> suggestionProvider) {
        this.suggestionProvider = suggestionProvider;
        return this;
    }

    /**
     * Sets a requirement predicate for the command.
     * 
     * @param requirement the requirement predicate
     * @return this builder for chaining
     */
    @NotNull
    public RCommandBuilder<S> requires(@NotNull Predicate<S> requirement) {
        this.requirement = requirement;
        return this;
    }

    /**
     * Sets a permission requirement for the command.
     * <p>
     * This is a convenience method that creates a requirement predicate.
     * The actual permission checking should be done by the requirement predicate.
     * For example, on Paper/Bukkit:
     * <pre>{@code
     * .requires(source -> source.hasPermission("zones.teleport"))
     * }</pre>
     * 
     * @param permission the permission string required
     * @return this builder for chaining
     */
    @NotNull
    public RCommandBuilder<S> requiresPermission(@NotNull String permission) {
        this.permission = permission;
        // Store permission for metadata use, actual checking needs platform-specific logic
        // Platform implementation should bind this to actual permission checking
        return this;
    }

    /**
     * Sets the description of the command.
     * 
     * @param description the description text
     * @return this builder for chaining
     */
    @NotNull
    public RCommandBuilder<S> description(@Nullable String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds a single alias for the command.
     * 
     * @param alias the alias name
     * @return this builder for chaining
     */
    @NotNull
    public RCommandBuilder<S> alias(@NotNull String alias) {
        this.aliases.add(alias);
        return this;
    }

    /**
     * Adds multiple aliases for the command.
     * 
     * @param aliases the alias names
     * @return this builder for chaining
     */
    @NotNull
    public RCommandBuilder<S> aliases(@NotNull String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
        return this;
    }

    /**
     * Sets a redirect target for the command.
     * 
     * @param redirect the node to redirect to
     * @return this builder for chaining
     */
    @NotNull
    public RCommandBuilder<S> redirects(@NotNull RCommandNode<S> redirect) {
        this.redirect = redirect;
        return this;
    }

    /**
     * Builds the command node.
     * 
     * @return the constructed command node
     */
    @NotNull
    public RCommandNode<S> build() {
        @SuppressWarnings("unchecked")
        RCommandNode<S> node = (RCommandNode<S>) RCommandNode.literal(name);

        if (executor != null) {
            node.setExecutor(executor);
        }

        if (suggestionProvider != null) {
            node.setSuggestionProvider(suggestionProvider);
        }

        node.setRequirement(requirement);

        if (permission != null) {
            node.setPermission(permission);
        }

        if (description != null) {
            node.setDescription(description);
        }

        for (String alias : aliases) {
            node.addAlias(alias);
        }

        if (redirect != null) {
            node.redirects(redirect);
        }

        return node;
    }
}
