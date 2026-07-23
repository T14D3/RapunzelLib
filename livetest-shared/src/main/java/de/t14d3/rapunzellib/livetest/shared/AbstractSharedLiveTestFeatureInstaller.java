package de.t14d3.rapunzellib.livetest.shared;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.arguments.RStringArgument;
import de.t14d3.rapunzellib.commands.core.RCommandArguments;
import de.t14d3.rapunzellib.commands.core.RCommandBuilder;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandResult;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.livetest.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for platform-specific {@link LiveTestFeatureInstaller}
 * implementations.
 * <p>
 * Provides common setup: registering a default {@link LiveTestHost} and
 * (optionally) a {@link BotService} into the context's service registry.
 * Platform subclasses override {@link #install(RapunzelContext)} to register
 * platform-specific services and call {@code super.install(context)}.
 * </p>
 * <p>
 * Subclasses should also override {@link #registerCommands(RapunzelContext)}
 * to register the {@code /livetest} command using the platform's command
 * registration API. The shared handler methods in this class can be used from
 * command executors.
 * </p>
 */
public abstract class AbstractSharedLiveTestFeatureInstaller implements LiveTestFeatureInstaller {

    /**
     * Creates the platform-specific {@link LiveTestHost} for the given context.
     *
     * @param context  the Rapunzel context
     * @param registry the test registry
     * @return the live test host
     */
    protected @NotNull LiveTestHost createHost(
            @NotNull RapunzelContext context,
            @NotNull LiveTestRegistry registry
    ) {
        return new SharedLiveTestHost(context, registry);
    }

    /**
     * Creates the platform-specific {@link BotService} for the given context.
     * <p>
     * The default implementation returns {@code null}, which means no bot
     * service is registered. Test code that calls {@link Bot#connect} will
     * throw {@link IllegalStateException} until a platform wires a service.
     * </p>
     *
     * @param context the Rapunzel context
     * @return the bot service, or null to leave bot support unregistered
     */
    protected BotService createBotService(@NotNull RapunzelContext context) {
        return null;
    }

    /**
     * Registers the {@code /livetest} command for this platform.
     * <p>
     * Called during {@link #install(RapunzelContext)} after the host and registry
     * are set up. Subclasses should override this to register the command using
     * the platform-specific command registration API. The default implementation
     * does nothing (commands will not be available until a platform module
     * registers them).
     * </p>
     *
     * @param context the Rapunzel context
     */
    protected void registerCommands(@NotNull RapunzelContext context) {
        // Default: no-op. Subclasses should register /livetest.
    }

    // ── Shared command handler logic ───────────────────────────────────────

    /**
     * Returns a description of all registered tests.
     *
     * @param context the Rapunzel context
     * @return a formatted string listing all tests, or a "no tests" message
     */
    protected static @NotNull String listTests(@NotNull RapunzelContext context) {
        LiveTestRegistry registry = context.services()
                .find(LiveTestRegistry.class).orElse(null);
        if (registry == null || registry.testCount() == 0) {
            return "No live tests registered.";
        }
        StringBuilder sb = new StringBuilder("Registered live tests (" + registry.testCount() + "):\n");
        for (LiveTest test : registry.allTests()) {
            sb.append(" - ").append(test.name()).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    /**
     * Runs a single named test asynchronously and returns a result message.
     *
     * @param name    the test name
     * @param context the Rapunzel context
     * @return a future that completes with the result description
     */
    protected static @NotNull CompletableFuture<String> runTest(
            @NotNull String name, @NotNull RapunzelContext context
    ) {
        LiveTestRegistry registry = context.services()
                .find(LiveTestRegistry.class).orElse(null);
        LiveTestHost host = context.services().find(LiveTestHost.class).orElse(null);

        if (registry == null || host == null) {
            return CompletableFuture.completedFuture("Live test system not available.");
        }
        LiveTest test = registry.find(name);
        if (test == null) {
            return CompletableFuture.completedFuture("Test not found: " + name);
        }
        return CompletableFuture.supplyAsync(() -> {
            LiveTestResult result = host.runTest(test, Duration.ofMillis(test.timeoutMs()));
            return result.format();
        });
    }

    /**
     * Runs all registered tests asynchronously and returns completion message.
     * <p>
     * Delegates to {@link SharedLiveTestHost#runAll()} when the host is a
     * {@code SharedLiveTestHost} instance, so that report files are written.
     * Otherwise falls back to a manual loop over all tests.
     * </p>
     *
     * @param context the Rapunzel context
     * @return a future that completes when all tests have been reported
     */
    protected static @NotNull CompletableFuture<String> runAllTests(@NotNull RapunzelContext context) {
        LiveTestRegistry registry = context.services()
                .find(LiveTestRegistry.class).orElse(null);
        LiveTestHost host = context.services().find(LiveTestHost.class).orElse(null);

        if (registry == null || host == null) {
            return CompletableFuture.completedFuture("Live test system not available.");
        }
        if (registry.testCount() == 0) {
            return CompletableFuture.completedFuture("No live tests registered.");
        }

        // When the host is a SharedLiveTestHost, delegate to runAll() which
        // handles report file generation after all tests complete.
        if (host instanceof SharedLiveTestHost sharedHost) {
            return CompletableFuture.supplyAsync(() -> {
                sharedHost.runAll();
                return "[LIVETEST] All tests completed.";
            });
        }

        // Fallback for custom LiveTestHost implementations
        return CompletableFuture.supplyAsync(() -> {
            for (LiveTest test : registry.allTests()) {
                LiveTestResult result = host.runTest(test, Duration.ofMillis(test.timeoutMs()));
                host.reportResult(result);
            }
            return "[LIVETEST] All tests completed.";
        });
    }

    /**
     * Creates an RLib command node for the {@code /livetest} command tree
     * with {@code list}, {@code run <name>}, and {@code runall} subcommands
     * and the {@code lt} alias.
     *
     * @param context the Rapunzel context
     * @return the root command node
     */
    protected static @NotNull RCommandNode<RCommandSource> createLivetestNode(@NotNull RapunzelContext context) {
        // /livetest list
        RCommandNode<RCommandSource> listNode = RCommandNode.<RCommandSource>literal("list")
                .executes((source, args) -> {
                    String testList = listTests(context);
                    source.sendMessage(Component.text(testList, NamedTextColor.GOLD));
                    return RCommandResult.SUCCESS;
                });

        // /livetest run <name>
        LiveTestRegistry registry = context.services()
                .find(LiveTestRegistry.class).orElse(null);
        java.util.List<String> testNames = registry != null
                ? registry.allTests().stream().map(LiveTest::name).toList()
                : java.util.Collections.emptyList();
        RStringArgument<RCommandSource> testNameArg = RStringArgument.word("name")
                .suggestions(testNames);
        RCommandNode<RCommandSource> runNode = RCommandNode.<RCommandSource>literal("run")
                .then(RCommandNode.argument(testNameArg)
                        .executes((source, args) -> {
                            args.getString("name").ifPresent(name -> {
                                source.sendMessage(Component.text("Running test: " + name, NamedTextColor.GRAY));
                                runTest(name, context).thenAccept(result -> {
                                    if (result.startsWith("[LIVETEST]")) {
                                        Rapunzel.findContext().ifPresent(ctx ->
                                                ctx.logger().info(result));
                                    } else {
                                        source.sendMessage(Component.text(result, NamedTextColor.GOLD));
                                    }
                                });
                            });
                            return RCommandResult.SUCCESS;
                        }));

        // /livetest runall
        RCommandNode<RCommandSource> runAllNode = RCommandNode.<RCommandSource>literal("runall")
                .executes((source, args) -> {
                    source.sendMessage(Component.text(
                            "Running all tests asynchronously...", NamedTextColor.GOLD));
                    runAllTests(context).thenAccept(result ->
                            source.sendMessage(Component.text(result, NamedTextColor.GOLD)));
                    return RCommandResult.SUCCESS;
                });

        // Root: /livetest with alias lt
        return RCommandNode.<RCommandSource>literal("livetest")
                .addAlias("lt")
                .then(listNode)
                .then(runNode)
                .then(runAllNode);
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");

        // Ensure a registry exists
        LiveTestRegistry registry = context.getOrCreate(
                LiveTestRegistry.class, LiveTestRegistry::new);

        // Register the host
        LiveTestHost host = createHost(context, registry);
        context.register(LiveTestHost.class, host);

        // Optionally register a bot service
        BotService botService = createBotService(context);
        if (botService != null) {
            context.registerIfAbsent(BotService.class, botService);
        }

        // Register platform-specific commands
        registerCommands(context);
    }
}