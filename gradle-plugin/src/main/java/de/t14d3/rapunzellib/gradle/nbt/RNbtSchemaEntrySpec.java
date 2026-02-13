package de.t14d3.rapunzellib.gradle.nbt;

import java.util.List;

public record RNbtSchemaEntrySpec(String key, CodecSpec codec, List<RNbtSchemaEntrySpec> children) {
    public boolean requiresListImport() {
        return codec.requiresListImport() || children.stream().anyMatch(RNbtSchemaEntrySpec::requiresListImport);
    }

    public boolean requiresCompoundImport() {
        return codec.requiresCompoundImport() || children.stream().anyMatch(RNbtSchemaEntrySpec::requiresCompoundImport);
    }

    public boolean requiresValueImport() {
        return codec.requiresValueImport() || children.stream().anyMatch(RNbtSchemaEntrySpec::requiresValueImport);
    }

    public boolean requiresComponentImport() {
        return codec.requiresComponentImport() || children.stream().anyMatch(RNbtSchemaEntrySpec::requiresComponentImport);
    }
}
