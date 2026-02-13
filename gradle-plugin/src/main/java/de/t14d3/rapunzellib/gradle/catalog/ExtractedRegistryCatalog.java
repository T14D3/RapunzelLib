package de.t14d3.rapunzellib.gradle.catalog;

import java.util.List;

public record ExtractedRegistryCatalog(String description, List<NamespacedKeyEntry> keys) {
}
