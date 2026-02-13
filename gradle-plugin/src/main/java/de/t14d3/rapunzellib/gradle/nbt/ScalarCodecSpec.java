package de.t14d3.rapunzellib.gradle.nbt;

public record ScalarCodecSpec(ScalarCodecKind kind) implements CodecSpec {
    @Override
    public String javaType() {
        return kind.javaType();
    }

    @Override
    public String codecExpression() {
        return kind.codecExpression();
    }

    @Override
    public boolean requiresListImport() {
        return false;
    }

    @Override
    public boolean requiresCompoundImport() {
        return kind == ScalarCodecKind.COMPOUND;
    }

    @Override
    public boolean requiresValueImport() {
        return kind == ScalarCodecKind.VALUE;
    }

    @Override
    public boolean requiresComponentImport() {
        return kind == ScalarCodecKind.COMPONENT;
    }
}
