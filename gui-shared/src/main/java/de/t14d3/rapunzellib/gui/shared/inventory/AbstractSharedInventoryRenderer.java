package de.t14d3.rapunzellib.gui.shared.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import de.t14d3.rapunzellib.events.inventory.InventoryEventBridge;
import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiCloseHooks;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.gui.context.ClickType;
import de.t14d3.rapunzellib.gui.context.CloseReason;
import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.core.GuiChildTransitions;
import de.t14d3.rapunzellib.gui.core.GuiContexts;
import de.t14d3.rapunzellib.gui.core.GuiInventoryElementHandler;
import de.t14d3.rapunzellib.gui.core.GuiSessionStore;
import de.t14d3.rapunzellib.gui.core.GuiSlotPlan;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.inventory.GuiInventoryClickTypes;
import de.t14d3.rapunzellib.gui.shared.SharedGuiClickTypes;
import de.t14d3.rapunzellib.gui.shared.SharedGuiComponents;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Abstract base for platform-specific inventory GUI renderers.
 * <p>
 * Renders a GUI as a Minecraft chest menu with slot-based elements.
 * Supports child dialogs for dropdown selection and text input using
 * separate chest menus and anvil menus respectively.
 */
public abstract class AbstractSharedInventoryRenderer implements GuiRenderer {
    private static final MenuType<?>[] CHEST_MENUS = {
        MenuType.GENERIC_9x1,
        MenuType.GENERIC_9x2,
        MenuType.GENERIC_9x3,
        MenuType.GENERIC_9x4,
        MenuType.GENERIC_9x5,
        MenuType.GENERIC_9x6,
    };

    private final String name;
    private final Supplier<? extends ItemStackAdapter<ItemStack>> itemAdapterSupplier;
    private final GuiSessionStore<OpenGuiSession> openGuis = new GuiSessionStore<>();
    private final GuiSessionStore<DropdownSession> activeDropdowns = new GuiSessionStore<>();
    private final GuiSessionStore<InputSession> activeInputs = new GuiSessionStore<>();
    private final GuiChildTransitions childTransitions = new GuiChildTransitions();

    /**
     * Creates an inventory renderer.
     *
     * @param name                 the renderer name
     * @param itemAdapterSupplier  supplier for the item stack adapter
     */
    protected AbstractSharedInventoryRenderer(
        @NotNull String name,
        @NotNull Supplier<? extends ItemStackAdapter<ItemStack>> itemAdapterSupplier
    ) {
        this.name = name;
        this.itemAdapterSupplier = itemAdapterSupplier;
    }

    @Override
    public final @NotNull String name() {
        return name;
    }

    @Override
    public final @NotNull Set<GuiCapability> capabilities() {
        return GuiRendererSelectionSupport.inventoryCapabilities();
    }

    @Override
    public final boolean supports(@NotNull GuiCapability capability) {
        return capabilities().contains(capability);
    }

    @Override
    public final void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
        ServerPlayer serverPlayer = unwrap(player);
        if (serverPlayer == null) {
            return;
        }

        childTransitions.end(player.uuid());
        GuiSlotPlan slotPlan = GuiSlotPlan.resolve(gui, 6);
        SimpleContainer container = buildContainer(slotPlan, context);
        RInventory wrappedContainer = wrapInventory(container);
        Component title = gui.title() != null ? gui.title() : Component.text("GUI");

        if (!InventoryEventBridge.dispatchOpenPre(player, wrappedContainer)) {
            return;
        }

