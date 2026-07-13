package de.t14d3.rapunzellib.commands.core;

import de.t14d3.rapunzellib.commands.CommandFeatures;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.RegisteredCommandTree;
import de.t14d3.rapunzellib.commands.arguments.RArgument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Represents a node in the command tree structure.
 * <p>
 * This class provides a more convenient API for building and managing command trees in RapunzelLib.
 * Command nodes form a tree structure where each node represents a command or command argument.
 * </p>
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Child node management for building command trees</li>
 *   <li>Command execution through {@link RCommandExecutor}</li>
 *   <li>Permission and requirement-based access control</li>
 *   <li>Command aliases for alternative names</li>
 *   <li>Descriptions for help systems</li>
 * </ul>
 * 
 * @param <S> the command source type, typically {@link RCommandSource}
 */
public class RCommandNode<S extends RCommandSource> {

    private final String name;
    private final RCommandNode<S> parent;
    private final RArgument<S, ?> argument;
    private final Map<String, RCommandNode<S>> children;
    private RCommandExecutor<S> executor;
    private RCommandNode<S> executionDelegate;
    private RCommandSuggestionProvider<S> suggestionProvider;
    private Predicate<S> requirement;
    private String permission;
    private String description;
    private final List<String> aliases;
    private RCommandNode<S> redirect;
    
    public static <S extends RCommandSource> RCommandNode<S> literal(@NotNull String name) {
        return new RCommandNode<>(name, null, null);
    }

    public static <S extends RCommandSource, T> RCommandNode<S> argument(@NotNull RArgument<S, T> argument) {
        return new RCommandNode<>(argument.getName(), null, argument);
    }

    private RCommandNode(
        @NotNull String name,
        @Nullable RCommandNode<S> parent,
        @Nullable RArgument<S, ?> argument
    ) {
        this.name = name;
        this.parent = parent;
        this.argument = argument;
        this.children = new LinkedHashMap<>();
        this.aliases = new ArrayList<>();
        this.requirement = s -> true;
    }
    
    @NotNull
    public RCommandNode<S> then(@NotNull String name) {
        if (children.containsKey(name)) {
            return children.get(name);
        }
        RCommandNode<S> child = new RCommandNode<>(name, this, null);
        children.put(name, child);
        return child;
    }

    /**
     * Adds an argument child node from an {@link RArgument} definition.
     *
     * @param argument the argument definition
     * @param <T>      the argument value type
     * @return the child node
     */
    @NotNull
    public <T> RCommandNode<S> then(@NotNull RArgument<S, T> argument) {
        String argumentName = argument.getName();
        if (children.containsKey(argumentName)) {
            return children.get(argumentName);
        }
        RCommandNode<S> child = new RCommandNode<>(argumentName, this, argument);
        children.put(argumentName, child);
        return child;
    }

    /**
     * Adds a preconfigured argument branch that stays on this node's execution
     * path unless it defines its own executor.
     *
     * @param child the detached argument node to attach
     * @return this node for chaining
     */
    @NotNull
    public RCommandNode<S> argument(@NotNull RCommandNode<S> child) {
        RCommandNode<S> attachedChild = attachChild(child);
        attachedChild.executionDelegate = this;
        return this;
    }

    /**
     * Adds a preconfigured child node and keeps this node as the active builder.
     *
     * <p>This mirrors Brigadier's nested builder style so callers can write
     * {@code root.then(RCommandNode.argument(...).suggests(...).executes(...))}.</p>
     *
     * @param child the child node to attach
     * @return this node for chaining
     */
    @NotNull
    public RCommandNode<S> then(@NotNull RCommandNode<S> child) {
        attachChild(child);
        return this;
    }
    
    /**
     * Adds a child node to this node.
     * 
     * @param child the child node to add
     * @return this node for chaining
     */
    @NotNull
    public RCommandNode<S> addChild(@NotNull RCommandNode<S> child) {
        attachChild(child);
        return this;
    }
    
    @Nullable
    public RCommandNode<S> getChild(@NotNull String name) {
        return children.get(name);
    }
    
    @NotNull
    public Collection<RCommandNode<S>> getChildren() {
        return Collections.unmodifiableCollection(children.values());
    }
    
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Checks if this node is a literal (non-argument) node.
     *
     * @return true if this is a literal node
     */
    public boolean isLiteral() {
        return argument == null;
    }

    /**
     * Checks if this node is an argument node.
     *
     * @return true if this is an argument node
     */
    public boolean isArgument() {
        return argument != null;
    }

    public @Nullable RArgument<S, ?> getArgument() {
        return argument;
    }
    
    @Nullable
    public RCommandNode<S> getParent() {
        return parent;
    }
    
    @Nullable
    public RCommandExecutor<S> getExecutor() {
        return executor;
    }

    @Nullable
    public RCommandNode<S> getExecutionDelegate() {
        return executionDelegate;
    }
    
    @NotNull
    public RCommandNode<S> setExecutor(@NotNull RCommandExecutor<S> executor) {
        this.executor = executor;
        return this;
    }
    
    @NotNull
    public RCommandNode<S> executes(@NotNull RCommandExecutor<S> executor) {
        this.executor = executor;
        return this;
    }

    @Nullable
    public RCommandSuggestionProvider<S> getSuggestionProvider() {
        return suggestionProvider;
    }

    @NotNull
    public RCommandNode<S> setSuggestionProvider(@NotNull RCommandSuggestionProvider<S> suggestionProvider) {
        this.suggestionProvider = suggestionProvider;
        return this;
    }

