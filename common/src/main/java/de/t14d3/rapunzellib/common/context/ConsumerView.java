package de.t14d3.rapunzellib.common.context;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.AttachmentSupport;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import de.t14d3.rapunzellib.common.message.YamlMessageFormatService;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.interop.RNativeInterop;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RapunzelRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A per-consumer view over a shared platform {@link RapunzelContext}.
 *
 * <p>The platform plugin (e.g. RapunzelLibPaper) owns the canonical context
 * with all platform services registered by {@code PlatformFeatures.install()}.
 * Each consumer plugin gets a {@code ConsumerView} that:</p>
 *
 * <ul>
 *   <li>Has its own {@link ServiceRegistry} for consumer-specific services</li>
 *   <li>Delegates all platform accessors ({@link #players()}, {@link #blocks()},
 *       {@link #worlds()}, …) to the shared context when the consumer hasn't
 *       registered a local override</li>
 *   <li>Provides the consumer's own {@link Logger}, {@link Path data directory}
 *       and {@link ResourceProvider}</li>
 *   <li>Tracks consumer-registered closeables independently from the shared context</li>
 *   <li>{@link #close()} releases only consumer resources - the shared context
 *       stays alive for other consumers and the platform plugin</li>
 * </ul>
 */
public final class ConsumerView implements RapunzelContext {

    private final RapunzelContext shared;
    private final Logger consumerLogger;
    private final Path consumerDataDir;
    private final ResourceProvider consumerResources;
    private final LifecycleOwner consumerOwner;
    private final List<AutoCloseable> closeables = new ArrayList<>();

    /**
     * The consumer's own config and message services.
     * <p>
     * These must be scoped to the consumer, not to the shared platform
     * context: {@link ConfigService} resolves bundled default resources
     * through the consumer's {@link ResourceProvider} (i.e. the consumer
     * plugin's jar), and {@link MessageFormatService} must read and write
     * {@code messages.yml} in the consumer's own data directory. Delegating
     * to the shared services would look for defaults inside the platform
     * jar and put {@code messages.yml} in the platform plugin's folder.
     * </p>
     */
    private final ConfigService consumerConfigs;
    private final MessageFormatService consumerMessages;

    public ConsumerView(
        @NotNull RapunzelContext shared,
        @NotNull Logger logger,
        @NotNull Path dataDirectory,
        @NotNull ResourceProvider resources,
        @NotNull LifecycleOwner owner
    ) {
        this.shared = shared;
        this.consumerLogger = logger;
        this.consumerDataDir = dataDirectory;
        this.consumerResources = resources;
        this.consumerOwner = owner;
        this.consumerConfigs = new SnakeYamlConfigService(resources, logger);
        this.consumerMessages = new YamlMessageFormatService(
            consumerConfigs, logger, dataDirectory.resolve("messages.yml"), "messages.yml"
        );
    }

    // -- Shared runtime (global) --------------------------------------------------

    @Override
    public @NotNull RapunzelRuntime sharedRuntime() {
        return shared.sharedRuntime();
    }

    // -- Platform runtime ---------------------------------------------------------

    @Override
    public @NotNull PlatformRuntime runtime() {
        return shared.runtime();
    }

    @Override
    public @NotNull PlatformId platformId() {
        return shared.platformId();
    }

    // -- Consumer-specific resources ----------------------------------------------

    @Override
    public @NotNull Logger logger() {
        return consumerLogger;
    }

    @Override
    public @NotNull Path dataDirectory() {
        return consumerDataDir;
    }

    @Override
    public @NotNull ResourceProvider resources() {
        return consumerResources;
    }

    @Override
    public @NotNull LifecycleOwner owner() {
        return consumerOwner;
    }

    // -- Scheduler ----------------------------------------------------------------

    @Override
    public @NotNull Scheduler scheduler() {
        return shared.scheduler();
    }

    // -- Service registry (shared with platform) -----------------------------------

    @Override
    public @NotNull ServiceRegistry services() {
        return shared.services();
    }

    // -- Accessors (all delegate to shared - consumer-local services go into the
    //    same service registry, tracked via registerCloseable for cleanup) ---------

    @Override
    public @NotNull RRegistryAccess registries() {
        return shared.registries();
    }

    @Override
    public @NotNull ConfigService configs() {
        return consumerConfigs;
    }

    @Override
    public @NotNull MessageFormatService messages() {
        return consumerMessages;
    }

    @Override
    public @NotNull Players players() {
        return shared.players();
    }

    @Override
    public @NotNull Entities entities() {
        return shared.entities();
    }

    @Override
    public @NotNull Worlds worlds() {
        return shared.worlds();
    }

    @Override
    public @NotNull Blocks blocks() {
        return shared.blocks();
    }

    @Override
    public @NotNull AttachmentSupport attachments() {
        return shared.attachments();
    }

    @Override
    public @NotNull Optional<RNativeInterop> nativeInterop() {
        return shared.nativeInterop();
    }

    // -- Type registries (always global) ------------------------------------------

    @Override
    public @NotNull REntityTypeRegistry entityTypes() {
        return shared.entityTypes();
    }

    @Override
    public @NotNull RItemTypeRegistry itemTypes() {
        return shared.itemTypes();
    }

    @Override
    public @NotNull RBlockTypeRegistry blockTypes() {
        return shared.blockTypes();
    }

    // -- Lifecycle owner helpers --------------------------------------------------

    @Override
    public @NotNull LifecycleOwner lifecycleOwner() {
        return consumerOwner;
    }

    @Override
    public @NotNull <T> Optional<T> owner(@NotNull Class<T> type) {
        return consumerOwner.as(type);
    }

    @Override
    public @NotNull <T> T requireOwner(@NotNull Class<T> type) {
        return consumerOwner.require(type);
    }

    // -- Service registration (consumer-local) ------------------------------------

    @Override
    public @NotNull <T> T register(@NotNull Class<T> type, @NotNull T instance) {
        RapunzelContext.super.register(type, instance);
        return instance;
    }

    @Override
    public @NotNull <T> T registerLinked(
        @NotNull Class<T> primaryType, @NotNull T instance, @NotNull Class<?>... linkedTypes
    ) {
        RapunzelContext.super.registerLinked(primaryType, instance, linkedTypes);
        return instance;
    }

    @Override
    public @NotNull <T> T registerIfAbsent(@NotNull Class<T> type, @NotNull T instance) {
        return RapunzelContext.super.registerIfAbsent(type, instance);
    }

    @Override
    public @NotNull <T> T registerIfAbsent(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return RapunzelContext.super.registerIfAbsent(type, supplier);
    }

    @Override
    public @NotNull <T> T getOrCreate(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return RapunzelContext.super.getOrCreate(type, supplier);
    }

    @Override
    public void registerCloseable(@NotNull AutoCloseable closeable) {
        closeables.add(closeable);
    }

    // -- Capabilities -------------------------------------------------------------

    @Override
    public boolean supports(@NotNull RuntimeCapability capability) {
        return shared.supports(capability);
    }

    @Override
    public void requireCapability(@NotNull RuntimeCapability capability) {
        shared.requireCapability(capability);
    }

    @Override
    public void requireCapability(@NotNull RuntimeCapability capability, @NotNull String useCase) {
        shared.requireCapability(capability, useCase);
    }

    // -- Attachments --------------------------------------------------------------

    @Override
    public boolean supportsAttachments(@NotNull RNative target) {
        return attachments().supports(target);
    }

    @Override
    public @NotNull <T extends RNative> T requireAttachmentSupport(@NotNull T target) {
        return attachments().requireSupported(target);
    }

    @Override
    public @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
        return attachments().attachments(target);
    }

    // -- Command dispatch ---------------------------------------------------------

    @Override
    public void dispatchCommand(@NotNull String command) {
        scheduler().run(() -> services().get(ConsoleCommandDispatcher.class).dispatch(command));
    }

    // -- Lifecycle ----------------------------------------------------------------

    /**
     * Closes consumer-registered closeables and releases local resources.
     * The shared platform context is NOT closed.
     */
    @Override
    public void close() throws Exception {
        List<AutoCloseable> toClose;
        synchronized (closeables) {
            toClose = new ArrayList<>(closeables);
            closeables.clear();
        }
        // Walk in reverse registration order, collecting exceptions
        Exception first = null;
        for (int i = toClose.size() - 1; i >= 0; i--) {
            try {
                toClose.get(i).close();
            } catch (Exception e) {
                if (first == null) {
                    first = e;
                } else {
                    first.addSuppressed(e);
                }
            }
        }
        if (first != null) throw first;
        // shared context is NOT closed
    }
}
