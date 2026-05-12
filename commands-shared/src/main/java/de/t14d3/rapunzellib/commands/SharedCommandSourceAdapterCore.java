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
 * <p>
 * Uses reflection to invoke {@code sendSuccess}, {@code sendFailure}, and
 * {@code sendSystemMessage} on {@link CommandSourceStack}, converting between
 * Adventure {@link Component} and Minecraft's native component types.
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

    /**
     * Extracts reply channels from a {@link CommandSourceStack}.
     *
     * @param source the command source
     * @return the reply channels
     */
    public static @NotNull RCommandSources.ReplyChannels commandSourceStackReplyChannels(@NotNull CommandSourceStack source) {
        Objects.requireNonNull(source, "source");

        return RCommandSources.replyChannels(
            nativeAudience(message -> sendSuccess(source, message)),
            nativeAudience(message -> sendSystemMessage(source, message)),
            nativeAudience(message -> sendFailure(source, message))
        );
    }

    /**
     * Wraps a native command source into an {@link RCommandSource}.
     *
     * @param platformId             the platform identifier
     * @param source                 the native command source
     * @param replyChannelsExtractor extracts reply channels
     * @param permissionChecker      checks permissions
     * @param playerExtractor        extracts an optional RPlayer
     * @param <T>                    the native source type
     * @return the wrapped command source
     */
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

    /**
     * Wraps a native command source using a throwing function for player extraction.
     *
     * @param platformId        the platform identifier
     * @param source            the native command source
     * @param permissionChecker checks permissions
     * @param playerExtractor   a throwing function that extracts the native player
     * @param <T>               the native source type
     * @return the wrapped command source
     */
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

    /**
     * Resolves reply channels from a source using a throwing player extractor.
     *
     * @param source          the source
     * @param playerExtractor the player extractor
     * @param <T>             the source type
     * @return the reply channels
     */
    private static <T> @NotNull RCommandSources.ReplyChannels resolveReplyChannels(
        @NotNull T source,
        @NotNull ThrowingFunction<? super T, ?> playerExtractor
    ) {
        Optional<RPlayer> player = resolvePlayer(source, playerExtractor);
        return player
            .map(value -> RCommandSources.replyChannels(value.audience()))
            .orElseGet(() -> RCommandSources.replyChannels(Audience.empty()));
    }

    /**
     * Resolves a player from a source using a throwing extractor.
     *
     * @param source          the source
     * @param playerExtractor the player extractor
     * @param <T>             the source type
     * @return an optional RPlayer
     */
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

    /**
     * Creates an Adventure {@link Audience} that delegates to the given consumer.
     *
     * @param consumer the consumer for outgoing messages
     * @return the audience
     */
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

    /**
     * Sends a success message via reflection.
     *
     * @param source  the command source
     * @param message the message component
     */
    private static void sendSuccess(@NotNull CommandSourceStack source, @NotNull Component message) {
        invokeVoid(SEND_SUCCESS_METHOD, source, (Supplier<net.minecraft.network.chat.Component>) () -> toNative(message), false);
    }

    /**
     * Sends a system message via reflection, falling back to sendSuccess if unavailable.
     *
     * @param source  the command source
     * @param message the message component
     */
    private static void sendSystemMessage(@NotNull CommandSourceStack source, @NotNull Component message) {
        if (SEND_SYSTEM_MESSAGE_METHOD == null) {
            sendSuccess(source, message);
            return;
        }
        invokeVoid(SEND_SYSTEM_MESSAGE_METHOD, source, toNative(message));
    }

    /**
     * Sends a failure message via reflection.
     *
     * @param source  the command source
     * @param message the message component
     */
    private static void sendFailure(@NotNull CommandSourceStack source, @NotNull Component message) {
        invokeVoid(SEND_FAILURE_METHOD, source, toNative(message));
    }

    /**
     * Converts an Adventure component to a Minecraft native component.
     *
     * @param message the Adventure component
     * @return the native component
     */
    private static @NotNull net.minecraft.network.chat.Component toNative(@NotNull Component message) {
        String json = GsonComponentSerializer.gson().serialize(Objects.requireNonNull(message, "message"));
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    /**
     * Resolves a required method via reflection.
     *
     * @param owner          the owning class
     * @param name           the method name
     * @param parameterTypes the parameter types
     * @return the method
     * @throws IllegalStateException if the method is not found
     */
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

    /**
     * Tries to resolve a method via reflection.
     *
     * @param owner          the owning class
     * @param name           the method name
     * @param parameterTypes the parameter types
     * @return the method, or {@code null} if not found
     */
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

    /**
     * Invokes a void method via reflection.
     *
     * @param method    the method to invoke
     * @param target    the target object
     * @param arguments the arguments
     * @throws IllegalStateException if invocation fails
     */
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

    /**
     * A functional interface that allows throwing checked exceptions.
     *
     * @param <T> the input type
     * @param <R> the result type
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        /**
         * Applies this function to the given value.
         *
         * @param value the input
         * @return the result
         * @throws Exception if an error occurs
         */
        R apply(T value) throws Exception;
    }
}
