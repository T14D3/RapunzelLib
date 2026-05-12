package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Support utility for installing shared command source stack support.
 * <p>
 * Provides factory methods for creating {@link CommandSourceStackAdapterSpec}
 * instances, wiring them into the context, and resolving permissions through
 * either modern or legacy API paths.
 */
public final class SharedCommandFeatureInstallerSupport {
    private static final int OPERATOR_PERMISSION_LEVEL = 4;
    private static final @Nullable Method COMMAND_SOURCE_STACK_PERMISSIONS_METHOD = optionalMethod(CommandSourceStack.class, "permissions");
    private static final @Nullable Method COMMAND_SOURCE_STACK_HAS_PERMISSION_LEVEL_METHOD = optionalMethod(
        CommandSourceStack.class,
        "hasPermission",
        int.class
    );
    private static final @Nullable Class<?> PERMISSION_CLASS = optionalClass("net.minecraft.server.permissions.Permission");
    private static final @Nullable Object COMMANDS_OWNER_PERMISSION = optionalStaticField(
        "net.minecraft.server.permissions.Permissions",
        "COMMANDS_OWNER"
    );
    private static final @Nullable Method PERMISSION_SET_HAS_PERMISSION_METHOD = permissionSetHasPermissionMethod();

    private SharedCommandFeatureInstallerSupport() {
    }

    /**
     * Specification for adapting a command source type into an {@link RCommandSource}.
     *
     * @param <T> the native command source type
     */
    public record CommandSourceStackAdapterSpec<T>(
        @NotNull PlatformId platformId,
        @NotNull Class<T> sourceType,
        @NotNull Function<? super T, ? extends RCommandSources.ReplyChannels> replyChannelsExtractor,
        @NotNull BiPredicate<? super T, ? super String> permissionChecker,
        @NotNull Function<? super T, Optional<RPlayer>> playerExtractor
    ) {
        /**
         * Creates an adapter spec with non-null validation.
         *
         * @param platformId             the platform identifier
         * @param sourceType             the native source class
         * @param replyChannelsExtractor extracts reply channels from the source
         * @param permissionChecker      checks permissions on the source
         * @param playerExtractor        extracts an optional RPlayer from the source
         */
        public CommandSourceStackAdapterSpec {
            Objects.requireNonNull(platformId, "platformId");
            Objects.requireNonNull(sourceType, "sourceType");
            Objects.requireNonNull(replyChannelsExtractor, "replyChannelsExtractor");
            Objects.requireNonNull(permissionChecker, "permissionChecker");
            Objects.requireNonNull(playerExtractor, "playerExtractor");
        }
    }

    /**
     * Creates a default adapter spec for {@link CommandSourceStack}.
     *
     * @param platformId the platform identifier
     * @return the default adapter spec
     */
    public static @NotNull CommandSourceStackAdapterSpec<CommandSourceStack> defaultCommandSourceStackAdapterSpec(
        @NotNull PlatformId platformId
    ) {
        Objects.requireNonNull(platformId, "platformId");

        return new CommandSourceStackAdapterSpec<>(
            platformId,
            CommandSourceStack.class,
            SharedCommandSourceAdapterCore::commandSourceStackReplyChannels,
            SharedCommandFeatureInstallerSupport::defaultPermissionChecker,
            SharedCommandFeatureInstallerSupport::defaultPlayerExtractor
        );
    }

    /**
     * Installs command source stack support using the default spec.
     *
     * @param context    the Rapunzel context
     * @param platformId the platform identifier
     */
    public static void installCommandSourceStackSupport(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId
    ) {
        installCommandSourceStackSupport(context, defaultCommandSourceStackAdapterSpec(platformId));
    }

    /**
     * Installs command source stack support using a custom spec.
     *
     * @param context the Rapunzel context
     * @param spec    the adapter specification
     * @param <T>     the native command source type
     */
    public static <T> void installCommandSourceStackSupport(
        @NotNull RapunzelContext context,
        @NotNull CommandSourceStackAdapterSpec<T> spec
    ) {
        registerCommandSourceStackAdapter(context, spec);
    }

    /**
     * Registers a command source stack adapter from the given spec.
     *
     * @param context the Rapunzel context
     * @param spec    the adapter specification
     * @param <T>     the native command source type
     */
    public static <T> void registerCommandSourceStackAdapter(
        @NotNull RapunzelContext context,
        @NotNull CommandSourceStackAdapterSpec<T> spec
    ) {
        Objects.requireNonNull(spec, "spec");

        registerCommandSourceStackAdapter(
            context,
            spec.platformId(),
            spec.sourceType(),
            spec.replyChannelsExtractor(),
            spec.permissionChecker(),
            spec.playerExtractor()
        );
    }

