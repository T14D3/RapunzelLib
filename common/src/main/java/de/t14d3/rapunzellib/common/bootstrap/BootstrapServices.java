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

public final class BootstrapServices {
    private BootstrapServices() {
    }

    public record FirstPhaseResult(RapunzelContext context, ConfigService configService) {
        public FirstPhaseResult {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(configService, "configService");
        }
    }

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

    public static @NotNull PlatformRuntime serverRuntime(
        PlatformId platformId,
        EngineFamily engineFamily,
        Object lifecycleOwner,
        RuntimeProfile runtimeProfile
    ) {
        return runtime(platformId, RuntimeRole.SERVER, engineFamily, lifecycleOwner, runtimeProfile);
    }

    public static @NotNull PlatformRuntime serverRuntime(
        PlatformId platformId,
        EngineFamily engineFamily,
        Object lifecycleOwner,
        RuntimeCapability... capabilities
    ) {
        return runtime(platformId, RuntimeRole.SERVER, engineFamily, lifecycleOwner, capabilities);
    }

    public static @NotNull PlatformRuntime proxyRuntime(
        PlatformId platformId,
        Object lifecycleOwner,
        RuntimeProfile runtimeProfile
    ) {
        return runtime(platformId, RuntimeRole.PROXY, EngineFamily.PROXY, lifecycleOwner, runtimeProfile);
    }

    public static @NotNull PlatformRuntime proxyRuntime(
        PlatformId platformId,
        Object lifecycleOwner,
        RuntimeCapability... capabilities
    ) {
        return runtime(platformId, RuntimeRole.PROXY, EngineFamily.PROXY, lifecycleOwner, capabilities);
    }

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

    public static @NotNull PlatformRuntime runtime(
        PlatformId platformId,
        RuntimeRole role,
        EngineFamily engineFamily,
        Object lifecycleOwner,
        RuntimeCapability... capabilities
    ) {
        return runtime(platformId, role, engineFamily, lifecycleOwner, profileOf(capabilities));
    }

    public static @NotNull RuntimeProfile profileOf(@NotNull RuntimeCapability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");

        EnumSet<RuntimeCapability> capabilitySet = EnumSet.noneOf(RuntimeCapability.class);
        for (RuntimeCapability capability : capabilities) {
            capabilitySet.add(Objects.requireNonNull(capability, "capability"));
        }
        return RuntimeProfile.of(capabilitySet);
    }

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

        DefaultRapunzelContext ctx = new DefaultRapunzelContext(runtime, logger, dataDir, resources, scheduler);
        if (scheduler instanceof AutoCloseable closeable) {
            ctx.registerCloseable(closeable);
        }
        return ctx;
    }

    public static DefaultRNativeInterop registerNativeInterop(RapunzelContext context) {
        Objects.requireNonNull(context, "context");

        return context.registerLinked(
            DefaultRNativeInterop.class,
            new DefaultRNativeInterop(context.platformId()),
            RNativeInterop.class,
            MutableRNativeInterop.class
        );
    }

    public static ConfigService registerYamlConfig(RapunzelContext ctx, ResourceProvider resources, Logger logger) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(logger, "logger");

        ConfigService configService = new SnakeYamlConfigService(resources, logger);
        ctx.register(ConfigService.class, configService);
        return configService;
    }

    public static <P extends Players> void registerPlayerAccessors(
        RapunzelContext context,
        P players,
        Class<P> playersType
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(playersType, "playersType");

        context.registerLinked(playersType, players, Players.class);
    }

    public static <E extends Entities> void registerEntityAccessors(
        RapunzelContext context,
        E entities,
        Class<E> entitiesType
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(entitiesType, "entitiesType");

        context.registerLinked(entitiesType, entities, Entities.class);
    }

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

        context.registerLinked(worldsType, worlds, Worlds.class);
        context.registerLinked(blocksType, blocks, Blocks.class);
    }

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

        DefaultRRegistryAccess registries = context.getOrCreate(DefaultRRegistryAccess.class, DefaultRRegistryAccess::new);
        registries.register(RRegistries.ENTITY_TYPES, entityTypes);
        registries.register(RRegistries.ITEM_TYPES, itemTypes);
        registries.register(RRegistries.BLOCK_TYPES, blockTypes);

        registerRegistryAccess(context, DefaultRRegistryAccess.class, registries);
    }

    public static <A extends RRegistryAccess> @NotNull A registerRegistryAccess(
        RapunzelContext context,
        Class<A> registryAccessType,
        A registries
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(registryAccessType, "registryAccessType");
        Objects.requireNonNull(registries, "registries");

        context.register(registryAccessType, registries);
        if (registryAccessType != RRegistryAccess.class) {
            context.registerAlias(RRegistryAccess.class, registryAccessType);
        }

        context.register(REntityTypeRegistry.class, new RegistryAccessBackedEntityTypeRegistry(registries));
        context.register(RItemTypeRegistry.class, new RegistryAccessBackedItemTypeRegistry(registries));
        context.register(RBlockTypeRegistry.class, new RegistryAccessBackedBlockTypeRegistry(registries));
        return registries;
    }

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
