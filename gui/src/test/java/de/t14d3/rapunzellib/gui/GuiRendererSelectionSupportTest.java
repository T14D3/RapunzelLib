package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.gui.element.ButtonBuilder;
import de.t14d3.rapunzellib.gui.element.InputBuilder;
import de.t14d3.rapunzellib.gui.element.TextBuilder;
import de.t14d3.rapunzellib.gui.layout.GridLayout;
import de.t14d3.rapunzellib.gui.layout.LinearLayout;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiRendererSelectionSupportTest {
    @Test
    void prefersInventoryForPlainGridLayouts() {
        Gui gui = TestGui.of(
            GridLayout.builder(1)
                .slot(0, new ButtonBuilder().label("Open").build())
                .build()
        );

        assertFalse(GuiRendererSelectionSupport.prefersDialogRenderer(gui));
    }

    @Test
    void prefersDialogForLinearLayouts() {
        Gui gui = TestGui.of(
            LinearLayout.builder()
                .element(new TextBuilder().text("Hello").build())
                .build()
        );

        assertTrue(GuiRendererSelectionSupport.prefersDialogRenderer(gui));
    }

    @Test
    void prefersDialogForNativeInputElements() {
        Gui gui = TestGui.of(
            GridLayout.builder(1)
                .slot(0, new InputBuilder().key("name").label("Name").build())
                .build()
        );

        assertTrue(GuiRendererSelectionSupport.prefersDialogRenderer(gui));
    }

    @Test
    void autoRendererSupportsUnionOfDelegates() {
        GuiRenderer autoRenderer = GuiRendererSelectionSupport.autoRenderer(
            "test-auto",
            new StubRenderer("inventory", Set.of(GuiCapability.GRID_LAYOUT)),
            new StubRenderer("dialog", Set.of(GuiCapability.MODAL))
        );

        assertTrue(autoRenderer.supports(GuiCapability.GRID_LAYOUT));
        assertTrue(autoRenderer.supports(GuiCapability.MODAL));
    }

    @Test
    void autoRendererFallsBackWhenDialogIsUnavailableForPlayer() {
        AtomicInteger inventoryRenders = new AtomicInteger();
        AtomicInteger dialogRenders = new AtomicInteger();
        GuiRenderer autoRenderer = GuiRendererSelectionSupport.autoRenderer(
            "test-auto",
            new RecordingRenderer("inventory", Set.of(GuiCapability.GRID_LAYOUT), inventoryRenders),
            new RecordingRenderer("dialog", Set.of(GuiCapability.MODAL), dialogRenders),
            GuiRendererSelectionSupport::prefersDialogRenderer,
            (gui, player) -> false,
            () -> Set.of(GuiCapability.GRID_LAYOUT)
        );

        Gui gui = TestGui.of(
            LinearLayout.builder()
                .element(new TextBuilder().text("Hello").build())
                .build()
        );

        autoRenderer.render(gui, new TestPlayer(), new StubRenderContext(gui));

        assertTrue(inventoryRenders.get() == 1);
        assertTrue(dialogRenders.get() == 0);
        assertFalse(autoRenderer.supports(GuiCapability.MODAL));
    }

    private record StubRenderer(String name, Set<GuiCapability> capabilities) implements GuiRenderer {
        @Override
        public boolean supports(@NotNull GuiCapability capability) {
            return capabilities.contains(capability);
        }

        @Override
        public void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
        }

        @Override
        public void close(@NotNull Gui gui, @NotNull RPlayer player) {
        }
    }

    private record RecordingRenderer(String name, Set<GuiCapability> capabilities, AtomicInteger renderCount) implements GuiRenderer {
        @Override
        public boolean supports(@NotNull GuiCapability capability) {
            return capabilities.contains(capability);
        }

        @Override
        public void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
            renderCount.incrementAndGet();
        }

        @Override
        public void close(@NotNull Gui gui, @NotNull RPlayer player) {
        }
    }

    private record StubRenderContext(@NotNull Gui gui) implements RenderContext {
        @Override
        public @NotNull RPlayer player() {
            return new TestPlayer();
        }

        @Override
        public de.t14d3.rapunzellib.gui.context.GuiState state() {
            return de.t14d3.rapunzellib.gui.context.GuiState.create();
        }

        @Override
        public de.t14d3.rapunzellib.gui.element.GuiElement elementAt(int slot) {
            return null;
        }

        @Override
        public @NotNull java.util.Map<Integer, de.t14d3.rapunzellib.gui.element.GuiElement> elementRegistry() {
            return java.util.Map.of();
        }

        @Override
        public void registerElement(int slot, @NotNull de.t14d3.rapunzellib.gui.element.GuiElement element) {
        }

        @Override
        public void set(@NotNull String key, @NotNull de.t14d3.rapunzellib.gui.value.GuiValue value) {
        }
    }

    private static final class TestPlayer implements RPlayer {
        @Override
        public java.util.@NotNull UUID uuid() {
            return java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        }

        @Override
        public @NotNull String name() {
            return "tester";
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return true;
        }

        @Override
        public net.kyori.adventure.audience.Audience audience() {
            return net.kyori.adventure.audience.Audience.empty();
        }

        @Override
        public @NotNull de.t14d3.rapunzellib.PlatformId platformId() {
            return de.t14d3.rapunzellib.PlatformId.PAPER;
        }

        @Override
        public @NotNull Object handle() {
            return this;
        }
    }

    private record TestGui(de.t14d3.rapunzellib.gui.layout.GuiLayout layout) implements Gui {
        private static TestGui of(de.t14d3.rapunzellib.gui.layout.GuiLayout layout) {
            return new TestGui(layout);
        }

        @Override
        public @NotNull GuiRenderer renderer() {
            return new StubRenderer("test", Set.of());
        }

        @Override
        public net.kyori.adventure.text.Component title() {
            return net.kyori.adventure.text.Component.text("Test");
        }

        @Override
        public int rows() {
            return 1;
        }

        @Override
        public void open(@NotNull RPlayer player) {
        }

        @Override
        public void close(@NotNull RPlayer player) {
        }
    }
}
