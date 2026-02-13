package de.t14d3.rapunzellib.gui.element;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PaginationBuilder {
    private int currentPage = 1;
    private int totalPages = 1;
    private Consumer<Integer> onPageChange;
    
    @NotNull
    public PaginationBuilder currentPage(int currentPage) {
        this.currentPage = currentPage;
        return this;
    }
    
    @NotNull
    public PaginationBuilder totalPages(int totalPages) {
        this.totalPages = totalPages;
        return this;
    }
    
    @NotNull
    public PaginationBuilder pages(int current, int total) {
        this.currentPage = current;
        this.totalPages = total;
        return this;
    }
    
    @NotNull
    public PaginationBuilder onPageChange(@NotNull Consumer<Integer> onPageChange) {
        this.onPageChange = onPageChange;
        return this;
    }
    
    @NotNull
    public PaginationElement build() {
        if (onPageChange == null) {
            throw new IllegalStateException("onPageChange handler is required");
        }
        
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
}
