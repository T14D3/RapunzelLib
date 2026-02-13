package de.t14d3.rapunzellib.gui.element;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface PaginationElement extends GuiElement {
    int currentPage();
    
    int totalPages();
    
    @NotNull Consumer<Integer> onPageChange();
    
    @Override
    default @NotNull ElementType type() {
        return ElementType.PAGINATION;
    }
    
    @NotNull
    static PaginationElement of(int currentPage, int totalPages, @NotNull Consumer<Integer> onPageChange) {
        return new PaginationElement() {
            @Override
            public int currentPage() {
                return currentPage;
            }
            
            @Override
            public int totalPages() {
                return totalPages;
            }
            
            @Override
            public @NotNull Consumer<Integer> onPageChange() {
                return onPageChange;
            }
        };
    }
    
    @NotNull
    static PaginationBuilder builder() {
        return new PaginationBuilder();
    }
}
