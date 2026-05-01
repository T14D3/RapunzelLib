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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RCommandSourcesTest {
    @Test
    void legacyAudienceOverloadsUseSingleReplyChannel() {
        RecordingAudience audience = new RecordingAudience();

        RCommandSource source = RCommandSources.of(PlatformId.PAPER, new Object(), audience, Optional.empty());

        Component reply = Component.text("reply");
        Component system = Component.text("system");
        Component failure = Component.text("failure");
        source.sendMessage(reply);
        source.sendSystemMessage(system);
        source.sendFailure(failure);

        assertSame(audience, source.audience());
        assertSame(audience, source.systemAudience());
        assertSame(audience, source.failureAudience());
        assertEquals(List.of(reply, system, failure), audience.messages());
        assertTrue(source.player().isEmpty());
    }

    @Test
    void explicitReplyChannelsPreserveDedicatedAudiences() {
        RecordingAudience replyAudience = new RecordingAudience();
        RecordingAudience systemAudience = new RecordingAudience();
        RecordingAudience failureAudience = new RecordingAudience();

        RCommandSource source = RCommandSources.of(
            PlatformId.PAPER,
            new Object(),
            RCommandSources.replyChannels(replyAudience, systemAudience, failureAudience),
            Optional.empty(),
            permission -> permission.equals("zones.use")
        );

        Component reply = Component.text("reply");
        Component system = Component.text("system");
        Component failure = Component.text("failure");
        source.sendMessage(reply);
        source.sendSystemMessage(system);
        source.sendFailure(failure);

        assertSame(replyAudience, source.audience());
        assertSame(systemAudience, source.systemAudience());
        assertSame(failureAudience, source.failureAudience());
        assertEquals(List.of(reply), replyAudience.messages());
        assertEquals(List.of(system), systemAudience.messages());
        assertEquals(List.of(failure), failureAudience.messages());
        assertTrue(source.hasPermission("zones.use"));
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