    @NotNull
    public RCommandNode<S> suggests(@NotNull RCommandSuggestionProvider<S> suggestionProvider) {
        this.suggestionProvider = suggestionProvider;
        return this;
    }
    
    @NotNull
    public Predicate<S> getRequirement() {
        return requirement;
    }
    
    @NotNull
    public RCommandNode<S> setRequirement(@NotNull Predicate<S> requirement) {
        this.requirement = requirement;
        return this;
    }
    
    @NotNull
    public RCommandNode<S> requires(@NotNull Predicate<S> requirement) {
        this.requirement = requirement;
        return this;
    }
    
    @NotNull
    public RCommandNode<S> setPermission(@Nullable String permission) {
        this.permission = permission;
        return this;
    }
    
    @NotNull
    public RCommandNode<S> requiresPermission(@NotNull String permission) {
        this.permission = permission;
        return this;
    }
    
    @Nullable
    public String getPermission() {
        return permission;
    }
    
    @NotNull
    public RCommandNode<S> setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }
    
    @Nullable
    public String getDescription() {
        return description;
    }
    
    /**
     * Adds an alias for this command node.
     * 
     * @param alias the alias name
     * @return this node for chaining
     */
    @NotNull
    public RCommandNode<S> addAlias(@NotNull String alias) {
        this.aliases.add(alias);
        return this;
    }
    
    @NotNull
    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }
    
    @Nullable
    public RCommandNode<S> getRedirect() {
        return redirect;
    }
    
    /**
     * Redirects this node to another node.
     * 
     * @param redirect the node to redirect to
     * @return this node for chaining
     */
    @NotNull
    public RCommandNode<S> redirects(@NotNull RCommandNode<S> redirect) {
        this.redirect = redirect;
        return this;
    }
    
    /**
     * Checks if this node is a root command (has no parent).
     * 
     * @return true if this is a root node
     */
    public boolean isRoot() {
        return parent == null;
    }
    
    /**
     * Checks if this node has an executor.
     * 
     * @return true if this node is executable
     */
    public boolean isExecutable() {
        return executor != null;
    }
    
    @NotNull
    public String getPath() {
        if (parent == null) {
            return isArgument() ? '<' + name + '>' : name;
        }
        String parentPath = parent.getPath();
        String segment = isArgument() ? '<' + name + '>' : name;
        return parentPath.isEmpty() ? segment : parentPath + " " + segment;
    }

    private @NotNull RCommandNode<S> attachChild(@NotNull RCommandNode<S> child) {
        Map<RCommandNode<S>, RCommandNode<S>> copies = new LinkedHashMap<>();
        RCommandNode<S> attachedChild = copySubtree(child, this, copies);
        copyRedirects(child, attachedChild, copies);
        children.put(attachedChild.getName(), attachedChild);
        return attachedChild;
    }

    private static <S extends RCommandSource> @NotNull RCommandNode<S> copySubtree(
        @NotNull RCommandNode<S> source,
        @Nullable RCommandNode<S> parent,
        @NotNull Map<RCommandNode<S>, RCommandNode<S>> copies
    ) {
        RCommandNode<S> copy = new RCommandNode<>(source.name, parent, source.argument);
        copy.executor = source.executor;
        copy.executionDelegate = source.executionDelegate;
        copy.suggestionProvider = source.suggestionProvider;
        copy.requirement = source.requirement;
        copy.permission = source.permission;
        copy.description = source.description;
        copy.aliases.addAll(source.aliases);
        copies.put(source, copy);

        for (RCommandNode<S> child : source.children.values()) {
            copy.children.put(child.getName(), copySubtree(child, copy, copies));
        }
        return copy;
    }

    private static <S extends RCommandSource> void copyRedirects(
        @NotNull RCommandNode<S> source,
        @NotNull RCommandNode<S> copy,
        @NotNull Map<RCommandNode<S>, RCommandNode<S>> copies
    ) {
        if (source.redirect != null) {
            copy.redirect = copies.getOrDefault(source.redirect, source.redirect);
        }
        if (source.executionDelegate != null) {
            copy.executionDelegate = copies.getOrDefault(source.executionDelegate, source.executionDelegate);
        }

        for (RCommandNode<S> sourceChild : source.children.values()) {
            RCommandNode<S> copyChild = copy.children.get(sourceChild.getName());
            if (copyChild != null) {
                copyRedirects(sourceChild, copyChild, copies);
            }
        }
    }

    /**
     * Registers this node as a command root with the command service.
     *
     * @return the registered command tree
     */
    @SuppressWarnings("unchecked")
    public @NotNull RegisteredCommandTree register() {
        return register(name);
    }

    /**
     * Registers this node with a specific registration ID.
     *
     * @param registrationId the unique registration identifier
     * @return the registered command tree
     */
    @SuppressWarnings("unchecked")
    public @NotNull RegisteredCommandTree register(@NotNull String registrationId) {
        return CommandFeatures.commands().registerRoot(
            registrationId,
            (RCommandNode<RCommandSource>) this
        );
    }

    /**
     * Queues this node for registration using its own name as the registration ID.
     *
     * @return the registered command tree
     */
    @SuppressWarnings("unchecked")
    public @NotNull RegisteredCommandTree queue() {
        return queue(name);
    }

    /**
     * Queues this node for registration with a specific registration ID.
     *
     * @param registrationId the unique registration identifier
     * @return the registered command tree
     */
    @SuppressWarnings("unchecked")
    public @NotNull RegisteredCommandTree queue(@NotNull String registrationId) {
        return CommandFeatures.commands().queueRoot(
            registrationId,
            (RCommandNode<RCommandSource>) this
        );
    }
}
