package de.t14d3.rapunzellib.gui.animation;

import de.t14d3.rapunzellib.gui.Gui;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public interface ItemAnimation extends Animation {
    @NotNull List<String> items();
    
    int targetSlot();
    
    @Override
    default void tick(@NotNull Gui gui, int frame) {
        List<String> items = items();
        if (items.isEmpty()) return;
        
        int index = frame % items.size();
        String item = items.get(index);
        onFrame(targetSlot(), item, frame);
    }
    
    void onFrame(int slot, @NotNull String item, int frame);
    
    @NotNull
    static Builder builder() {
        return new Builder();
    }
    
    class Builder {
        private Duration interval = Duration.ofMillis(500);
        private List<String> items = List.of();
        private int slot = 0;
        private Consumer<AnimationFrame> onFrame;
        
        @NotNull
        public Builder interval(@NotNull Duration interval) {
            this.interval = interval;
            return this;
        }
        
        @NotNull
        public Builder intervalMillis(long millis) {
            return interval(Duration.ofMillis(millis));
        }
        
        @NotNull
        public Builder items(@NotNull List<String> items) {
            this.items = items;
            return this;
        }
        
        @NotNull
        public Builder items(@NotNull String... items) {
            this.items = List.of(items);
            return this;
        }
        
        @NotNull
        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }
        
        @NotNull
        public Builder onFrame(@NotNull Consumer<AnimationFrame> onFrame) {
            this.onFrame = onFrame;
            return this;
        }
        
        @NotNull
        public ItemAnimation build() {
            if (items.isEmpty()) {
                throw new IllegalStateException("Items are required");
            }
            
            Duration finalInterval = interval;
            List<String> finalItems = items;
            int finalSlot = slot;
            Consumer<AnimationFrame> finalOnFrame = onFrame;
            
            return new ItemAnimation() {
                @Override
                public @NotNull Duration interval() {
                    return finalInterval;
                }
                
                @Override
                public @NotNull List<String> items() {
                    return finalItems;
                }
                
                @Override
                public int targetSlot() {
                    return finalSlot;
                }
                
                @Override
                public void onFrame(int slot, @NotNull String item, int frame) {
                    if (finalOnFrame != null) {
                        finalOnFrame.accept(new AnimationFrame(slot, item, frame));
                    }
                }
            };
        }
    }
    
    record AnimationFrame(int slot, String item, int frame) {}
}
