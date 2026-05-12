package de.t14d3.rapunzellib.common.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.context.DefaultRapunzelContext;
import de.t14d3.rapunzellib.common.message.YamlMessageFormatService;
import de.t14d3.rapunzellib.common.objects.interop.DefaultRNativeInterop;
import de.t14d3.rapunzellib.common.registry.DefaultRRegistryAccess;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedBlockTypeRegistry;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedEntityTypeRegistry;
import de.t14d3.rapunzellib.common.registry.RegistryAccessBackedItemTypeRegistry;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.objects.interop.RNativeInterop;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.runtime.RuntimeProfile;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Static bootstrap utility for wiring the default RapunzelLib service implementations.
 * <p>
 * Provides factory methods for creating platform runtimes, the application context,
 * and registering all standard services (config, messaging, native interop, registries,
 * player/entity/world accessors).
 * <p>
 * Designed for use in platform adapter initialisation (Bukkit, Bungee, Velocity, etc.)
 * during plugin startup. Organised into phases for extensibility.
 */
public final class BootstrapServices {
    private BootstrapServices() {
    }

    /**
     * Holds the result of the first bootstrap phase.
     *
     * @param context       the created application context
     * @param configService the registered config service
     */
    public record FirstPhaseResult(RapunzelContext context, ConfigService configService) {
        public FirstPhaseResult {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(configService, "configService");
        }
    }

    /**
     * Runs the first bootstrap phase: creates the context, registers native interop,
     * YAML config, and YAML-based message formatting.
     *
     * @param runtime   the platform runtime
     * @param logger    the logger
     * @param dataDir   the plugin data directory
     * @param resources the resource provider
     * @param scheduler the platform scheduler
     * @return the first phase result containing context and config service
     */
    public static FirstPhaseResult bootstrapFirstPhase(
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler
    ) {
        RapunzelContext context = createContext(runtime, logger, dataDir, resources, scheduler);
        registerNativeInterop(context);
        ConfigService configService = registerYamlConfig(context, resources, logger);
        registerYamlMessages(context, configService, logger, dataDir);
        return new FirstPhaseResult(context, configService);
    }

    /**
     * Creates a {@link PlatformRuntime} for a server with a profile.
     *
     * @param platformId     the platform identifier
     * @param engineFamily   the engine family (e.g. PAPER, SPONGE)
     * @param lifecycleOwner the lifecycle owner object
     * @param runtimeProfile the runtime profile
     * @return a new platform runtime
     */
    public static @NotNull PlatformRuntime serverRuntime(
        PlatformId platformId,
        EngineFamily engineFamily,
        Object lifecycleOwner,
        RuntimeProfile runtimeProfile
    ) {
        return runtime(platformId, RuntimeRole.SERVER, engineFamily, lifecycleOwner, runtimeProfile);
    }

    /**
     * Creates a {@link PlatformRuntime} for a server with capabilities.
     *
     * @param platformId     the platform identifier
     * @param engineFamily   the engine family
     * @param lifecycleOwner the lifecycle owner object
     * @param capabilities   the runtime capabilities
     * @return a new platform runtime
     */
    public static @NotNull PlatformRuntime serverRuntime(
        PlatformId platformId,
        EngineFamily engineFamily,
        Object lifecycleOwner,
        RuntimeCapability... capabilities
    ) {
        return runtime(platformId, RuntimeRole.SERVER, engineFamily, lifecycleOwner, capabilities);
    }

    /**
     * Creates a {@link PlatformRuntime} for a proxy with a profile.
     *
     * @param platformId     the platform identifier
     * @param lifecycleOwner the lifecycle owner object
     * @param runtimeProfile the runtime profile
     * @return a new platform runtime
     */
    public static @NotNull PlatformRuntime proxyRuntime(
        PlatformId platformId,
        Object lifecycleOwner,
        RuntimeProfile runtimeProfile
    ) {
        return runtime(platformId, RuntimeRole.PROXY, EngineFamily.PROXY, lifecycleOwner, runtimeProfile);
    }

    /**
     * Creates a {@link PlatformRuntime} for a proxy with capabilities.
     *
     * @param platformId     the platform identifier
     * @param lifecycleOwner the lifecycle owner object
     * @param capabilities   the runtime capabilities
     * @return a new platform runtime
     */
    public static @NotNull PlatformRuntime proxyRuntime(
        PlatformId platformId,
        Object lifecycleOwner,
        RuntimeCapability... capabilities
    ) {
        return runtime(platformId, RuntimeRole.PROXY, EngineFamily.PROXY, lifecycleOwner, capabilities);
    }

