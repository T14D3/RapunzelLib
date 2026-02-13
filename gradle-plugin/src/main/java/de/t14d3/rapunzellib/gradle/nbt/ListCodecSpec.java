package de.t14d3.rapunzellib.gradle.nbt;

public record ListCodecSpec(CodecSpec elementCodec) implements CodecSpec {
    @Override
    public String javaType() {
        return "List<" + elementCodec.javaType() + ">";
    }

    @Override
    public String codecExpression() {
        return "RNbtCodecs.listOf(" + elementCodec.codecExpression() + ")";
    }

    @Override
    public boolean requiresListImport() {
        return true;
    }

    @Override
    public boolean requiresCompoundImport() {
        return elementCodec.requiresCompoundImport();
    }

    @Override
    public boolean requiresValueImport() {
        return elementCodec.requiresValueImport();
    }

    @Override
    public boolean requiresComponentImport() {
        return elementCodec.requiresComponentImport();
    }
}
