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

    public record CommandSourceStackAdapterSpec<T>(
        @NotNull PlatformId platformId,
        @NotNull Class<T> sourceType,
        @NotNull Function<? super T, ? extends RCommandSources.ReplyChannels> replyChannelsExtractor,
        @NotNull BiPredicate<? super T, ? super String> permissionChecker,
        @NotNull Function<? super T, Optional<RPlayer>> playerExtractor
    ) {
        public CommandSourceStackAdapterSpec {
            Objects.requireNonNull(platformId, "platformId");
            Objects.requireNonNull(sourceType, "sourceType");
            Objects.requireNonNull(replyChannelsExtractor, "replyChannelsExtractor");
            Objects.requireNonNull(permissionChecker, "permissionChecker");
            Objects.requireNonNull(playerExtractor, "playerExtractor");
        }
    }

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

    public static void installCommandSourceStackSupport(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId
    ) {
        installCommandSourceStackSupport(context, defaultCommandSourceStackAdapterSpec(platformId));
    }

    public static <T> void installCommandSourceStackSupport(
        @NotNull RapunzelContext context,
        @NotNull CommandSourceStackAdapterSpec<T> spec
    ) {
        registerCommandSourceStackAdapter(context, spec);
    }

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

    private static @NotNull Optional<RPlayer> defaultPlayerExtractor(@NotNull CommandSourceStack source) {
        Objects.requireNonNull(source, "source");
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return Optional.empty();
        }
        return Rapunzel.players().wrap(player);
    }

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

    private static boolean hasOperatorPermission(@NotNull CommandSourceStack source) {
        Boolean modernPermission = invokeModernPermission(source);
        if (modernPermission != null) {
            return modernPermission;
        }

        Boolean legacyPermission = invokeBoolean(COMMAND_SOURCE_STACK_HAS_PERMISSION_LEVEL_METHOD, source, OPERATOR_PERMISSION_LEVEL);
        return legacyPermission != null && legacyPermission;
    }

    private static <T> @NotNull RCommandSources.ReplyChannels sharedReplyChannelsExtractor(
        @NotNull T source,
        @NotNull SharedCommandSourceAdapterCore.ThrowingFunction<? super T, ?> playerExtractor
    ) {
        Optional<RPlayer> player = sharedPlayerExtractor(source, playerExtractor);
        return player
            .map(value -> RCommandSources.replyChannels(value.audience()))
            .orElseGet(() -> RCommandSources.replyChannels(net.kyori.adventure.audience.Audience.empty()));
    }

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

    private static @Nullable Method permissionSetHasPermissionMethod() {
        if (COMMAND_SOURCE_STACK_PERMISSIONS_METHOD == null || PERMISSION_CLASS == null) {
            return null;
        }

        return optionalMethod(COMMAND_SOURCE_STACK_PERMISSIONS_METHOD.getReturnType(), "hasPermission", PERMISSION_CLASS);
    }

    private static @Nullable Class<?> optionalClass(@NotNull String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

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
