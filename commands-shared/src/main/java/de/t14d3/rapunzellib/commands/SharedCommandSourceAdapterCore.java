package de.t14d3.rapunzellib.commands;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Core utilities for wrapping native command sources into {@link RCommandSource}.
 * Uses reflection to invoke sendSuccess/sendFailure/sendSystemMessage on {@link CommandSourceStack}.
 */
public final class SharedCommandSourceAdapterCore {
    private static final Method SEND_SUCCESS_METHOD = requiredMethod(CommandSourceStack.class, "sendSuccess", Supplier.class, boolean.class);
    private static final Method SEND_FAILURE_METHOD = requiredMethod(
        CommandSourceStack.class,
        "sendFailure",
        net.minecraft.network.chat.Component.class
    );
    private static final @Nullable Method SEND_SYSTEM_MESSAGE_METHOD = optionalMethod(
        CommandSourceStack.class,
        "sendSystemMessage",
        net.minecraft.network.chat.Component.class
    );

    private SharedCommandSourceAdapterCore() {
    }

    public static @NotNull RCommandSources.ReplyChannels commandSourceStackReplyChannels(@NotNull CommandSourceStack source) {
        Objects.requireNonNull(source, "source");

        return RCommandSources.replyChannels(
            nativeAudience(message -> sendSuccess(source, message)),
            nativeAudience(message -> sendSystemMessage(source, message)),
            nativeAudience(message -> sendFailure(source, message))
        );
    }

    public static <T> @NotNull RCommandSource wrap(
        @NotNull PlatformId platformId,
        @NotNull T source,
        @NotNull Function<? super T, ? extends RCommandSources.ReplyChannels> replyChannelsExtractor,
        @NotNull BiPredicate<? super T, ? super String> permissionChecker,
        @NotNull Function<? super T, Optional<RPlayer>> playerExtractor
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(replyChannelsExtractor, "replyChannelsExtractor");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        Objects.requireNonNull(playerExtractor, "playerExtractor");

        RCommandSources.ReplyChannels replyChannels = Objects.requireNonNull(replyChannelsExtractor.apply(source), "replyChannels");
        Optional<RPlayer> player = Objects.requireNonNull(playerExtractor.apply(source), "player");
        return RCommandSources.of(platformId, source, replyChannels, player, permission -> permissionChecker.test(source, permission));
    }

    public static <T> RCommandSource wrap(
        PlatformId platformId,
        T source,
        BiPredicate<? super T, ? super String> permissionChecker,
        ThrowingFunction<? super T, ?> playerExtractor
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        Objects.requireNonNull(playerExtractor, "playerExtractor");

        return wrap(
            platformId,
            source,
            value -> resolveReplyChannels(value, playerExtractor),
            permissionChecker,
            value -> resolvePlayer(value, playerExtractor)
        );
    }

    private static <T> @NotNull RCommandSources.ReplyChannels resolveReplyChannels(
        @NotNull T source,
        @NotNull ThrowingFunction<? super T, ?> playerExtractor
    ) {
        Optional<RPlayer> player = resolvePlayer(source, playerExtractor);
        return player
            .map(value -> RCommandSources.replyChannels(value.audience()))
            .orElseGet(() -> RCommandSources.replyChannels(Audience.empty()));
    }

    private static <T> @NotNull Optional<RPlayer> resolvePlayer(
        @NotNull T source,
        @NotNull ThrowingFunction<? super T, ?> playerExtractor
    ) {
        Object nativePlayer;
        try {
            nativePlayer = playerExtractor.apply(source);
        } catch (Exception ignored) {
            nativePlayer = null;
        }

        if (nativePlayer == null) {
            return Optional.empty();
        }

        return Optional.of(Rapunzel.players().require(nativePlayer));
    }

    private static @NotNull Audience nativeAudience(@NotNull Consumer<Component> consumer) {
        Objects.requireNonNull(consumer, "consumer");

        return new Audience() {
            @Override
            public void sendMessage(@NotNull Component message) {
                consumer.accept(Objects.requireNonNull(message, "message"));
            }

            @Override
            public void sendActionBar(@NotNull Component message) {
                sendMessage(message);
            }
        };
    }

    private static void sendSuccess(@NotNull CommandSourceStack source, @NotNull Component message) {
        invokeVoid(SEND_SUCCESS_METHOD, source, (Supplier<net.minecraft.network.chat.Component>) () -> toNative(message), false);
    }

    private static void sendSystemMessage(@NotNull CommandSourceStack source, @NotNull Component message) {
        if (SEND_SYSTEM_MESSAGE_METHOD == null) {
            sendSuccess(source, message);
            return;
        }
        invokeVoid(SEND_SYSTEM_MESSAGE_METHOD, source, toNative(message));
    }

    private static void sendFailure(@NotNull CommandSourceStack source, @NotNull Component message) {
        invokeVoid(SEND_FAILURE_METHOD, source, toNative(message));
    }

    private static @NotNull net.minecraft.network.chat.Component toNative(@NotNull Component message) {
        String json = GsonComponentSerializer.gson().serialize(Objects.requireNonNull(message, "message"));
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static @NotNull Method requiredMethod(
        @NotNull Class<?> owner,
        @NotNull String name,
        Class<?>... parameterTypes
    ) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Missing method " + owner.getName() + "#" + name, ex);
        }
    }

    private static @Nullable Method optionalMethod(
        @NotNull Class<?> owner,
        @NotNull String name,
        Class<?>... parameterTypes
    ) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void invokeVoid(@NotNull Method method, @NotNull Object target, Object... arguments) {
        try {
            method.invoke(target, arguments);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                "Could not invoke method " + method.getDeclaringClass().getName() + "#" + method.getName(),
                ex
            );
        }
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T value) throws Exception;
    }
}
