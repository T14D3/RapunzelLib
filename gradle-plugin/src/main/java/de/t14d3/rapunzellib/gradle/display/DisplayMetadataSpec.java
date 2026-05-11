package de.t14d3.rapunzellib.gradle.display;

import java.util.List;

public record DisplayMetadataSpec(
    String className,
    List<DisplayFieldSpec> fields,
    List<ByteFlagSpec> byteFlags
) {
    public record ByteFlagSpec(String name, int bit) {
    }
}