    /**
     * Creates a {@link PlatformRuntime} from explicit role, family, and profile.
     *
     * @param platformId     the platform identifier
     * @param role           the runtime role (SERVER, PROXY)
     * @param engineFamily   the engine family
     * @param lifecycleOwner the lifecycle owner object
     * @param runtimeProfile the runtime profile
     * @return a new platform runtime
     */
    public static @NotNull PlatformRuntime runtime(
        PlatformId platformId,
        RuntimeRole role,
        EngineFamily engineFamily,
        Object lifecycleOwner,
        RuntimeProfile runtimeProfile
    ) {
        Objects.requireNonNull(runtimeProfile, "runtimeProfile");
        return new PlatformRuntime(
            Objects.requireNonNull(platformId, "platformId"),
            Objects.requireNonNull(role, "role"),
            Objects.requireNonNull(engineFamily, "engineFamily"),
            runtimeProfile.capabilities(),
            new LifecycleOwner(Objects.requireNonNull(lifecycleOwner, "lifecycleOwner"))
        );
    }

    /**
     * Creates a {@link PlatformRuntime} from explicit role, family, and capabilities.
     *
     * @param platformId     the platform identifier
     * @param role           the runtime role
     * @param engineFamily   the engine family
     * @param lifecycleOwner the lifecycle owner object
     * @param capabilities   the runtime capabilities
     * @return a new platform runtime
     */
    public static @NotNull PlatformRuntime runtime(
        PlatformId platformId,
        RuntimeRole role,
        EngineFamily engineFamily,
        Object lifecycleOwner,
        RuntimeCapability... capabilities
    ) {
        return runtime(platformId, role, engineFamily, lifecycleOwner, profileOf(capabilities));
    }

    /**
     * Creates a {@link RuntimeProfile} from the given capabilities.
     *
     * @param capabilities the runtime capabilities
     * @return a new runtime profile
     */
    public static @NotNull RuntimeProfile profileOf(@NotNull RuntimeCapability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");

        EnumSet<RuntimeCapability> capabilitySet = EnumSet.noneOf(RuntimeCapability.class);
        for (RuntimeCapability capability : capabilities) {
            capabilitySet.add(Objects.requireNonNull(capability, "capability"));
        }
        return RuntimeProfile.of(capabilitySet);
    }