    /**
     * Registers a command source stack adapter from individual components.
     *
     * @param context                the Rapunzel context
     * @param platformId             the platform identifier
     * @param sourceType             the native source class
     * @param replyChannelsExtractor extracts reply channels
     * @param permissionChecker      checks permissions
     * @param playerExtractor        extracts an optional RPlayer
     * @param <T>                    the native command source type
     */
    public static <T> void registerCommandSourceStackAdapter(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull Class<T> sourceType,
        @NotNull Function<? super T, ? extends RCommandSources.ReplyChannels> replyChannelsExtractor,
        @NotNull BiPredicate<? super T, ? super String> permissionChecker,
        @NotNull Function<? super T, Optional<RPlayer>> playerExtractor
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(replyChannelsExtractor, "replyChannelsExtractor");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        Objects.requireNonNull(playerExtractor, "playerExtractor");

        CommandSourceAdapter adapter = CommandFeatureInstallerSupport.commandSourceAdapter(
            platformId,
            sourceType,
            source -> SharedCommandSourceAdapterCore.wrap(
                platformId,
                source,
                replyChannelsExtractor,
                permissionChecker,
                playerExtractor
            )
        );

        CommandFeatureInstallerSupport.registerSharedBrigadierCommandServices(
            context,
            platformId,
            List.of(adapter),
            sourceType
        );
        installRuntimeCommandRegistrationSupport(context);
    }

    /**
     * Registers a command source stack adapter using a throwing function for player extraction.
     *
     * @param context           the Rapunzel context
     * @param platformId        the platform identifier
     * @param sourceType        the native source class
     * @param permissionChecker checks permissions
     * @param playerExtractor   a throwing function that extracts the native player
     * @param <T>               the native command source type
     */
    public static <T> void registerCommandSourceStackAdapter(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull Class<T> sourceType,
        @NotNull BiPredicate<? super T, ? super String> permissionChecker,
        @NotNull SharedCommandSourceAdapterCore.ThrowingFunction<? super T, ?> playerExtractor
    ) {
        registerCommandSourceStackAdapter(
            context,
            platformId,
            sourceType,
            source -> sharedReplyChannelsExtractor(source, playerExtractor),
            permissionChecker,
            source -> sharedPlayerExtractor(source, playerExtractor)
        );
    }

    /**
     * Installs and returns a {@link SharedRuntimeCommandRegistrationSupport} for the context.
     *
     * @param context the Rapunzel context
     * @return the registration support instance
     */
    public static @NotNull SharedRuntimeCommandRegistrationSupport installRuntimeCommandRegistrationSupport(
        @NotNull RapunzelContext context
    ) {
        Objects.requireNonNull(context, "context");

        return context.getOrCreate(
            SharedRuntimeCommandRegistrationSupport.class,
            () -> new SharedRuntimeCommandRegistrationSupport(
                context.services().get(RCommandService.class),
                context.scheduler(),
                context.services().get(MinecraftServer.class)
            )
        );
    }

