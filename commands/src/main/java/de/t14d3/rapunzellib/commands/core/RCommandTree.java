package de.t14d3.rapunzellib.commands.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.arguments.RArgument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class RCommandTree<S extends RCommandSource> {
    private final Map<String, RCommandNode<S>> roots;
    private final Map<String, RCommandNode<S>> allNodes;
    private final Map<String, List<RCommandNode<S>>> nodesByName;
    private final Map<String, List<RCommandNode<S>>> aliases;
    private CommandDispatcher<S> dispatcher;
    private boolean attached;

    public RCommandTree() {
        this.roots = new LinkedHashMap<>();
        this.allNodes = new LinkedHashMap<>();
        this.nodesByName = new LinkedHashMap<>();
        this.aliases = new LinkedHashMap<>();
    }

    public RCommandTree<S> register(@NotNull RCommandNode<S> node) {
        if (!node.isRoot()) {
            throw new IllegalArgumentException("Cannot register non-root node: " + node.getName());
        }
        if (!node.isLiteral()) {
            throw new IllegalArgumentException("Cannot register non-literal root node: " + node.getName());
        }

        registerNode(node);
        for (String aliasName : node.getAliases()) {
            aliases.computeIfAbsent(aliasName, ignored -> new ArrayList<>()).add(node);
        }

        if (attached && dispatcher != null) {
            attach(dispatcher);
        }
        return this;
    }

    private void registerNode(@NotNull RCommandNode<S> node) {
        allNodes.put(node.getPath(), node);
        nodesByName.computeIfAbsent(node.getName(), ignored -> new ArrayList<>()).add(node);
        if (node.isRoot()) {
            roots.put(node.getName(), node);
        }
        for (RCommandNode<S> child : node.getChildren()) {
            registerNode(child);
        }
    }

    public RCommandTree<S> unregister(@NotNull String nodeName) {
        RCommandNode<S> node = roots.remove(nodeName);
        if (node == null) {
            return this;
        }

        unregisterNode(node);
        for (String alias : node.getAliases()) {
            List<RCommandNode<S>> aliasNodes = aliases.get(alias);
            if (aliasNodes == null) {
                continue;
            }
            aliasNodes.remove(node);
            if (aliasNodes.isEmpty()) {
                aliases.remove(alias);
            }
        }
        return this;
    }

    private void unregisterNode(@NotNull RCommandNode<S> node) {
        allNodes.remove(node.getPath());
        List<RCommandNode<S>> sameNameNodes = nodesByName.get(node.getName());
        if (sameNameNodes != null) {
            sameNameNodes.remove(node);
            if (sameNameNodes.isEmpty()) {
                nodesByName.remove(node.getName());
            }
        }
        for (RCommandNode<S> child : node.getChildren()) {
            unregisterNode(child);
        }
    }

    public @Nullable RCommandNode<S> getRoot(@NotNull String nodeName) {
        return roots.get(nodeName);
    }

    public @NotNull Optional<RCommandNode<S>> getNode(@NotNull String nodeName) {
        return Optional.ofNullable(nodesByName.getOrDefault(nodeName, List.of()).stream().findFirst().orElse(null));
    }

    public @NotNull List<RCommandNode<S>> resolveAlias(@NotNull String aliasName) {
        List<RCommandNode<S>> nodes = aliases.get(aliasName);
        return nodes != null ? Collections.unmodifiableList(nodes) : List.of();
    }

    public @NotNull Collection<RCommandNode<S>> getRoots() {
        return Collections.unmodifiableCollection(roots.values());
    }

    public @NotNull Collection<RCommandNode<S>> getAllNodes() {
        return Collections.unmodifiableCollection(allNodes.values());
    }

    public boolean hasCommand(@NotNull String nodeName) {
        return nodesByName.containsKey(nodeName);
    }

    public RCommandTree<S> attach(@NotNull CommandDispatcher<S> dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        this.dispatcher = dispatcher;
        attachMapped(dispatcher, Function.identity());
        this.attached = true;
        return this;
    }

    public <N> RCommandTree<S> attachMapped(
        @NotNull CommandDispatcher<N> dispatcher,
        @NotNull Function<? super N, ? extends S> sourceMapper
    ) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        Objects.requireNonNull(sourceMapper, "sourceMapper");

        for (RCommandNode<S> root : roots.values()) {
            dispatcher.getRoot().addChild(buildRootMapped(root, dispatcher, sourceMapper));
        }
        return this;
    }

    public <N> @NotNull LiteralCommandNode<N> buildRootMapped(
        @NotNull RCommandNode<S> root,
        @NotNull CommandDispatcher<N> dispatcher,
        @NotNull Function<? super N, ? extends S> sourceMapper
    ) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(dispatcher, "dispatcher");
        Objects.requireNonNull(sourceMapper, "sourceMapper");

        if (!root.isRoot()) {
            throw new IllegalArgumentException("Cannot build non-root node: " + root.getPath());
        }
        if (!root.isLiteral()) {
            throw new IllegalArgumentException("Cannot build non-literal root node: " + root.getPath());
        }
        if (roots.get(root.getName()) != root) {
            throw new IllegalArgumentException("Command root is not registered in this tree: " + root.getName());
        }

        return (LiteralCommandNode<N>) buildNode(root, dispatcher, sourceMapper);
    }

    public RCommandTree<S> detach() {
        attached = false;
        return this;
    }

    public boolean isAttached() {
        return attached;
    }

    public @Nullable CommandDispatcher<S> getDispatcher() {
        return dispatcher;
    }

    private <N> @NotNull CommandNode<N> buildNode(
        @NotNull RCommandNode<S> node,
        @NotNull CommandDispatcher<N> dispatcher,
        @NotNull Function<? super N, ? extends S> sourceMapper
    ) {
        ArgumentBuilder<N, ?> builder = createBuilder(node);
        configureBuilder(builder, node, dispatcher, sourceMapper);
        for (RCommandNode<S> child : node.getChildren()) {
            builder.then(buildNode(child, dispatcher, sourceMapper));
        }
        return builder.build();
    }

    private <N> @NotNull ArgumentBuilder<N, ?> createBuilder(@NotNull RCommandNode<S> node) {
        if (node.isLiteral()) {
            return LiteralArgumentBuilder.literal(node.getName());
        }

        @SuppressWarnings("unchecked")
        RArgument<S, Object> argument = (RArgument<S, Object>) Objects.requireNonNull(node.getArgument(), "argument");
        return RequiredArgumentBuilder.argument(node.getName(), argument.getArgumentType());
    }

    private <N> void configureBuilder(
        @NotNull ArgumentBuilder<N, ?> builder,
        @NotNull RCommandNode<S> node,
        @NotNull CommandDispatcher<N> dispatcher,
        @NotNull Function<? super N, ? extends S> sourceMapper
    ) {
        builder.requires(createRequirement(node, sourceMapper));
        RCommandNode<S> executionNode = resolveExecutionNode(node);
        if (executionNode != null) {
            builder.executes(context -> execute(executionNode, node, context, sourceMapper));
        }
        if (node.isArgument() && builder instanceof RequiredArgumentBuilder<?, ?>) {
            @SuppressWarnings("unchecked")
            RequiredArgumentBuilder<N, ?> argumentBuilder = (RequiredArgumentBuilder<N, ?>) builder;
            configureSuggestions(argumentBuilder, node, sourceMapper);
        }

        RCommandNode<S> redirect = node.getRedirect();
        if (redirect != null && redirect.isRoot()) {
            CommandNode<N> target = dispatcher.getRoot().getChild(redirect.getName());
            if (target != null) {
                builder.redirect(target);
            }
        }
    }

    private <N> int execute(
        @NotNull RCommandNode<S> executionNode,
        @NotNull RCommandNode<S> contextNode,
        @NotNull CommandContext<N> context,
        @NotNull Function<? super N, ? extends S> sourceMapper
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        S mappedSource = sourceMapper.apply(context.getSource());
        RCommandArguments<S> args = new RCommandArguments<>(mappedSource, context, contextNode);
        try {
            return Objects.requireNonNull(executionNode.getExecutor(), "executor").execute(mappedSource, args);
        } catch (RCommandException exception) {
            throw RCommandFailureMapper.toSyntaxException(exception);
        }
    }

    private @Nullable RCommandNode<S> resolveExecutionNode(@NotNull RCommandNode<S> node) {
        RCommandNode<S> current = node;
        while (current != null) {
            if (current.isExecutable()) {
                return current;
            }
            RCommandNode<S> delegate = current.getExecutionDelegate();
            if (delegate == current) {
                break;
            }
            current = delegate;
        }
        return null;
    }

    private <N> void configureSuggestions(
        @NotNull RequiredArgumentBuilder<N, ?> builder,
        @NotNull RCommandNode<S> node,
        @NotNull Function<? super N, ? extends S> sourceMapper
    ) {
        builder.suggests((context, suggestionsBuilder) -> {
            S mappedSource = sourceMapper.apply(context.getSource());
            RCommandArguments<S> info = new RCommandArguments<>(mappedSource, context, node, suggestionsBuilder);
            RCommandSuggestionProvider<S> suggestionProvider = node.getSuggestionProvider();
            if (suggestionProvider != null) {
                return suggestionProvider.suggest(mappedSource, info);
            }

            @SuppressWarnings("unchecked")
            RArgument<S, Object> argument = (RArgument<S, Object>) Objects.requireNonNull(node.getArgument(), "argument");
            return argument.listSuggestions(mappedSource, info);
        });
    }

    private @NotNull Predicate<S> createRequirement(@NotNull RCommandNode<S> node) {
        Predicate<S> requirement = node.getRequirement();
        String permission = node.getPermission();
        if (permission == null || permission.isBlank()) {
            return requirement;
        }
        return source -> requirement.test(source) && source.hasPermission(permission);
    }

    private <N> @NotNull Predicate<N> createRequirement(
        @NotNull RCommandNode<S> node,
        @NotNull Function<? super N, ? extends S> sourceMapper
    ) {
        Predicate<S> requirement = createRequirement(node);
        return source -> requirement.test(sourceMapper.apply(source));
    }
}
