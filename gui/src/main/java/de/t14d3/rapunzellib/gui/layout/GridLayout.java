package de.t14d3.rapunzellib.gui.layout;

import de.t14d3.rapunzellib.gui.element.GuiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface GridLayout extends GuiLayout {
    int rows();
    
    int columns();
    
    @NotNull Map<Integer, GuiElement> slots();
    
    default int totalSlots() {
        return rows() * columns();
    }
    
    @Override
    default @NotNull List<GuiElement> elements() {
        List<GuiElement> elements = new ArrayList<>(Collections.nCopies(totalSlots(), null));
        slots().forEach(elements::set);
        return elements;
    }
    
    @Override
    default @NotNull List<GuiElement> elementsAt(int slot) {
        GuiElement element = slots().get(slot);
        return element != null ? List.of(element) : Collections.emptyList();
    }
    
    default @NotNull Optional<GuiElement> elementAt(int row, int column) {
        return Optional.ofNullable(slots().get(row * columns() + column));
    }
    
    default @NotNull Optional<GuiElement> elementAt(int slot) {
        return Optional.ofNullable(slots().get(slot));
    }
    
    @NotNull
    static Builder builder() {
        return new Builder();
    }
    
    @NotNull
    static Builder builder(int rows) {
        return new Builder().rows(rows);
    }
    
    @NotNull
    static Builder builder(int rows, int columns) {
        return new Builder().rows(rows).columns(columns);
    }
    
    class Builder {
        private int rows = 6;
        private int columns = 9;
        private final Map<Integer, GuiElement> slots = new HashMap<>();
        
        @NotNull
        public Builder rows(int rows) {
            if (rows < 1 || rows > 6) {
                throw new IllegalArgumentException("rows must be between 1 and 6 but was " + rows);
            }
            this.rows = rows;
            return this;
        }
        
        @NotNull
        public Builder columns(int columns) {
            if (columns < 1) {
                throw new IllegalArgumentException("columns must be greater than 0 but was " + columns);
            }
            this.columns = columns;
            return this;
        }
        
        @NotNull
        public Builder size(int rows, int columns) {
            return rows(rows).columns(columns);
        }
        
        @NotNull
        public Builder slot(int slot, @NotNull GuiElement element) {
            int capacity = rows * columns;
            if (slot < 0 || slot >= capacity) {
                throw new IllegalArgumentException("slot must be between 0 and " + (capacity - 1) + " but was " + slot);
            }
            this.slots.put(slot, element);
            return this;
        }
        
        @NotNull
        public Builder slot(int row, int column, @NotNull GuiElement element) {
            return slot(row * columns + column, element);
        }
        
        @NotNull
        public Builder row(int row, @NotNull GuiElement... elements) {
            int startSlot = row * columns;
            for (int i = 0; i < elements.length && i < columns; i++) {
                slots.put(startSlot + i, elements[i]);
            }
            return this;
        }
        
        @NotNull
        public Builder fill(@NotNull GuiElement element) {
            for (int i = 0; i < rows * columns; i++) {
                if (!slots.containsKey(i)) {
                    slots.put(i, element);
                }
            }
            return this;
        }
        
        @NotNull
        public Builder fillRange(int startSlot, int endSlot, @NotNull GuiElement element) {
            if (startSlot < 0 || endSlot < startSlot || endSlot > rows * columns) {
                throw new IllegalArgumentException(
                    "fill range must stay within 0.." + (rows * columns) + " but was " + startSlot + ".." + endSlot
                );
            }
            for (int i = startSlot; i < endSlot; i++) {
                if (!slots.containsKey(i)) {
                    slots.put(i, element);
                }
            }
            return this;
        }
        
        @NotNull
        public Builder fillRow(int row, @NotNull GuiElement element) {
            int startSlot = row * columns;
            fillRange(startSlot, startSlot + columns, element);
            return this;
        }
        
        @NotNull
        public Builder border(@NotNull GuiElement element) {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    if (row == 0 || row == rows - 1 || col == 0 || col == columns - 1) {
                        int slot = row * columns + col;
                        if (!slots.containsKey(slot)) {
                            slots.put(slot, element);
                        }
                    }
                }
            }
            return this;
        }
        
        @NotNull
        public GridLayout build() {
            int finalRows = rows;
            int finalColumns = columns;
            Map<Integer, GuiElement> finalSlots = Map.copyOf(slots);
            
            return new GridLayout() {
                @Override
                public int rows() {
                    return finalRows;
                }
                
                @Override
                public int columns() {
                    return finalColumns;
                }
                
                @Override
                public @NotNull Map<Integer, GuiElement> slots() {
                    return finalSlots;
                }
            };
        }
    }
}