    /**
     * Extracts a player from a CommandSourceStack.
     *
     * @param source the command source
     * @return an optional RPlayer
     */
    private static @NotNull Optional<RPlayer> defaultPlayerExtractor(@NotNull CommandSourceStack source) {
        Objects.requireNonNull(source, "source");
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return Optional.empty();
        }
        return Rapunzel.players().wrap(player);
    }

    /**
     * Default permission checker for CommandSourceStack.
     *
     * @param source     the command source
     * @param permission the permission to check
     * @return {@code true} if the source has the permission
     */
    private static boolean defaultPermissionChecker(@NotNull CommandSourceStack source, @NotNull String permission) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(permission, "permission");
        if (permission.isBlank()) {
            return true;
        }

        return defaultPlayerExtractor(source)
            .map(player -> player.hasPermission(permission))
            .orElseGet(() -> hasOperatorPermission(source));
    }

    /**
     * Checks if the source has operator-level permission.
     *
     * @param source the command source
     * @return {@code true} if operator
     */
    private static boolean hasOperatorPermission(@NotNull CommandSourceStack source) {
        Boolean modernPermission = invokeModernPermission(source);
        if (modernPermission != null) {
            return modernPermission;
        }

        Boolean legacyPermission = invokeBoolean(COMMAND_SOURCE_STACK_HAS_PERMISSION_LEVEL_METHOD, source, OPERATOR_PERMISSION_LEVEL);
        return legacyPermission != null && legacyPermission;
    }

    /**
     * Extracts reply channels using a throwing player extractor.
     *
     * @param source          the command source
     * @param playerExtractor the throwing player extractor
     * @param <T>             the native source type
     * @return the reply channels
     */
    private static <T> @NotNull RCommandSources.ReplyChannels sharedReplyChannelsExtractor(
        @NotNull T source,
        @NotNull SharedCommandSourceAdapterCore.ThrowingFunction<? super T, ?> playerExtractor
    ) {
        Optional<RPlayer> player = sharedPlayerExtractor(source, playerExtractor);
        return player
            .map(value -> RCommandSources.replyChannels(value.audience()))
            .orElseGet(() -> RCommandSources.replyChannels(net.kyori.adventure.audience.Audience.empty()));
    }

    /**
     * Extracts a player using a throwing extractor.
     *
     * @param source          the command source
     * @param playerExtractor the throwing player extractor
     * @param <T>             the native source type
     * @return an optional RPlayer
     */
    private static <T> @NotNull Optional<RPlayer> sharedPlayerExtractor(
        @NotNull T source,
        @NotNull SharedCommandSourceAdapterCore.ThrowingFunction<? super T, ?> playerExtractor
    ) {
        try {
            Object nativePlayer = playerExtractor.apply(source);
            if (nativePlayer == null) {
                return Optional.empty();
            }
            return Optional.of(Rapunzel.players().require(nativePlayer));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Invokes the modern permission check (1.21.x+) on the command source.
     *
     * @param source the command source
     * @return the permission result, or {@code null} if unavailable
     */
    private static @Nullable Boolean invokeModernPermission(@NotNull CommandSourceStack source) {
        if (COMMAND_SOURCE_STACK_PERMISSIONS_METHOD == null
            || PERMISSION_SET_HAS_PERMISSION_METHOD == null
            || COMMANDS_OWNER_PERMISSION == null) {
            return null;
        }

        try {
            Object permissionSet = COMMAND_SOURCE_STACK_PERMISSIONS_METHOD.invoke(source);
            if (permissionSet == null) {
                return null;
            }
            return (boolean) PERMISSION_SET_HAS_PERMISSION_METHOD.invoke(permissionSet, COMMANDS_OWNER_PERMISSION);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not resolve command source permissions", ex);
        }
    }

    /**
     * Resolves the PermissionSet#hasPermission method via reflection.
     *
     * @return the method, or {@code null} if unavailable
     */
    private static @Nullable Method permissionSetHasPermissionMethod() {
        if (COMMAND_SOURCE_STACK_PERMISSIONS_METHOD == null || PERMISSION_CLASS == null) {
            return null;
        }

        return optionalMethod(COMMAND_SOURCE_STACK_PERMISSIONS_METHOD.getReturnType(), "hasPermission", PERMISSION_CLASS);
    }

    /**
     * Tries to load a class by name.
     *
     * @param className the fully qualified class name
     * @return the class, or {@code null} if not found
     */
    private static @Nullable Class<?> optionalClass(@NotNull String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    /**
     * Tries to read a static field value.
     *
     * @param ownerClassName the owning class name
     * @param fieldName      the field name
     * @return the field value, or {@code null} on failure
     */
    private static @Nullable Object optionalStaticField(@NotNull String ownerClassName, @NotNull String fieldName) {
        Class<?> owner = optionalClass(ownerClassName);
        if (owner == null) {
            return null;
        }

        try {
            return owner.getField(fieldName).get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Tries to resolve a method by name and parameter types.
     *
     * @param owner          the owning class
     * @param methodName     the method name
     * @param parameterTypes the parameter types
     * @return the method, or {@code null} on failure
     */
    private static @Nullable Method optionalMethod(
        @NotNull Class<?> owner,
        @NotNull String methodName,
        Class<?>... parameterTypes
    ) {
        try {
            return owner.getMethod(methodName, parameterTypes);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Invokes a method with a boolean return type.
     *
     * @param method    the method (may be null)
     * @param target    the target object
     * @param arguments the arguments
     * @return the boolean result, or {@code null} if method is null
     * @throws IllegalStateException if invocation fails
     */
    private static @Nullable Boolean invokeBoolean(@Nullable Method method, @NotNull Object target, Object... arguments) {
        if (method == null) {
            return null;
        }

        try {
            return (boolean) method.invoke(target, arguments);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                "Could not invoke method " + method.getDeclaringClass().getName() + "#" + method.getName(),
                ex
            );
        }
    }
}
