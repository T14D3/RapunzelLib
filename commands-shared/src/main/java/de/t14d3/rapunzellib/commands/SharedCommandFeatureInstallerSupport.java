package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class SharedCommandFeatureInstallerSupport {
    private static final String COMMAND_SOURCE_STACK_CLASS_NAME = "net.minecraft.commands.CommandSourceStack";
    private static final int OPERATOR_PERMISSION_LEVEL = 4;

    private SharedCommandFeatureInstallerSupport() {
    }

    public static void installCommandSourceStackSupport(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");

        Class<Object> sourceType = commandSourceStackType();
        Method getPlayerMethod = requiredMethod(sourceType, "getPlayer");
        Method hasPermissionMethod = requiredMethod(sourceType, "hasPermission", int.class);

        registerCommandSourceStackAdapter(
            context,
            platformId,
            sourceType,
            defaultPermissionChecker(getPlayerMethod, source -> invokeBoolean(hasPermissionMethod, source, OPERATOR_PERMISSION_LEVEL)),
            source -> getPlayerMethod.invoke(source)
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
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        Objects.requireNonNull(playerExtractor, "playerExtractor");

        CommandSourceAdapter adapter = CommandFeatureInstallerSupport.commandSourceAdapter(
            platformId,
            sourceType,
            source -> SharedCommandSourceAdapterCore.wrap(platformId, source, permissionChecker, playerExtractor)
        );

        CommandFeatureInstallerSupport.registerSharedBrigadierCommandServices(
            context,
            platformId,
            List.of(adapter),
            sourceType
        );
        installRuntimeCommandRegistrationSupport(context);
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

    private static @NotNull BiPredicate<Object, String> defaultPermissionChecker(
        @NotNull Method getPlayerMethod,
        @NotNull Predicate<Object> fallbackChecker
    ) {
        return (source, permission) -> {
            if (permission == null || permission.isBlank()) {
                return true;
            }

            try {
                Object nativePlayer = getPlayerMethod.invoke(source);
                return Rapunzel.players().require(nativePlayer).hasPermission(permission);
            } catch (Exception ignored) {
                return fallbackChecker.test(source);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Class<Object> commandSourceStackType() {
        try {
            return (Class<Object>) Class.forName(COMMAND_SOURCE_STACK_CLASS_NAME);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Missing Shared command source stack type: " + COMMAND_SOURCE_STACK_CLASS_NAME, ex);
        }
    }

    private static @NotNull Method requiredMethod(
        @NotNull Class<?> sourceType,
        @NotNull String methodName,
        Class<?>... parameterTypes
    ) {
        try {
            return sourceType.getMethod(methodName, parameterTypes);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Missing method " + sourceType.getName() + "#" + methodName, ex);
        }
    }

    private static boolean invokeBoolean(@NotNull Method method, @NotNull Object target, Object... arguments) {
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
