package de.t14d3.rapunzellib.gradle.nbt;

public sealed interface CodecSpec permits ScalarCodecSpec, ListCodecSpec {
    String javaType();

    String codecExpression();

    boolean requiresListImport();

    boolean requiresCompoundImport();

    boolean requiresValueImport();

    boolean requiresComponentImport();
}
