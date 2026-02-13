package de.t14d3.rapunzellib.commands.paper;

import de.t14d3.rapunzellib.objects.RNativeHandle;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PaperCommandSourceAdapterTest {
    @Test
    void wrapsCommandSenderWithoutLosingNativeHandle() {
        CommandSender sender = commandSender(Set.of("zones.use"));

        var wrapped = PaperCommandSourceAdapter.wrap(sender);

        assertSame(sender, ((RNativeHandle<?>) wrapped).handle());
        assertSame(sender, wrapped.audience());
        assertTrue(wrapped.hasPermission("zones.use"));
        assertFalse(wrapped.hasPermission("zones.admin"));
        assertTrue(wrapped.player().isEmpty());
    }

    @Test
    void rejectsNullSenders() {
        assertThrows(IllegalArgumentException.class, () -> PaperCommandSourceAdapter.wrap(null));
    }

    private static CommandSender commandSender(Set<String> permissions) {
        return (CommandSender) Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(),
            new Class<?>[]{CommandSender.class, Audience.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "hasPermission" -> permissions.contains((String) args[0]);
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "paper-command-sender";
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