    /**
     * Creates the default application context.
     *
     * @param runtime   the platform runtime
     * @param logger    the logger
     * @param dataDir   the plugin data directory
     * @param resources the resource provider
     * @param scheduler the platform scheduler
     * @return a new {@link DefaultRapunzelContext}
     */
    public static RapunzelContext createContext(
        PlatformRuntime runtime,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler
    ) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataDir, "dataDir");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(scheduler, "scheduler");

        return new DefaultRapunzelContext(runtime, logger, dataDir, resources, scheduler);
    }

    /**
     * Registers the default native interop service in the context.
     *
     * @param context the application context
     * @return the created interop instance
     */
    public static DefaultRNativeInterop registerNativeInterop(RapunzelContext context) {
        Objects.requireNonNull(context, "context");

        DefaultRNativeInterop interop = context.sharedRuntime().getOrCreate(
            DefaultRNativeInterop.class,
            () -> new DefaultRNativeInterop(context.platformId())
        );
        return context.registerLinked(
            DefaultRNativeInterop.class,
            interop,
            RNativeInterop.class,
            MutableRNativeInterop.class
        );
    }

    /**
     * Registers the SnakeYAML-based config service in the context.
     *
     * @param ctx       the application context
     * @param resources the resource provider
     * @param logger    the logger
     * @return the registered config service
     */
    public static ConfigService registerYamlConfig(RapunzelContext ctx, ResourceProvider resources, Logger logger) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(logger, "logger");

        ConfigService configService = new SnakeYamlConfigService(resources, logger);
        ctx.register(ConfigService.class, configService);
        return configService;
    }

    /**
     * Registers player accessors in the context.
     *
     * @param context     the application context
     * @param players     the players accessor
     * @param playersType the concrete players accessor type
     * @param <P>         the players accessor type
     */
    public static <P extends Players> void registerPlayerAccessors(
        RapunzelContext context,
        P players,
        Class<P> playersType
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(playersType, "playersType");

        P sharedPlayers = context.sharedRuntime().getOrCreate(playersType, () -> players);
        context.registerLinked(playersType, sharedPlayers, Players.class);
    }

    /**
     * Registers entity accessors in the context.
     *
     * @param context      the application context
     * @param entities     the entities accessor
     * @param entitiesType the concrete entities accessor type
     * @param <E>          the entities accessor type
     */
    public static <E extends Entities> void registerEntityAccessors(
        RapunzelContext context,
        E entities,
        Class<E> entitiesType
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(entitiesType, "entitiesType");

        E sharedEntities = context.sharedRuntime().getOrCreate(entitiesType, () -> entities);
        context.registerLinked(entitiesType, sharedEntities, Entities.class);
    }

    /**
     * Registers world and block accessors in the context.
     *
     * @param context    the application context
     * @param worlds     the worlds accessor
     * @param worldsType the concrete worlds accessor type
     * @param blocks     the blocks accessor
     * @param blocksType the concrete blocks accessor type
     * @param <W>        the worlds accessor type
     * @param <B>        the blocks accessor type
     */
    public static <W extends Worlds, B extends Blocks> void registerWorldAccessors(
        RapunzelContext context,
        W worlds,
        Class<W> worldsType,
        B blocks,
        Class<B> blocksType
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(worlds, "worlds");
        Objects.requireNonNull(worldsType, "worldsType");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(blocksType, "blocksType");

        W sharedWorlds = context.sharedRuntime().getOrCreate(worldsType, () -> worlds);
        B sharedBlocks = context.sharedRuntime().getOrCreate(blocksType, () -> blocks);
        context.registerLinked(worldsType, sharedWorlds, Worlds.class);
        context.registerLinked(blocksType, sharedBlocks, Blocks.class);
    }

    /**
     * Registers entity, item, and block type registries directly.
     *
     * @param context      the application context
     * @param entityTypes  the entity type registry
     * @param itemTypes    the item type registry
     * @param blockTypes   the block type registry
     */
    public static void registerTypeRegistries(
        RapunzelContext context,
        REntityTypeRegistry entityTypes,
        RItemTypeRegistry itemTypes,
        RBlockTypeRegistry blockTypes
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(entityTypes, "entityTypes");
        Objects.requireNonNull(itemTypes, "itemTypes");
        Objects.requireNonNull(blockTypes, "blockTypes");

        DefaultRRegistryAccess registries = context.sharedRuntime().getOrCreate(DefaultRRegistryAccess.class, DefaultRRegistryAccess::new);
        registries.register(RRegistries.ENTITY_TYPES, entityTypes);
        registries.register(RRegistries.ITEM_TYPES, itemTypes);
        registries.register(RRegistries.BLOCK_TYPES, blockTypes);

        registerRegistryAccess(context, DefaultRRegistryAccess.class, registries);
    }

    /**
     * Registers an {@link RRegistryAccess} in the context and creates registry-backed
     * type registries for entities, items, and blocks.
     *
     * @param context            the application context
     * @param registryAccessType the registry access type
     * @param registries         the registry access instance
     * @param <A>                the registry access type
     * @return the shared registry access instance
     */
    public static <A extends RRegistryAccess> @NotNull A registerRegistryAccess(
        RapunzelContext context,
        Class<A> registryAccessType,
        A registries
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(registryAccessType, "registryAccessType");
        Objects.requireNonNull(registries, "registries");

        A sharedRegistries = context.sharedRuntime().getOrCreate(registryAccessType, () -> registries);
        context.register(registryAccessType, sharedRegistries);
        if (registryAccessType != RRegistryAccess.class) {
            context.registerAlias(RRegistryAccess.class, registryAccessType);
        }

        context.register(REntityTypeRegistry.class, new RegistryAccessBackedEntityTypeRegistry(sharedRegistries));
        context.register(RItemTypeRegistry.class, new RegistryAccessBackedItemTypeRegistry(sharedRegistries));
        context.register(RBlockTypeRegistry.class, new RegistryAccessBackedBlockTypeRegistry(sharedRegistries));
        return sharedRegistries;
    }

    /**
     * Registers player, entity, world, and block accessors for a server.
     *
     * @param context      the application context
     * @param players      the players accessor
     * @param playersType  the concrete players accessor type
     * @param entities     the entities accessor
     * @param entitiesType the concrete entities accessor type
     * @param worlds       the worlds accessor
     * @param worldsType   the concrete worlds accessor type
     * @param blocks       the blocks accessor
     * @param blocksType   the concrete blocks accessor type
     * @param <P>          the players accessor type
     * @param <W>          the worlds accessor type
     * @param <B>          the blocks accessor type
     */
    public static <P extends Players, W extends Worlds, B extends Blocks> void registerServerAccessors(
        RapunzelContext context,
        P players,
        Class<P> playersType,
        Entities entities,
        Class<? extends Entities> entitiesType,
        W worlds,
        Class<W> worldsType,
        B blocks,
        Class<B> blocksType
    ) {
        registerPlayerAccessors(context, players, playersType);
        registerEntityAccessors(context, entities, castType(entitiesType));
        registerWorldAccessors(context, worlds, worldsType, blocks, blocksType);
    }

    /**
     * Registers all server platform services including accessors, native interop,
     * and registry access.
     *
     * @param context              the application context
     * @param players              the players accessor
     * @param playersType          the concrete players accessor type
     * @param entities             the entities accessor
     * @param entitiesType         the concrete entities accessor type
     * @param worlds               the worlds accessor
     * @param worldsType           the concrete worlds accessor type
     * @param blocks               the blocks accessor
     * @param blocksType           the concrete blocks accessor type
     * @param nativeInteropRegistrar consumer to register native interop adapters
     * @param registryAccessFactory supplier for the registry access implementation
     * @param <P>                  the players accessor type
     * @param <E>                  the entities accessor type
     * @param <W>                  the worlds accessor type
     * @param <B>                  the blocks accessor type
     */
    public static <P extends Players, E extends Entities, W extends Worlds, B extends Blocks> void registerServerPlatformServices(
        RapunzelContext context,
        P players,
        Class<P> playersType,
        E entities,
        Class<E> entitiesType,
        W worlds,
        Class<W> worldsType,
        B blocks,
        Class<B> blocksType,
        Consumer<? super MutableRNativeInterop> nativeInteropRegistrar,
        Supplier<? extends RRegistryAccess> registryAccessFactory
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(playersType, "playersType");
        Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(entitiesType, "entitiesType");
        Objects.requireNonNull(worlds, "worlds");
        Objects.requireNonNull(worldsType, "worldsType");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(blocksType, "blocksType");
        Objects.requireNonNull(nativeInteropRegistrar, "nativeInteropRegistrar");
        Objects.requireNonNull(registryAccessFactory, "registryAccessFactory");

        registerServerAccessors(context, players, playersType, entities, entitiesType, worlds, worldsType, blocks, blocksType);
        nativeInteropRegistrar.accept(context.services().get(MutableRNativeInterop.class));
        RRegistryAccess registries = Objects.requireNonNull(registryAccessFactory.get(), "registryAccessFactory.get()");
        registerRegistryAccess(context, RRegistryAccess.class, registries);
    }

    /**
     * Registers world accessors (deprecated convenience overload).
     *
     * @param context      the application context
     * @param players      the players accessor
     * @param playersType  the concrete players accessor type
     * @param entities     the entities accessor
     * @param entitiesType the concrete entities accessor type
     * @param worlds       the worlds accessor
     * @param worldsType   the concrete worlds accessor type
     * @param blocks       the blocks accessor
     * @param blocksType   the concrete blocks accessor type
     * @param <P>          the players accessor type
     * @param <W>          the worlds accessor type
     * @param <B>          the blocks accessor type
     */
    public static <P extends Players, W extends Worlds, B extends Blocks> void registerWorldAccessors(
        RapunzelContext context,
        P players,
        Class<P> playersType,
        Entities entities,
        Class<? extends Entities> entitiesType,
        W worlds,
        Class<W> worldsType,
        B blocks,
        Class<B> blocksType
    ) {
        registerServerAccessors(context, players, playersType, entities, entitiesType, worlds, worldsType, blocks, blocksType);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Entities> Class<E> castType(Class<? extends Entities> entitiesType) {
        return (Class<E>) entitiesType;
    }

    /**
     * Registers the YAML-based message format service in the context.
     *
     * @param ctx          the application context
     * @param configService the config service
     * @param logger       the logger
     * @param dataDir      the plugin data directory
     * @return the registered message format service
     */
    @SuppressWarnings("UnusedReturnValue")
    public static MessageFormatService registerYamlMessages(
        RapunzelContext ctx,
        ConfigService configService,
        Logger logger,
        Path dataDir
    ) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(configService, "configService");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataDir, "dataDir");

        MessageFormatService messageFormatService =
            new YamlMessageFormatService(configService, logger, dataDir.resolve("messages.yml"), "messages.yml");
        ctx.register(MessageFormatService.class, messageFormatService);
        return messageFormatService;
    }
}