        OptionalInt containerId = serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (syncId, playerInventory, nativePlayer) -> new RootMenu(syncId, playerInventory, container, slotPlan.rows(), player.uuid()),
            SharedGuiComponents.toNative(title)
        ));
        if (containerId.isEmpty()) {
            return;
        }

        openGuis.put(player.uuid(), new OpenGuiSession(gui, context, container, slotPlan));
        InventoryEventBridge.dispatchOpen(player, wrappedContainer);
    }

    @Override
    public final void close(@NotNull Gui gui, @NotNull RPlayer player) {
        ServerPlayer serverPlayer = unwrap(player);
        if (serverPlayer != null) {
            serverPlayer.closeContainer();
        }
        clearPlayerState(player.uuid());
    }

    /**
     * Unwraps an RPlayer to a native ServerPlayer.
     *
     * @param player the Rapunzel player
     * @return the native server player, or {@code null}
     */
    protected abstract @Nullable ServerPlayer unwrap(@NotNull RPlayer player);

    /**
     * Builds a SimpleContainer with rendered GUI elements according to the slot plan.
     *
     * @param slotPlan the slot plan
     * @param context  the render context
     * @return the container with rendered elements
     */
    private @NotNull SimpleContainer buildContainer(@NotNull GuiSlotPlan slotPlan, @NotNull RenderContext context) {
        SharedInventoryElementRenderer elementRenderer = elementRenderer();
        SimpleContainer container = new SimpleContainer(slotPlan.size());
        for (Map.Entry<Integer, GuiElement> entry : slotPlan.slots().entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= slotPlan.size()) {
                continue;
            }
            GuiElement element = entry.getValue();
            container.setItem(slot, elementRenderer.render(element, context));
            context.registerElement(slot, element);
        }
        return container;
    }

    /**
     * Handles a click on the root menu.
     *
     * @param playerId  the player UUID
     * @param container the container
     * @param slot      the slot index
     * @param button    the button index
     * @param clickType the native click type
     */
    private void handleRootClick(
        @NotNull UUID playerId,
        @NotNull SimpleContainer container,
        int slot,
        int button,
        // #if VERSION >= 26.0.0
        @NotNull net.minecraft.world.inventory.ContainerInput clickType
        // #else
        // # @NotNull net.minecraft.world.inventory.ClickType clickType
        // #endif
    ) {
        OpenGuiSession openGui = openGuis.get(playerId);
        if (openGui == null) {
            return;
        }

        InventoryClickType eventClickType = SharedGuiClickTypes.mapMenuClick(clickType, button);
        ClickType mappedClickType = GuiInventoryClickTypes.fromEventClickType(eventClickType);
        RPlayer player = openGui.context.player();
        RInventory wrappedContainer = wrapInventory(container);

        InventoryEventBridge.ClickDispatch clickDispatch = InventoryEventBridge.dispatchClick(player, wrappedContainer, slot, eventClickType);
        try {
            if (clickDispatch.cancelled()) {
                return;
            }

            GuiElement element = openGui.context.elementAt(slot);
            if (element == null) {
                return;
            }

            ClickContext clickContext = GuiContexts.click(player, element, slot, mappedClickType, openGui.context.state());
            GuiInventoryElementHandler.Result result = GuiInventoryElementHandler.handle(
                element,
                clickContext,
                input -> openInputSession(playerId, input, openGui),
                dropdown -> openDropdownSession(playerId, dropdown, openGui)
            );

            onElementClicked(player, element, clickContext, result);

            if (result.stateMutated()) {
                container.setItem(slot, elementRenderer().render(element, openGui.context));
                ServerPlayer serverPlayer = unwrap(player);
                if (serverPlayer != null) {
                    serverPlayer.containerMenu.broadcastChanges();
                }
            }
        } finally {
            clickDispatch.post();
        }
    }

    /**
     * Hook invoked after a click on a GUI element has been dispatched but before
     * state-mutation re-rendering. Platform-specific subclasses may override this
     * to play feedback sounds or perform other side effects.
     * <p>
     * The default implementation is a no-op.
     *
     * @param player       the clicking player
     * @param element      the clicked element
     * @param clickContext the click context
     * @param result       the element-handler result
     */
    protected void onElementClicked(
        @NotNull RPlayer player,
        @NotNull GuiElement element,
        @NotNull ClickContext clickContext,
        @NotNull GuiInventoryElementHandler.Result result
    ) {
    }

    /**
     * Opens a dropdown selection menu as a child of the parent GUI.
     *
     * @param playerId the player UUID
     * @param element  the dropdown element
     * @param parent   the parent GUI session
     */
    private void openDropdownSession(
        @NotNull UUID playerId,
        @NotNull DropdownElement element,
        @NotNull OpenGuiSession parent
    ) {
        ServerPlayer serverPlayer = unwrap(parent.context.player());
        if (serverPlayer == null || element.options().isEmpty() || !childTransitions.begin(playerId)) {
            return;
        }

        int rows = Math.max(1, Math.min(6, (element.options().size() + 8) / 9));
        SimpleContainer container = new SimpleContainer(rows * 9);
        Map<Integer, Option> options = new LinkedHashMap<>();
        String selectedId = parent.context.state().get(
            element.key(),
            String.class,
            element.defaultValue() != null ? element.defaultValue().id() : ""
        );

        for (int i = 0; i < element.options().size() && i < container.getContainerSize(); i++) {
            Option option = element.options().get(i);
            container.setItem(i, elementRenderer().renderDropdownOption(option, option.id().equals(selectedId)));
            options.put(i, option);
        }

        DropdownSession session = new DropdownSession(parent, element, container, options);
        activeDropdowns.put(playerId, session);
        RInventory wrappedContainer = wrapInventory(container);

        String title = element.label() != null ? SharedGuiComponents.plain(element.label()) : "Select Option";
        if (!InventoryEventBridge.dispatchOpenPre(parent.context.player(), wrappedContainer)) {
            activeDropdowns.remove(playerId);
            childTransitions.end(playerId);
            return;
        }

        OptionalInt containerId = serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (syncId, playerInventory, nativePlayer) -> new DropdownMenu(syncId, playerInventory, container, rows, playerId),
            net.minecraft.network.chat.Component.literal(title)
        ));
        if (containerId.isEmpty()) {
            activeDropdowns.remove(playerId);
            childTransitions.end(playerId);
            return;
        }

        InventoryEventBridge.dispatchOpen(parent.context.player(), wrappedContainer);
    }

    /**
     * Opens an anvil-based text input menu as a child of the parent GUI.
     *
     * @param playerId the player UUID
     * @param element  the input element
     * @param parent   the parent GUI session
     */
    private void openInputSession(
        @NotNull UUID playerId,
        @NotNull InputElement element,
        @NotNull OpenGuiSession parent
    ) {
        ServerPlayer serverPlayer = unwrap(parent.context.player());
        if (serverPlayer == null || !childTransitions.begin(playerId)) {
            return;
        }

        String title = element.label() != null ? SharedGuiComponents.plain(element.label()) : "Enter Text";
        InputSession session = new InputSession(parent, element);
        activeInputs.put(playerId, session);

        OptionalInt containerId = serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (syncId, playerInventory, nativePlayer) -> {
                InputMenu menu = new InputMenu(syncId, playerInventory, playerId, element, parent.context.state());
                if (!InventoryEventBridge.dispatchOpenPre(parent.context.player(), wrapInventory(menu))) {
                    return null;
                }
                return menu;
            },
            net.minecraft.network.chat.Component.literal(title)
        ));
        if (containerId.isEmpty()) {
            activeInputs.remove(playerId);
            childTransitions.end(playerId);
            return;
        }

        InventoryEventBridge.dispatchOpen(parent.context.player(), wrapInventory(serverPlayer.containerMenu));
    }

    /**
     * Completes a dropdown selection for a player.
     *
     * @param playerId the player UUID
     * @param slot     the selected slot
     */
    private void completeDropdownSelection(@NotNull UUID playerId, int slot) {
        DropdownSession session = activeDropdowns.get(playerId);
        if (session == null) {
            return;
        }
        Option option = session.slotOptions.get(slot);
        if (option == null) {
            return;
        }
        de.t14d3.rapunzellib.gui.core.GuiInteractionEngine.selectDropdown(
            session.element,
            session.parent.context.state(),
            session.parent.context.player(),
            option
        );
        session.completed = true;
    }

    /**
     * Completes a text input for a player.
     *
     * @param playerId the player UUID
     * @param value    the input value
     */
    private void completeInput(@NotNull UUID playerId, @NotNull String value) {
        InputSession session = activeInputs.get(playerId);
        if (session == null) {
            return;
        }
        de.t14d3.rapunzellib.gui.core.GuiInteractionEngine.submitInput(
            session.element,
            session.parent.context.state(),
            session.parent.context.player(),
            value
        );
        session.completed = true;
    }

    /**
     * Re-opens the parent GUI after a child dialog (dropdown or input) is closed.
     *
     * @param playerId the player UUID
     */
    private void reopenParent(@NotNull UUID playerId) {
        OpenGuiSession parent = null;
        DropdownSession dropdownSession = activeDropdowns.remove(playerId);
        if (dropdownSession != null) {
            parent = dropdownSession.parent;
        }
        InputSession inputSession = activeInputs.remove(playerId);
        if (inputSession != null) {
            parent = inputSession.parent;
        }
        childTransitions.end(playerId);
        if (parent == null) {
            return;
        }

        ServerPlayer serverPlayer = unwrap(parent.context.player());
        if (serverPlayer != null) {
            parent.gui.open(parent.context.player());
        }
    }

    /**
     * Clears all session state for a player.
     *
     * @param playerId the player UUID
     */
    private void clearPlayerState(@NotNull UUID playerId) {
        openGuis.remove(playerId);
        activeDropdowns.remove(playerId);
        activeInputs.remove(playerId);
        childTransitions.end(playerId);
    }

    /**
     * Root chest menu that handles element click dispatch.
     */
    private final class RootMenu extends ChestMenu {
        private final UUID playerId;
        private final SimpleContainer topInventory;

        private RootMenu(int containerId, Inventory playerInventory, SimpleContainer container, int rows, UUID playerId) {
            super(CHEST_MENUS[rows - 1], containerId, playerInventory, container, rows);
            this.playerId = playerId;
            this.topInventory = container;
        }

        @Override
        public void clicked(
                int slot,
                int button,
                // #if VERSION >= 26.0.0
                @NotNull net.minecraft.world.inventory.ContainerInput clickType,
                // #else
                // # @NotNull net.minecraft.world.inventory.ClickType clickType,
                // #endif
                Player player) {
            if (slot >= 0 && slot < topInventory.getContainerSize()) {
                handleRootClick(playerId, topInventory, slot, button, clickType);
                return;
            }
            super.clicked(slot, button, clickType, player);
        }

        @Override
        public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void removed(@NotNull Player player) {
            RPlayer rPlayer = openGuis.get(playerId) != null ? openGuis.get(playerId).context.player() : null;
            if (rPlayer != null) {
                InventoryEventBridge.dispatchClose(rPlayer, wrapInventory(topInventory));
                OpenGuiSession openGui = openGuis.get(playerId);
                if (openGui != null) {
                    GuiCloseHooks.close(openGui.gui(), rPlayer, CloseReason.UNKNOWN);
                }
            }
            openGuis.remove(playerId);
            super.removed(player);
        }
    }

    /**
     * Dropdown selection chest menu.
     */
    private final class DropdownMenu extends ChestMenu {
        private final UUID playerId;
        private final SimpleContainer topInventory;

        private DropdownMenu(int containerId, Inventory playerInventory, SimpleContainer container, int rows, UUID playerId) {
            super(CHEST_MENUS[rows - 1], containerId, playerInventory, container, rows);
            this.playerId = playerId;
            this.topInventory = container;
        }

        @Override
        public void clicked(
                int slot,
                int button,
                // #if VERSION >= 26.0.0
                @NotNull net.minecraft.world.inventory.ContainerInput clickType,
                // #else
                // # @NotNull net.minecraft.world.inventory.ClickType clickType,
                // #endif
                Player player) {
            if (slot >= 0 && slot < topInventory.getContainerSize()) {
                DropdownSession session = activeDropdowns.get(playerId);
                if (session == null) {
                    return;
                }
                InventoryClickType eventClickType = SharedGuiClickTypes.mapMenuClick(clickType, button);
                InventoryEventBridge.ClickDispatch clickDispatch = InventoryEventBridge.dispatchClick(
                    session.parent.context.player(),
                    wrapInventory(topInventory),
                    slot,
                    eventClickType
                );
                try {
                    if (!clickDispatch.cancelled() && session.slotOptions.containsKey(slot)) {
                        completeDropdownSelection(playerId, slot);
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.closeContainer();
                        }
                    }
                } finally {
                    clickDispatch.post();
                }
                return;
            }
            super.clicked(slot, button, clickType, player);
        }

        @Override
        public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void removed(@NotNull Player player) {
            DropdownSession session = activeDropdowns.get(playerId);
            if (session != null) {
                InventoryEventBridge.dispatchClose(session.parent.context.player(), wrapInventory(topInventory));
            }
            super.removed(player);
            reopenParent(playerId);
        }
    }

    /**
     * Anvil-based text input menu.
     */
    private final class InputMenu extends AnvilMenu {
        private final UUID playerId;
        private final InputElement element;

        private InputMenu(int containerId, Inventory playerInventory, UUID playerId, InputElement element, GuiState state) {
            super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
            this.playerId = playerId;
            this.element = element;

            String initialValue = state.get(element.key(), String.class, element.defaultValue() != null ? element.defaultValue() : "");
            ItemStack inputItem = new ItemStack(Items.PAPER);
            inputItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(initialValue));
            this.inputSlots.setItem(0, inputItem);
            this.createResult();
        }

        @Override
        public void createResult() {
            ItemStack inputItem = this.inputSlots.getItem(0);
            if (inputItem.isEmpty()) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                return;
            }

            String inputText = inputItem.getHoverName().getString();
            ItemStack result = inputItem.copy();
            result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(inputText));
            this.resultSlots.setItem(0, result);
        }

        @Override
        public void clicked(
                int slot,
                int button,
                // #if VERSION >= 26.0.0
                @NotNull net.minecraft.world.inventory.ContainerInput clickType,
                // #else
                // # @NotNull net.minecraft.world.inventory.ClickType clickType,
                // #endif
                Player player) {
            InputSession session = activeInputs.get(playerId);
            if (slot >= 0 && slot < 3 && session != null) {
                InventoryClickType eventClickType = SharedGuiClickTypes.mapMenuClick(clickType, button);
                InventoryEventBridge.ClickDispatch clickDispatch = InventoryEventBridge.dispatchClick(
                    session.parent.context.player(),
                    wrapInventory(this),
                    slot,
                    eventClickType
                );
                try {
                    if (clickDispatch.cancelled()) {
                        return;
                    }
                    if (slot == 2) {
                        String inputText = this.resultSlots.getItem(0).getHoverName().getString();
                        completeInput(playerId, inputText);
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.closeContainer();
                        }
                        return;
                    }
                } finally {
                    clickDispatch.post();
                }
            }
            super.clicked(slot, button, clickType, player);
        }

        @Override
        public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void removed(@NotNull Player player) {
            InputSession session = activeInputs.get(playerId);
            if (session != null) {
                InventoryEventBridge.dispatchClose(session.parent.context.player(), wrapInventory(this));
            }
            super.removed(player);
            reopenParent(playerId);
        }
    }

    /**
     * Wraps a native inventory object into an RInventory.
     *
     * @param nativeInventory the native inventory
     * @return the wrapped RInventory
     */
    private static @NotNull RInventory wrapInventory(@NotNull Object nativeInventory) {
        return InventoryFeatures.install().require(nativeInventory);
    }

    /**
     * Creates a new element renderer using the current item adapter.
     *
     * @return the element renderer
     */
    private @NotNull SharedInventoryElementRenderer elementRenderer() {
        return new SharedInventoryElementRenderer(itemAdapterSupplier.get());
    }

    /**
     * Holds the state for an open root GUI session.
     *
     * @param gui     the GUI
     * @param context the render context
     * @param container the container
     * @param slotPlan the slot plan
     */
    private record OpenGuiSession(
        @NotNull Gui gui,
        @NotNull RenderContext context,
        @NotNull SimpleContainer container,
        @NotNull GuiSlotPlan slotPlan
    ) {
    }

    /**
     * Holds the state for an active dropdown selection session.
     */
    private static final class DropdownSession {
        private final OpenGuiSession parent;
        private final DropdownElement element;
        private final SimpleContainer container;
        private final Map<Integer, Option> slotOptions;
        private boolean completed;

        /**
         * Creates a dropdown session.
         *
         * @param parent      the parent GUI session
         * @param element     the dropdown element
         * @param container   the dropdown container
         * @param slotOptions the slot-to-option mapping
         */
        private DropdownSession(
            @NotNull OpenGuiSession parent,
            @NotNull DropdownElement element,
            @NotNull SimpleContainer container,
            @NotNull Map<Integer, Option> slotOptions
        ) {
            this.parent = parent;
            this.element = element;
            this.container = container;
            this.slotOptions = Map.copyOf(slotOptions);
        }
    }

    /**
     * Holds the state for an active text input session.
     */
    private static final class InputSession {
        private final OpenGuiSession parent;
        private final InputElement element;
        private boolean completed;

        /**
         * Creates an input session.
         *
         * @param parent  the parent GUI session
         * @param element the input element
         */
        private InputSession(@NotNull OpenGuiSession parent, @NotNull InputElement element) {
            this.parent = parent;
            this.element = element;
        }
    }
}
