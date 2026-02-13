package de.t14d3.rapunzellib.gradle.catalog;

public record NamespacedKeyEntry(String namespace, String path) {
    public String value() {
        return namespace + ":" + path;
    }
}
