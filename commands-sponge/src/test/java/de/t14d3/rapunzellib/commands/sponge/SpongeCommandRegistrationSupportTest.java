package de.t14d3.rapunzellib.commands.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.CommandSourceAdapter;
import de.t14d3.rapunzellib.commands.CommandSourceAdapters;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.RegisteredCommandTree;
import de.t14d3.rapunzellib.commands.core.RCommandException;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandResult;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import de.t14d3.rapunzellib.commands.arguments.RStringArgument;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpongeCommandRegistrationSupportTest {
    @Test
    void registersSpongeRawCommandsBackedBySharedBrigadierTree() throws Exception {
        AtomicReference<RCommandSource> executedSource = new AtomicReference<>();
        RecordingCommandService commandService = new RecordingCommandService();
        RCommandNode<RCommandSource> root = RCommandNode.<RCommandSource>literal("zones")
            .setDescription("Manage zones")
            .addAlias("zone")
            .requiresPermission("zones.use");
        root.then("list").executes((source, args) -> {
            executedSource.set(source);
            return RCommandResult.SUCCESS;
        });
        commandService.registerRoot(root);

        CommandSourceAdapters adapters = new CommandSourceAdapters(PlatformId.SPONGE, List.of(new CommandSourceAdapter() {
            @Override
            public @NotNull PlatformId platformId() {
                return PlatformId.SPONGE;
            }

            @Override
            public boolean supports(@NotNull Object source) {
                return source instanceof CommandCause;
            }

            @Override
            public @NotNull RCommandSource wrap(@NotNull Object source) {
                return SpongeCommandSourceAdapter.wrap((CommandCause) source);
            }
        }));

        SpongeCommandRegistrationSupport support = new SpongeCommandRegistrationSupport(commandService, adapters);
        List<Registration> registrations = new ArrayList<>();

        boolean registered = support.registerSharedCommands(registerCommandEvent(registrations), pluginContainer());

        assertTrue(registered);
        assertEquals(1, registrations.size());
        assertEquals(new Registration("zones", List.of("zone"), registrations.getFirst().command()), registrations.getFirst());

        Command.Raw command = registrations.getFirst().command();
        Audience audience = audience();
        CommandCause permittedCause = commandCause(audience, Set.of("zones.use"));

        assertTrue(command.canExecute(permittedCause));
        assertFalse(command.canExecute(commandCause(audience, Set.of())));
        assertEquals(Optional.of(Component.text("Manage zones")), command.shortDescription(permittedCause));

        List<String> completions = command.complete(permittedCause, argumentReader("l")).stream()
            .map(CommandCompletion::completion)
            .toList();
        assertEquals(List.of("list"), completions);

        CommandResult result = command.process(permittedCause, argumentReader("list"));
        assertTrue(result.isSuccess());
        assertEquals(RCommandResult.SUCCESS, result.result());
        assertSame(permittedCause, ((RNativeHandle<?>) executedSource.get()).handle());
        assertSame(audience, executedSource.get().audience());
    }

    @Test
    void surfacesSharedCommandFailuresThroughSpongeCommandExceptions() {
        RecordingCommandService commandService = new RecordingCommandService();
        commandService.registerRoot(RCommandNode.<RCommandSource>literal("zones").executes((source, args) -> {
            throw new RCommandException("Zone is required");
        }));

        CommandSourceAdapters adapters = new CommandSourceAdapters(PlatformId.SPONGE, List.of(new CommandSourceAdapter() {
            @Override
            public @NotNull PlatformId platformId() {
                return PlatformId.SPONGE;
            }

            @Override
            public boolean supports(@NotNull Object source) {
                return source instanceof CommandCause;
            }

            @Override
            public @NotNull RCommandSource wrap(@NotNull Object source) {
                return SpongeCommandSourceAdapter.wrap((CommandCause) source);
            }
        }));

        SpongeCommandRegistrationSupport support = new SpongeCommandRegistrationSupport(commandService, adapters);
        List<Registration> registrations = new ArrayList<>();
        support.registerSharedCommands(registerCommandEvent(registrations), pluginContainer());

        Command.Raw command = registrations.getFirst().command();
        CommandException exception = assertThrows(
            CommandException.class,
            () -> command.process(commandCause(audience(), Set.of()), argumentReader(""))
        );

        assertEquals(Component.text("Zone is required"), exception.componentMessage());
    }

    @Test
    void completesCustomArgumentSuggestionsThroughSpongeRawCommandBridge() throws CommandException {
        AtomicReference<List<String>> previousArguments = new AtomicReference<>();
        RecordingCommandService commandService = new RecordingCommandService();
        RCommandNode<RCommandSource> root = RCommandNode.<RCommandSource>literal("zones")
            .requiresPermission("zones.use");
        RCommandNode<RCommandSource> region = root.then(RStringArgument.word("region"));
        region.then(RStringArgument.word("action")).suggests((source, info) -> {
            previousArguments.set(info.getPreviousArguments());
            return info.suggestMatching(List.of("alpha", "albert", "beta"));
        });
        commandService.registerRoot(root);

        CommandSourceAdapters adapters = new CommandSourceAdapters(PlatformId.SPONGE, List.of(new CommandSourceAdapter() {
            @Override
            public @NotNull PlatformId platformId() {
                return PlatformId.SPONGE;
            }

            @Override
            public boolean supports(@NotNull Object source) {
                return source instanceof CommandCause;
            }

            @Override
            public @NotNull RCommandSource wrap(@NotNull Object source) {
                return SpongeCommandSourceAdapter.wrap((CommandCause) source);
            }
        }));

        SpongeCommandRegistrationSupport support = new SpongeCommandRegistrationSupport(commandService, adapters);
        List<Registration> registrations = new ArrayList<>();
        support.registerSharedCommands(registerCommandEvent(registrations), pluginContainer());

        Command.Raw command = registrations.getFirst().command();
        List<String> completions = command.complete(commandCause(audience(), Set.of("zones.use")), argumentReader("spawn a")).stream()
            .map(CommandCompletion::completion)
            .toList();

        assertEquals(List.of("albert", "alpha"), completions);
        assertEquals(List.of("spawn"), previousArguments.get());
    }

    private static RegisterCommandEvent<Command.Raw> registerCommandEvent(List<Registration> registrations) {
        return proxy(RegisterCommandEvent.class, (proxy, method, args) -> switch (method.getName()) {
            case "register" -> {
                registrations.add(new Registration(
                    (String) args[2],
                    List.of((String[]) args[3]),
                    (Command.Raw) args[1]
                ));
                yield registerResult();
            }
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "test-register-command-event";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RegisterCommandEvent.Result<Command.Raw> registerResult() {
        return proxy(RegisterCommandEvent.Result.class, (proxy, method, args) -> switch (method.getName()) {
            case "register" -> proxy;
            case "mapping" -> null;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "test-register-command-result";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static PluginContainer pluginContainer() {
        return proxy(PluginContainer.class, (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "test-plugin-container";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static Audience audience() {
        return proxy(Audience.class, (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "test-audience";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static CommandCause commandCause(Audience audience, Set<String> permissions) {
        return proxy(CommandCause.class, (proxy, method, args) -> switch (method.getName()) {
            case "audience" -> audience;
            case "hasPermission" -> permissions.contains((String) args[0]);
            case "first" -> Optional.empty();
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "test-command-cause";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ArgumentReader.Mutable argumentReader(String input) {
        return proxy(ArgumentReader.Mutable.class, (proxy, method, args) -> switch (method.getName()) {
            case "input", "remaining" -> input;
            case "parsed" -> "";
            case "canRead" -> !input.isEmpty();
            case "remainingLength", "totalLength" -> input.length();
            case "cursor" -> 0;
            case "immutable" -> proxy(ArgumentReader.Immutable.class, (immutableProxy, immutableMethod, immutableArgs) -> switch (immutableMethod.getName()) {
                case "input", "remaining" -> input;
                case "parsed" -> "";
                case "canRead" -> !input.isEmpty();
                case "remainingLength", "totalLength" -> input.length();
                case "cursor" -> 0;
                case "mutable" -> proxy;
                case "hashCode" -> System.identityHashCode(immutableProxy);
                case "equals" -> immutableProxy == immutableArgs[0];
                case "toString" -> "test-immutable-argument-reader";
                default -> defaultValue(immutableMethod.getReturnType());
            });
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "test-argument-reader";
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private record Registration(String alias, List<String> aliases, Command.Raw command) {
    }

    private static final class RecordingCommandService implements RCommandService {
        private final List<RegisteredCommandTree> registrations = new ArrayList<>();
        private final List<RCommandNode<RCommandSource>> roots = new ArrayList<>();

        @Override
        public @NotNull PlatformId platformId() {
            return PlatformId.SPONGE;
        }

        @Override
        public @NotNull RegisteredCommandTree registerRoot(@NotNull RCommandNode<RCommandSource> root) {
            return registerRoot(root.getName(), root);
        }

        @Override
        public @NotNull RegisteredCommandTree registerRoot(@NotNull String registrationId, @NotNull RCommandNode<RCommandSource> root) {
            RCommandTree<RCommandSource> tree = new RCommandTree<>();
            tree.register(root);
            RegisteredCommandTree registration = new RegisteredCommandTree(registrationId, tree, List.of(root));
            registrations.add(registration);
            roots.add(root);
            return registration;
        }

        @Override
        public @NotNull RegisteredCommandTree queueRoot(@NotNull RCommandNode<RCommandSource> root) {
            return registerRoot(root);
        }

        @Override
        public @NotNull RegisteredCommandTree queueRoot(@NotNull String registrationId, @NotNull RCommandNode<RCommandSource> root) {
            return registerRoot(registrationId, root);
        }

        @Override
        public @NotNull RegisteredCommandTree registerTree(@NotNull String registrationId, @NotNull RCommandTree<RCommandSource> tree) {
            List<RCommandNode<RCommandSource>> treeRoots = List.copyOf(tree.getRoots());
            RegisteredCommandTree registration = new RegisteredCommandTree(registrationId, tree, treeRoots);
            registrations.add(registration);
            roots.addAll(treeRoots);
            return registration;
        }

        @Override
        public @NotNull RegisteredCommandTree queueTree(@NotNull String registrationId, @NotNull RCommandTree<RCommandSource> tree) {
            return registerTree(registrationId, tree);
        }

        @Override
        public boolean unregister(@NotNull String registrationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean queueUnregister(@NotNull String registrationId) {
            return unregister(registrationId);
        }

        @Override
        public boolean hasQueuedChanges() {
            return false;
        }

        @Override
        public boolean flush() {
            return false;
        }

        @Override
        public @NotNull Optional<RegisteredCommandTree> find(@NotNull String registrationId) {
            return registrations.stream().filter(registration -> registration.registrationId().equals(registrationId)).findFirst();
        }

        @Override
        public @NotNull Optional<RCommandNode<RCommandSource>> findRoot(@NotNull String rootName) {
            return roots.stream().filter(root -> root.getName().equals(rootName)).findFirst();
        }

        @Override
        public @NotNull List<RegisteredCommandTree> registrations() {
            return List.copyOf(registrations);
        }

        @Override
        public @NotNull List<RCommandNode<RCommandSource>> roots() {
            return List.copyOf(roots);
        }

        @Override
        public @NotNull RCommandTree<RCommandSource> sharedTree() {
            RCommandTree<RCommandSource> tree = new RCommandTree<>();
            roots.forEach(tree::register);
            return tree;
        }
    }
}
