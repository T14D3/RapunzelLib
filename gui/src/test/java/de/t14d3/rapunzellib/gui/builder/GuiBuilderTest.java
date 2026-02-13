package de.t14d3.rapunzellib.gui.builder;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiCloseHooks;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.context.CloseReason;
import de.t14d3.rapunzellib.gui.core.GuiSlotPlan;
import de.t14d3.rapunzellib.gui.layout.LinearLayout;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GuiBuilderTest {
    private static final TestPlayer PLAYER = new TestPlayer();

    @Test
    void linearLayoutsReportZeroRowsAndPreserveOrientation() {
        Gui gui = Gui.builder()
            .title("Linear")
            .linear(false)
            .text(text -> text.text(Component.text("One")))
            .text(text -> text.text(Component.text("Two")))
            .build();

        assertEquals(0, gui.rows());
        LinearLayout layout = assertInstanceOf(LinearLayout.class, gui.layout());
        assertFalse(layout.isVertical());
        assertEquals(1, GuiSlotPlan.resolve(gui, 6).rows());
    }

    @Test
    void builtGuiSnapshotsMutableBuilderState() {
        GuiBuilder builder = Gui.builder().title("First");

        Gui first = builder.build();
        builder.title("Second");
        Gui second = builder.build();

        assertEquals(Component.text("First"), first.title());
        assertEquals(Component.text("Second"), second.title());
    }

    @Test
    void closeHooksRunOncePerOpenContext() {
        AtomicReference<CloseReason> closeReason = new AtomicReference<>();
        AtomicInteger closeCalls = new AtomicInteger();
        RecordingRenderer renderer = new RecordingRenderer();
        Gui gui = Gui.builder()
            .title("Closable")
            .renderer(renderer)
            .onClose(context -> {
                closeReason.set(context.reason());
                closeCalls.incrementAndGet();
            })
            .build();

        gui.open(PLAYER);
        GuiCloseHooks.close(gui, PLAYER, CloseReason.PLAYER);
        GuiCloseHooks.close(gui, PLAYER, CloseReason.UNKNOWN);

        assertEquals(1, closeCalls.get());
        assertEquals(CloseReason.PLAYER, closeReason.get());
    }

    @Test
    void rejectsOutOfRangeGridDefinitions() {
        GuiBuilder builder = Gui.builder().title("Grid").rows(1);

        assertThrows(IllegalArgumentException.class, () -> builder.slot(9, new TestElement()));
        assertThrows(IllegalArgumentException.class, () -> Gui.builder().title("Invalid").rows(0));
    }

    private static final class RecordingRenderer implements GuiRenderer {
        @Override
        public @NotNull String name() {
            return "recording";
        }

        @Override
        public @NotNull Set<GuiCapability> capabilities() {
            return Set.of();
        }

        @Override
        public boolean supports(@NotNull GuiCapability capability) {
            return false;
        }

        @Override
        public void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
        }

        @Override
        public void close(@NotNull Gui gui, @NotNull RPlayer player) {
        }
    }

    private static final class TestElement implements de.t14d3.rapunzellib.gui.element.GuiElement {
        @Override
        public @NotNull de.t14d3.rapunzellib.gui.element.ElementType type() {
            return de.t14d3.rapunzellib.gui.element.ElementType.TEXT;
        }
    }

    private static final class TestPlayer implements RPlayer {
        private final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000011");

        @Override
        public @NotNull UUID uuid() {
            return uuid;
        }

        @Override
        public @NotNull String name() {
            return "builder-test";
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return true;
        }

        @Override
        public Audience audience() {
            return Audience.empty();
        }

        @Override
        public @NotNull PlatformId platformId() {
            return PlatformId.PAPER;
        }

        @Override
        public @NotNull Object handle() {
            return this;
        }
    }
}
