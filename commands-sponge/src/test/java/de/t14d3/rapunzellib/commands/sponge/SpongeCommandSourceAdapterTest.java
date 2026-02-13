package de.t14d3.rapunzellib.commands.sponge;

import de.t14d3.rapunzellib.objects.RNativeHandle;
import net.kyori.adventure.audience.Audience;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.command.CommandCause;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpongeCommandSourceAdapterTest {
    @Test
    void wrapsSpongeCommandCauseWithoutLosingNativeHandle() {
        Audience audience = audience();
        CommandCause cause = commandCause(audience, Set.of("zones.use"));

        var wrapped = SpongeCommandSourceAdapter.wrap(cause);

        assertSame(cause, ((RNativeHandle<?>) wrapped).handle());
        assertSame(audience, wrapped.audience());
        assertTrue(wrapped.hasPermission("zones.use"));
        assertFalse(wrapped.hasPermission("zones.admin"));
        assertTrue(wrapped.player().isEmpty());
    }

    private static CommandCause commandCause(Audience audience, Set<String> permissions) {
        return (CommandCause) Proxy.newProxyInstance(
            CommandCause.class.getClassLoader(),
            new Class<?>[]{CommandCause.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "audience" -> audience;
                case "hasPermission" -> permissions.contains((String) args[0]);
                case "first" -> Optional.empty();
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "test-sponge-command-cause";
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Audience audience() {
        return (Audience) Proxy.newProxyInstance(
            Audience.class.getClassLoader(),
            new Class<?>[]{Audience.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "test-audience";
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
