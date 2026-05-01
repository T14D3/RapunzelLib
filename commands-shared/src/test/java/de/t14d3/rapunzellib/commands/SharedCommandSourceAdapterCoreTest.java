package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SharedCommandSourceAdapterCoreTest {
    @Test
    void wrapUsesConfiguredReplyChannelsForNonPlayers() {
        RecordingAudience replyAudience = new RecordingAudience();
        RecordingAudience systemAudience = new RecordingAudience();
        RecordingAudience failureAudience = new RecordingAudience();
        TestSource source = new TestSource();

        RCommandSource wrapped = SharedCommandSourceAdapterCore.wrap(
            PlatformId.FABRIC,
            source,
            value -> RCommandSources.replyChannels(replyAudience, systemAudience, failureAudience),
            (value, permission) -> permission.equals("zones.use"),
            value -> Optional.empty()
        );

        Component reply = Component.text("reply");
        Component system = Component.text("system");
        Component failure = Component.text("failure");
        wrapped.sendMessage(reply);
        wrapped.sendSystemMessage(system);
        wrapped.sendFailure(failure);

        assertSame(source, wrapped.handle());
        assertSame(replyAudience, wrapped.audience());
        assertSame(systemAudience, wrapped.systemAudience());
        assertSame(failureAudience, wrapped.failureAudience());
        assertEquals(List.of(reply), replyAudience.messages());
        assertEquals(List.of(system), systemAudience.messages());
        assertEquals(List.of(failure), failureAudience.messages());
        assertTrue(wrapped.hasPermission("zones.use"));
        assertFalse(wrapped.hasPermission("zones.admin"));
        assertTrue(wrapped.player().isEmpty());
    }

    private static final class TestSource {
    }

    private static final class RecordingAudience implements Audience {
        private final List<Component> messages = new ArrayList<>();

        @Override
        public void sendMessage(@NotNull Component message) {
            messages.add(message);
        }

        @Override
        public void sendActionBar(@NotNull Component message) {
            messages.add(message);
        }

        private @NotNull List<Component> messages() {
            return List.copyOf(messages);
        }
    }
}
