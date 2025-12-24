package de.t14d3.rapunzellib.objects;

public record RWorldRef(String name, String key) {
    public RWorldRef {
        boolean hasName = name != null && !name.isBlank();
        boolean hasKey = key != null && !key.isBlank();
        if (!hasName && !hasKey) {
            throw new IllegalArgumentException("Either name or key must be provided");
        }
    }

    public String identifier() {
        if (key != null && !key.isBlank()) return key;
        return name;
    }
}

