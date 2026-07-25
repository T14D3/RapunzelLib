package de.t14d3.rapunzellib.buildlogic.catalog;

import de.t14d3.rapunzellib.gradle.RegistryCatalogSpec.RegistryCatalogSourceSpec;
import de.t14d3.rapunzellib.gradle.catalog.ExtractedRegistryCatalog;
import de.t14d3.rapunzellib.gradle.catalog.RegistryCatalogSourceExtractor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public record RegistryCatalogSourceDefinition(
    String name,
    String sourceType,
    List<File> nativeSourceClasspath,
    String normalizationProfile,
    boolean allowSupersetOfCanonical,
    String nativeEnumClassName,
    String nativeStaticFieldOwnerClassName,
    String nativeStaticFieldValueTypeName,
    List<String> nativeIncludePredicateMethods,
    List<String> nativeExcludePredicateMethods,
    String nativeKeyAccessorMethodName,
    Set<String> nativeExcludedEnumConstants
) {
    private static final String FIELD_SEPARATOR = "|";
    private static final String LIST_SEPARATOR = ",";

    public ExtractedRegistryCatalog extract() {
        return RegistryCatalogSourceExtractor.extract(
            sourceType,
            nativeSourceClasspath,
            normalizationProfile,
            nativeEnumClassName,
            nativeStaticFieldOwnerClassName,
            nativeStaticFieldValueTypeName,
            nativeIncludePredicateMethods,
            nativeExcludePredicateMethods,
            nativeKeyAccessorMethodName,
            nativeExcludedEnumConstants
        );
    }

    public String encode() {
        return String.join(
            FIELD_SEPARATOR,
            encodeValue(name),
            encodeValue(sourceType),
            encodeList(nativeSourceClasspath.stream().map(File::getAbsolutePath).toList()),
            encodeValue(normalizationProfile),
            encodeValue(Boolean.toString(allowSupersetOfCanonical)),
            encodeValue(nativeEnumClassName),
            encodeValue(nativeStaticFieldOwnerClassName),
            encodeValue(nativeStaticFieldValueTypeName),
            encodeList(nativeIncludePredicateMethods),
            encodeList(nativeExcludePredicateMethods),
            encodeValue(nativeKeyAccessorMethodName),
            encodeList(new TreeSet<>(nativeExcludedEnumConstants).stream().toList())
        );
    }

    public static RegistryCatalogSourceDefinition fromSpec(String name, RegistryCatalogSourceSpec spec) {
        return new RegistryCatalogSourceDefinition(
            name,
            spec.getType().get(),
            spec.getClasspath().getFiles().stream().sorted(java.util.Comparator.comparing(File::getAbsolutePath)).toList(),
            spec.getNormalizationProfile().get(),
            spec.getAllowSupersetOfCanonical().get(),
            spec.getEnumClassName().get(),
            spec.getStaticFieldOwnerClassName().get(),
            spec.getStaticFieldValueTypeName().get(),
            spec.getIncludePredicateMethods().get(),
            spec.getExcludePredicateMethods().get(),
            spec.getKeyAccessorMethodName().get(),
            spec.getExcludedEnumConstants().get()
        );
    }

    public static RegistryCatalogSourceDefinition decode(String encoded) {
        String[] fields = encoded.split("\\|", -1);
        if (fields.length != 12) {
            throw new IllegalArgumentException("Invalid registry catalog source definition encoding.");
        }
        return new RegistryCatalogSourceDefinition(
            decodeValue(fields[0]),
            decodeValue(fields[1]),
            decodeList(fields[2]).stream().map(File::new).toList(),
            decodeValue(fields[3]),
            Boolean.parseBoolean(decodeValue(fields[4])),
            decodeValue(fields[5]),
            decodeValue(fields[6]),
            decodeValue(fields[7]),
            decodeList(fields[8]),
            decodeList(fields[9]),
            decodeValue(fields[10]),
            new TreeSet<>(decodeList(fields[11]))
        );
    }

    private static String encodeValue(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeValue(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String encodeList(List<String> values) {
        return values.stream().map(RegistryCatalogSourceDefinition::encodeValue).collect(java.util.stream.Collectors.joining(LIST_SEPARATOR));
    }

    private static List<String> decodeList(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(",", -1)).map(RegistryCatalogSourceDefinition::decodeValue).toList();
    }
}
