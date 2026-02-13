package de.t14d3.rapunzellib.gradle.nbt;

public enum ScalarCodecKind {
    STRING("String", "RNbtCodecs.STRING"),
    BYTE("Byte", "RNbtCodecs.BYTE"),
    SHORT("Short", "RNbtCodecs.SHORT"),
    INT("Integer", "RNbtCodecs.INT"),
    LONG("Long", "RNbtCodecs.LONG"),
    FLOAT("Float", "RNbtCodecs.FLOAT"),
    DOUBLE("Double", "RNbtCodecs.DOUBLE"),
    BOOLEAN("Boolean", "RNbtCodecs.BOOLEAN"),
    BYTE_ARRAY("byte[]", "RNbtCodecs.BYTE_ARRAY"),
    INT_ARRAY("int[]", "RNbtCodecs.INT_ARRAY"),
    LONG_ARRAY("long[]", "RNbtCodecs.LONG_ARRAY"),
    COMPOUND("RNbtCompound", "RNbtCodecs.COMPOUND"),
    VALUE("RNbtValue", "RNbtCodecs.VALUE"),
    COMPONENT("Component", "RNbtCodecs.COMPONENT");

    private final String javaType;
    private final String codecExpression;

    ScalarCodecKind(String javaType, String codecExpression) {
        this.javaType = javaType;
        this.codecExpression = codecExpression;
    }

    public String javaType() {
        return javaType;
    }

    public String codecExpression() {
        return codecExpression;
    }
}
