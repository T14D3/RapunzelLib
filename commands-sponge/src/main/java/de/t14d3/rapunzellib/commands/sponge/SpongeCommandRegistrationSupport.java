package de.t14d3.rapunzellib.commands.sponge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.t14d3.rapunzellib.commands.CommandSourceAdapters;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.core.RCommandFailureMapper;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.registrar.tree.CommandTreeNode;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SpongeCommandRegistrationSupport {
    private final RCommandService commandService;
    private final CommandSourceAdapters adapters;

    public SpongeCommandRegistrationSupport(
        @NotNull RCommandService commandService,
        @NotNull CommandSourceAdapters adapters
    ) {
        this.commandService = Objects.requireNonNull(commandService, "commandService");
        this.adapters = Objects.requireNonNull(adapters, "adapters");
    }

    public boolean registerSharedCommands(
        @NotNull RegisterCommandEvent<Command.Raw> event,
        @NotNull PluginContainer container
    ) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(container, "container");

        List<RCommandNode<RCommandSource>> roots = commandService.roots();
        for (RCommandNode<RCommandSource> root : roots) {
            event.register(container, rawCommand(root), root.getName(), root.getAliases().toArray(String[]::new));
        }
        return !roots.isEmpty();
    }

    @NotNull Command.Raw rawCommand(@NotNull RCommandNode<RCommandSource> root) {
        Objects.requireNonNull(root, "root");
        return new BrigadierBackedRawCommand(root, adapters);
    }

    private static final class BrigadierBackedRawCommand implements Command.Raw {
        private final RCommandNode<RCommandSource> root;
        private final CommandSourceAdapters adapters;
        private final CommandDispatcher<RCommandSource> dispatcher;

        private BrigadierBackedRawCommand(
            @NotNull RCommandNode<RCommandSource> root,
            @NotNull CommandSourceAdapters adapters
        ) {
            this.root = Objects.requireNonNull(root, "root");
            this.adapters = Objects.requireNonNull(adapters, "adapters");
            this.dispatcher = buildDispatcher(root);
        }

        @Override
        public @NotNull CommandResult process(
            @NotNull CommandCause cause,
            @NotNull ArgumentReader.Mutable arguments
        ) throws CommandException {
            Objects.requireNonNull(cause, "cause");
            Objects.requireNonNull(arguments, "arguments");

            if (!canUse(cause)) {
                throw new CommandException(Component.text("You do not have permission to use this command."));
            }

            try {
                int result = dispatcher.execute(commandInput(arguments.input()), adapters.wrap(cause));
                return new SimpleCommandResult(result, result > 0, null);
            } catch (CommandSyntaxException ex) {
                throw new CommandException(RCommandFailureMapper.toSpongeMessage(ex), ex);
            }
        }

        @Override
        public @NotNull List<CommandCompletion> complete(
            @NotNull CommandCause cause,
            @NotNull ArgumentReader.Mutable arguments
        ) {
            Objects.requireNonNull(cause, "cause");
            Objects.requireNonNull(arguments, "arguments");

            if (!canUse(cause)) {
                return List.of();
            }

            ParseResults<RCommandSource> parsed = dispatcher.parse(completionInput(arguments.input()), adapters.wrap(cause));
            return dispatcher.getCompletionSuggestions(parsed).join().getList().stream()
                .map(suggestion -> (CommandCompletion) new SimpleCommandCompletion(suggestion.getText(), null))
                .toList();
        }

        @Override
        public boolean canExecute(@NotNull CommandCause cause) {
            Objects.requireNonNull(cause, "cause");
            return canUse(cause);
        }

        @Override
        public @NotNull Optional<Component> shortDescription(@NotNull CommandCause cause) {
            return description();
        }

        @Override
        public @NotNull Optional<Component> extendedDescription(@NotNull CommandCause cause) {
            return description();
        }

        @Override
        public @NotNull Component usage(@NotNull CommandCause cause) {
            return Component.text(root.getName());
        }

        @Override
        public @NotNull CommandTreeNode.Root commandTree() {
            CommandTreeNode.Root tree = rootNode();
            applyNodeMetadata(tree, root);
            for (RCommandNode<RCommandSource> child : root.getChildren()) {
                tree.child(child.getName(), buildChildTree(child));
            }
            return tree;
        }

        private boolean canUse(@NotNull CommandCause cause) {
            return canUse(root, cause);
        }

        private boolean canUse(@NotNull RCommandNode<RCommandSource> node, @NotNull CommandCause cause) {
            RCommandSource source = adapters.wrap(cause);
            if (!node.getRequirement().test(source)) {
                return false;
            }
            String permission = node.getPermission();
            return permission == null || permission.isBlank() || source.hasPermission(permission);
        }

        private @NotNull Optional<Component> description() {
            String description = root.getDescription();
            if (description == null || description.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Component.text(description));
        }

        private @NotNull String commandInput(@Nullable String input) {
            if (input == null || input.isEmpty()) {
                return root.getName();
            }
            return root.getName() + " " + input;
        }

        private @NotNull String completionInput(@Nullable String input) {
            if (input == null || input.isEmpty()) {
                return root.getName() + " ";
            }
            return root.getName() + " " + input;
        }

        private <T extends CommandTreeNode<T>> void applyNodeMetadata(
            @NotNull T treeNode,
            @NotNull RCommandNode<RCommandSource> commandNode
        ) {
            treeNode.requires(cause -> canUse(commandNode, cause));
            if (commandNode.isExecutable()) {
                treeNode.executable();
            }
        }

        private @NotNull CommandTreeNode.Basic buildChildTree(@NotNull RCommandNode<RCommandSource> node) {
            CommandTreeNode.Basic tree = literalNode();
            applyNodeMetadata(tree, node);
            for (RCommandNode<RCommandSource> child : node.getChildren()) {
                tree.child(child.getName(), buildChildTree(child));
            }
            return tree;
        }

        @SuppressWarnings("unchecked")
        private static @NotNull CommandTreeNode.Root rootNode() {
            return (CommandTreeNode.Root) CommandTreeNode.root();
        }

        @SuppressWarnings("unchecked")
        private static @NotNull CommandTreeNode.Basic literalNode() {
            return (CommandTreeNode.Basic) CommandTreeNode.literal();
        }

        private static @NotNull CommandDispatcher<RCommandSource> buildDispatcher(@NotNull RCommandNode<RCommandSource> root) {
            CommandDispatcher<RCommandSource> dispatcher = new CommandDispatcher<>();
            RCommandTree<RCommandSource> tree = new RCommandTree<>();
            tree.register(root);
            tree.attach(dispatcher);
            return dispatcher;
        }
    }

    private static final class SimpleCommandCompletion implements CommandCompletion {
        private final String completion;
        private final Component tooltip;

        private SimpleCommandCompletion(@NotNull String completion, @Nullable Component tooltip) {
            this.completion = Objects.requireNonNull(completion, "completion");
            this.tooltip = tooltip;
        }

        @Override
        public @NotNull String completion() {
            return completion;
        }

        @Override
        public @NotNull Optional<Component> tooltip() {
            return Optional.ofNullable(tooltip);
        }
    }

    private static final class SimpleCommandResult implements CommandResult {
        private final int result;
        private final boolean success;
        private final Component errorMessage;

        private SimpleCommandResult(int result, boolean success, @Nullable Component errorMessage) {
            this.result = result;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        @Override
        public boolean isSuccess() {
            return success;
        }

        @Override
        public int result() {
            return result;
        }

        @Override
        public @NotNull Optional<Component> errorMessage() {
            return Optional.ofNullable(errorMessage);
        }
    }
}
