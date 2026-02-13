package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.gui.element.ElementType;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.layout.LinearLayout;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class GuiRendererSelectionSupport {
    private static final Set<GuiCapability> INVENTORY_CAPABILITIES = Set.of(
        GuiCapability.GRID_LAYOUT,
        GuiCapability.ITEM_DISPLAY,
        GuiCapability.PAGINATION,
        GuiCapability.ANIMATIONS
    );
    private static final Set<GuiCapability> DIALOG_CAPABILITIES = Set.of(
        GuiCapability.NATIVE_TEXT_INPUT,
        GuiCapability.NATIVE_SLIDER,
        GuiCapability.NATIVE_TOGGLE,
        GuiCapability.NATIVE_DROPDOWN,
        GuiCapability.MODAL,
        GuiCapability.SCROLLABLE
    );

    private GuiRendererSelectionSupport() {
    }

    public static @NotNull Set<GuiCapability> inventoryCapabilities() {
        return INVENTORY_CAPABILITIES;
    }

    public static @NotNull Set<GuiCapability> dialogCapabilities() {
        return DIALOG_CAPABILITIES;
    }

    public static boolean prefersDialogRenderer(@NotNull Gui gui) {
        Objects.requireNonNull(gui, "gui");

        Set<GuiCapability> requiredCapabilities = requiredCapabilities(gui);
        return requiredCapabilities.contains(GuiCapability.NATIVE_TEXT_INPUT)
            || requiredCapabilities.contains(GuiCapability.NATIVE_SLIDER)
            || requiredCapabilities.contains(GuiCapability.NATIVE_TOGGLE)
            || requiredCapabilities.contains(GuiCapability.NATIVE_DROPDOWN)
            || requiredCapabilities.contains(GuiCapability.MODAL)
            || requiredCapabilities.contains(GuiCapability.SCROLLABLE);
    }

    public static @NotNull Set<GuiCapability> requiredCapabilities(@NotNull Gui gui) {
        Objects.requireNonNull(gui, "gui");

        EnumSet<GuiCapability> required = EnumSet.noneOf(GuiCapability.class);

        if (gui.layout() instanceof LinearLayout) {
            required.add(GuiCapability.MODAL);
            required.add(GuiCapability.SCROLLABLE);
        } else {
            required.add(GuiCapability.GRID_LAYOUT);
        }

        for (GuiElement element : gui.layout().elements()) {
            if (element == null) {
                continue;
            }

            ElementType type = element.type();
            switch (type) {
                case INPUT -> {
                    required.add(GuiCapability.NATIVE_TEXT_INPUT);
                    required.add(GuiCapability.MODAL);
                }
                case SLIDER -> {
                    required.add(GuiCapability.NATIVE_SLIDER);
                    required.add(GuiCapability.MODAL);
                }
                case TOGGLE -> {
                    required.add(GuiCapability.NATIVE_TOGGLE);
                    required.add(GuiCapability.MODAL);
                }
                case DROPDOWN -> {
                    required.add(GuiCapability.NATIVE_DROPDOWN);
                    required.add(GuiCapability.MODAL);
                }
                case ITEM -> required.add(GuiCapability.ITEM_DISPLAY);
                case PAGINATION -> required.add(GuiCapability.PAGINATION);
                case TEXT, DIVIDER, SPACER -> {
                }
                default -> {
                }
            }
        }

        return Set.copyOf(required);
    }

    public static @NotNull Set<GuiCapability> unionCapabilities(@NotNull GuiRenderer... renderers) {
        Objects.requireNonNull(renderers, "renderers");

        EnumSet<GuiCapability> capabilities = EnumSet.noneOf(GuiCapability.class);
        for (GuiRenderer renderer : renderers) {
            if (renderer == null) {
                continue;
            }
            capabilities.addAll(renderer.capabilities());
        }
        return Set.copyOf(capabilities);
    }

    public static @NotNull GuiRenderer autoRenderer(
        @NotNull String name,
        @NotNull GuiRenderer inventoryRenderer,
        @NotNull GuiRenderer dialogRenderer
    ) {
        return autoRenderer(
            name,
            inventoryRenderer,
            dialogRenderer,
            GuiRendererSelectionSupport::prefersDialogRenderer,
            (gui, player) -> true,
            () -> unionCapabilities(inventoryRenderer, dialogRenderer)
        );
    }

    public static @NotNull GuiRenderer autoRenderer(
        @NotNull String name,
        @NotNull GuiRenderer inventoryRenderer,
        @NotNull GuiRenderer dialogRenderer,
        @NotNull Predicate<Gui> prefersDialogRenderer,
        @NotNull BiPredicate<Gui, RPlayer> dialogAvailability,
        @NotNull Supplier<Set<GuiCapability>> capabilitiesSupplier
    ) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(inventoryRenderer, "inventoryRenderer");
        Objects.requireNonNull(dialogRenderer, "dialogRenderer");
        Objects.requireNonNull(prefersDialogRenderer, "prefersDialogRenderer");
        Objects.requireNonNull(dialogAvailability, "dialogAvailability");
        Objects.requireNonNull(capabilitiesSupplier, "capabilitiesSupplier");

        return new AutoSelectingGuiRenderer(
            name,
            inventoryRenderer,
            dialogRenderer,
            prefersDialogRenderer,
            dialogAvailability,
            capabilitiesSupplier
        );
    }

    private static final class AutoSelectingGuiRenderer implements GuiRenderer {
        private final String name;
        private final GuiRenderer inventoryRenderer;
        private final GuiRenderer dialogRenderer;
        private final Predicate<Gui> prefersDialogRenderer;
        private final BiPredicate<Gui, RPlayer> dialogAvailability;
        private final Supplier<Set<GuiCapability>> capabilitiesSupplier;
        private final Map<UUID, GuiRenderer> activeRenderers = new ConcurrentHashMap<>();

        private AutoSelectingGuiRenderer(
            @NotNull String name,
            @NotNull GuiRenderer inventoryRenderer,
            @NotNull GuiRenderer dialogRenderer,
            @NotNull Predicate<Gui> prefersDialogRenderer,
            @NotNull BiPredicate<Gui, RPlayer> dialogAvailability,
            @NotNull Supplier<Set<GuiCapability>> capabilitiesSupplier
        ) {
            this.name = name;
            this.inventoryRenderer = inventoryRenderer;
            this.dialogRenderer = dialogRenderer;
            this.prefersDialogRenderer = prefersDialogRenderer;
            this.dialogAvailability = dialogAvailability;
            this.capabilitiesSupplier = capabilitiesSupplier;
        }

        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull Set<GuiCapability> capabilities() {
            return capabilitiesSupplier.get();
        }

        @Override
        public boolean supports(@NotNull GuiCapability capability) {
            return capabilities().contains(capability);
        }

        @Override
        public void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
            GuiRenderer renderer = selectRenderer(gui, player);
            activeRenderers.put(player.uuid(), renderer);
            renderer.render(gui, player, context);
        }

        @Override
        public void close(@NotNull Gui gui, @NotNull RPlayer player) {
            GuiRenderer renderer = activeRenderers.remove(player.uuid());
            if (renderer == null) {
                renderer = selectRenderer(gui, player);
            }
            renderer.close(gui, player);
        }

        private @NotNull GuiRenderer selectRenderer(@NotNull Gui gui, @NotNull RPlayer player) {
            if (prefersDialogRenderer.test(gui) && dialogAvailability.test(gui, player)) {
                return dialogRenderer;
            }
            return inventoryRenderer;
        }
    }
}
