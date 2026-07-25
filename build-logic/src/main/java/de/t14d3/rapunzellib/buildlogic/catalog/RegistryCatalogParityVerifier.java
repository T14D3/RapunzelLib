package de.t14d3.rapunzellib.buildlogic.catalog;

import de.t14d3.rapunzellib.gradle.catalog.ExtractedRegistryCatalog;
import de.t14d3.rapunzellib.gradle.catalog.NamespacedKeyEntry;
import org.gradle.api.GradleException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RegistryCatalogParityVerifier {
    private RegistryCatalogParityVerifier() {
    }

    public static RegistryCatalogParityResult verify(String catalogName, List<RegistryCatalogSourceDefinition> sources) {
        if (sources.size() < 2) {
            throw new GradleException("Registry catalog parity verification for '" + catalogName + "' requires at least two sources.");
        }

        Map<RegistryCatalogSourceDefinition, ExtractedRegistryCatalog> extractedBySource = new LinkedHashMap<>();
        for (RegistryCatalogSourceDefinition source : sources) {
            extractedBySource.put(source, source.extract());
        }

        RegistryCatalogSourceDefinition canonicalSource = sources.getFirst();
        Set<NamespacedKeyEntry> canonicalEntries = new LinkedHashSet<>(extractedBySource.get(canonicalSource).keys());
        List<RegistryCatalogParityMismatch> mismatches = new ArrayList<>();

        for (int index = 1; index < sources.size(); index++) {
            RegistryCatalogSourceDefinition candidate = sources.get(index);
            Set<NamespacedKeyEntry> candidateEntries = new LinkedHashSet<>(extractedBySource.get(candidate).keys());
            if (candidateEntries.equals(canonicalEntries)
                || (candidate.allowSupersetOfCanonical() && candidateEntries.containsAll(canonicalEntries))) {
                continue;
            }

            List<NamespacedKeyEntry> missing = canonicalEntries.stream()
                .filter(entry -> !candidateEntries.contains(entry))
                .sorted(Comparator.comparing(NamespacedKeyEntry::namespace).thenComparing(NamespacedKeyEntry::path))
                .toList();
            List<NamespacedKeyEntry> extra = candidateEntries.stream()
                .filter(entry -> !canonicalEntries.contains(entry))
                .sorted(Comparator.comparing(NamespacedKeyEntry::namespace).thenComparing(NamespacedKeyEntry::path))
                .toList();
            mismatches.add(new RegistryCatalogParityMismatch(canonicalSource, candidate, missing, extra));
        }

        if (!mismatches.isEmpty()) {
            throw new GradleException(buildFailureMessage(catalogName, extractedBySource, mismatches));
        }

        Map<RegistryCatalogSourceDefinition, String> descriptions = new LinkedHashMap<>();
        extractedBySource.forEach((source, extracted) -> descriptions.put(source, extracted.description()));
        return new RegistryCatalogParityResult(
            catalogName,
            canonicalSource,
            descriptions,
            canonicalEntries.size(),
            sources.subList(1, sources.size())
        );
    }

    private static String buildFailureMessage(
        String catalogName,
        Map<RegistryCatalogSourceDefinition, ExtractedRegistryCatalog> extractedBySource,
        List<RegistryCatalogParityMismatch> mismatches
    ) {
        StringBuilder message = new StringBuilder("Registry catalog parity verification failed for '")
            .append(catalogName)
            .append("'.\n");
        for (RegistryCatalogParityMismatch mismatch : mismatches) {
            ExtractedRegistryCatalog canonicalExtracted = extractedBySource.get(mismatch.canonicalSource());
            ExtractedRegistryCatalog candidateExtracted = extractedBySource.get(mismatch.candidateSource());
            message.append("- ")
                .append(mismatch.canonicalSource().name()).append(" (")
                .append(canonicalExtracted.keys().size()).append(" keys; ")
                .append(canonicalExtracted.description()).append(") != ")
                .append(mismatch.candidateSource().name()).append(" (")
                .append(candidateExtracted.keys().size()).append(" keys; ")
                .append(candidateExtracted.description()).append(")");
            if (mismatch.candidateSource().allowSupersetOfCanonical()) {
                message.append(" [superset allowed]");
            }
            message.append('\n');
            if (!mismatch.missingFromCandidate().isEmpty()) {
                message.append("  missing from ").append(mismatch.candidateSource().name()).append(": ")
                    .append(renderEntries(mismatch.missingFromCandidate())).append('\n');
            }
            if (!mismatch.extraInCandidate().isEmpty()) {
                message.append("  extra in ").append(mismatch.candidateSource().name()).append(": ")
                    .append(renderEntries(mismatch.extraInCandidate())).append('\n');
            }
        }
        return message.toString().trim();
    }

    private static String renderEntries(List<NamespacedKeyEntry> entries) {
        String preview = entries.stream().limit(20).map(NamespacedKeyEntry::value).collect(java.util.stream.Collectors.joining(", "));
        return entries.size() > 20 ? preview + ", ... (" + entries.size() + " total)" : preview;
    }

    private record RegistryCatalogParityMismatch(
        RegistryCatalogSourceDefinition canonicalSource,
        RegistryCatalogSourceDefinition candidateSource,
        List<NamespacedKeyEntry> missingFromCandidate,
        List<NamespacedKeyEntry> extraInCandidate
    ) {
    }
}
