package de.t14d3.rapunzellib.gradle.catalog;

import java.util.List;
import java.util.Map;

public record RegistryCatalogParityResult(
    String catalogName,
    RegistryCatalogSourceDefinition canonicalSource,
    Map<RegistryCatalogSourceDefinition, String> sourceDescriptions,
    int entryCount,
    List<RegistryCatalogSourceDefinition> comparedSources
) {
}
