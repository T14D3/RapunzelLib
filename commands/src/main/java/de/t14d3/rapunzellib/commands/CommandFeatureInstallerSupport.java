package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Support utilities for installing command services and registering command source adapters.
 * <p>
 * Provides static helper methods used by platform-specific installers to create and
 * register {@link RCommandService}, {@link CommandSourceAdapters}, and
 * {@link SharedBrigadierCommandRegistrar} instances within a {@link RapunzelContext}.
 * </p>
 */
public final class CommandFeatureInstallerSupport {
    private CommandFeatureInstallerSupport() {
    }

    /**
     * Registers command services with the given context and adapters, without a shared Brigadier registrar.
     *
     * @param context    the Rapunzel context
     * @param platformId the platform identifier
     * @param adapters   the list of command source adapters
     * @return the command service
     */
    public static @NotNull RCommandService registerCommandServices(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull List<CommandSourceAdapter> adapters
    ) {
        return registerCommandServices(context, platformId, adapters, null);
    }

    /**
     * Registers command services with the given context, adapters, and optional shared Brigadier registrar.
     *
     * @param context    the Rapunzel context
     * @param platformId the platform identifier
     * @param adapters   the list of command source adapters
     * @param registrar  the optional shared Brigadier registrar
     * @return the command service
     */
    public static @NotNull RCommandService registerCommandServices(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull List<CommandSourceAdapter> adapters,
        @Nullable SharedBrigadierCommandRegistrar<?> registrar
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(adapters, "adapters");

        RCommandService commandService = context.getOrCreate(
            RCommandService.class,
            () -> new DefaultRCommandService(platformId)
        );
        if (commandService.platformId() != platformId) {
            throw new IllegalStateException(
                "Command service platform mismatch: expected " + platformId + " but was " + commandService.platformId()
            );
        }

        context.register(CommandSourceAdapters.class, new CommandSourceAdapters(platformId, adapters));
        if (registrar != null) {
            context.register(SharedBrigadierCommandRegistrar.class, registrar);
        }
        return commandService;
    }

    /**
     * Registers command services along with a shared Brigadier command registrar.
     *
     * @param <S>        the Brigadier source type
     * @param context    the Rapunzel context
     * @param platformId the platform identifier
     * @param adapters   the list of command source adapters
     * @param sourceType the Brigadier source class
     * @return the command service
     */
    public static <S> @NotNull RCommandService registerSharedBrigadierCommandServices(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull List<CommandSourceAdapter> adapters,
        @NotNull Class<S> sourceType
    ) {
        Objects.requireNonNull(sourceType, "sourceType");

        RCommandService commandService = registerCommandServices(context, platformId, adapters);
        SharedBrigadierCommandRegistrar<S> registrar = sharedBrigadierCommandRegistrar(context, platformId, sourceType);
        context.register(SharedBrigadierCommandRegistrar.class, registrar);
        return commandService;
    }

    /**
     * Creates a shared Brigadier command registrar from the context.
     *
     * @param <S>        the Brigadier source type
     * @param context    the Rapunzel context
     * @param platformId the platform identifier
     * @param sourceType the Brigadier source class
     * @return the shared Brigadier command registrar
     */
    public static <S> @NotNull SharedBrigadierCommandRegistrar<S> sharedBrigadierCommandRegistrar(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull Class<S> sourceType
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(sourceType, "sourceType");

        RCommandService commandService = context.services().get(RCommandService.class);
        CommandSourceAdapters adapters = context.services().get(CommandSourceAdapters.class);
        return new DefaultSharedBrigadierCommandRegistrar<>(platformId, sourceType, commandService, adapters);
    }

    /**
     * Registers a command source adapter for a specific source type.
     *
     * @param <T>        the platform source type
     * @param context    the Rapunzel context
     * @param platformId the platform identifier
     * @param sourceType the source type class
     * @param wrap       the wrapping function
     */
    public static <T> void registerCommandSourceAdapter(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull Class<T> sourceType,
        @NotNull Function<? super T, ? extends RCommandSource> wrap
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(wrap, "wrap");

        registerCommandServices(context, platformId, List.of(commandSourceAdapter(platformId, sourceType, wrap)));
    }

    /**
     * Registers a command source adapter using a predicate-based source matcher.
     *
     * @param context    the Rapunzel context
     * @param platformId the platform identifier
     * @param supports   predicate that tests if a source is supported
     * @param wrap       the wrapping function
     */
    public static void registerCommandSourceAdapter(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull Predicate<Object> supports,
        @NotNull Function<Object, ? extends RCommandSource> wrap
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(supports, "supports");
        Objects.requireNonNull(wrap, "wrap");

        registerCommandServices(context, platformId, List.of(commandSourceAdapter(platformId, supports, wrap)));
    }

    /**
     * Creates a {@link CommandSourceAdapter} for a specific source type.
     *
     * @param <T>        the platform source type
     * @param platformId the platform identifier
     * @param sourceType the source type class
     * @param wrap       the wrapping function
     * @return the command source adapter
     */
    public static <T> @NotNull CommandSourceAdapter commandSourceAdapter(
        @NotNull PlatformId platformId,
        @NotNull Class<T> sourceType,
        @NotNull Function<? super T, ? extends RCommandSource> wrap
    ) {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(wrap, "wrap");

        return commandSourceAdapter(
            platformId,
            sourceType::isInstance,
            source -> wrap.apply(sourceType.cast(source))
        );
    }

    /**
     * Creates a {@link CommandSourceAdapter} using a predicate-based source matcher.
     *
     * @param platformId the platform identifier
     * @param supports   predicate that tests if a source is supported
     * @param wrap       the wrapping function
     * @return the command source adapter
     */
    public static @NotNull CommandSourceAdapter commandSourceAdapter(
        @NotNull PlatformId platformId,
        @NotNull Predicate<Object> supports,
        @NotNull Function<Object, ? extends RCommandSource> wrap
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(supports, "supports");
        Objects.requireNonNull(wrap, "wrap");

        return new CommandSourceAdapter() {
            @Override
            public @NotNull PlatformId platformId() {
                return platformId;
            }

            @Override
            public boolean supports(@NotNull Object source) {
                return supports.test(source);
            }

            @Override
            public @NotNull RCommandSource wrap(@NotNull Object source) {
                return wrap.apply(source);
            }
        };
    }
}
