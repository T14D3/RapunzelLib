package de.t14d3.rapunzellib.gradle.nbt;

import java.util.ArrayList;
import java.util.List;

public record RNbtSchemaSpec(String name, List<RNbtSchemaEntrySpec> entries) {
    public List<RNbtFlattenedEntrySpec> flattenedEntries() {
        List<RNbtFlattenedEntrySpec> flattened = new ArrayList<>();
        for (RNbtSchemaEntrySpec entry : entries) {
            flattened.addAll(flatten(entry, List.of()));
        }
        return flattened;
    }

    public boolean requiresListImport() {
        return entries.stream().anyMatch(RNbtSchemaEntrySpec::requiresListImport);
    }

    public boolean requiresCompoundImport() {
        return entries.stream().anyMatch(RNbtSchemaEntrySpec::requiresCompoundImport);
    }

    public boolean requiresValueImport() {
        return entries.stream().anyMatch(RNbtSchemaEntrySpec::requiresValueImport);
    }

    public boolean requiresComponentImport() {
        return entries.stream().anyMatch(RNbtSchemaEntrySpec::requiresComponentImport);
    }

    private static List<RNbtFlattenedEntrySpec> flatten(RNbtSchemaEntrySpec entry, List<String> parentSegments) {
        List<String> currentSegments = new ArrayList<>(parentSegments);
        currentSegments.add(entry.key());
        List<RNbtFlattenedEntrySpec> flattened = new ArrayList<>();
        flattened.add(new RNbtFlattenedEntrySpec(List.copyOf(currentSegments), entry.codec()));
        for (RNbtSchemaEntrySpec child : entry.children()) {
            flattened.addAll(flatten(child, currentSegments));
        }
        return flattened;
    }
}
