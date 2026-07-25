package de.t14d3.rapunzellib.buildlogic.tasks;

import de.t14d3.rapunzellib.buildlogic.ModuleMatrix;
import de.t14d3.rapunzellib.buildlogic.PlatformAdapterScaffoldRenderer;
import de.t14d3.rapunzellib.buildlogic.PlatformAdapterScaffoldSpec;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@DisableCachingByDefault
public abstract class GeneratePlatformAdapterScaffoldTask extends DefaultTask {
    private static final Pattern PLATFORM_KEY_PATTERN = Pattern.compile("[a-z0-9-]+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-zA-Z0-9]+");

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getBasePackage();

    @Input
    public abstract Property<String> getPlatformKey();

    @Input
    public abstract Property<String> getSharedCoreFamily();

    @Input
    public abstract SetProperty<String> getSharedCoreFeatures();

    @Input
    public abstract SetProperty<String> getFeatures();

    @TaskAction
    public void generate() {
        File out = getOutputDir().get().getAsFile();
        out.mkdirs();

        String normalizedPlatformKey = normalizePlatformKey(getPlatformKey().get());
        List<String> normalizedFeatures = normalizeFeatures(getFeatures().get());
        String normalizedSharedCoreFamily = ModuleMatrix.normalizeSharedCoreFamily(getSharedCoreFamily().get(), normalizedPlatformKey);
        PlatformAdapterScaffoldSpec spec = new PlatformAdapterScaffoldSpec(
            getBasePackage().get(),
            normalizedPlatformKey,
            normalizePackageSegment(normalizedPlatformKey),
            toPascalCase(normalizedPlatformKey),
            normalizedFeatures,
            normalizedSharedCoreFamily,
            normalizeSharedCoreFeatures(
                getSharedCoreFeatures().get(),
                normalizedFeatures,
                normalizedSharedCoreFamily,
                normalizedPlatformKey
            )
        );

        for (var generatedFile : PlatformAdapterScaffoldRenderer.render(spec)) {
            write(new File(out, generatedFile.relativePath()), generatedFile.content());
        }
    }

    private String normalizePlatformKey(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!PLATFORM_KEY_PATTERN.matcher(normalized).matches()) {
            throw new GradleException("Invalid platform key '" + value + "'. Use lowercase letters, digits, and dashes only.");
        }
        return normalized;
    }

    private String normalizePackageSegment(String value) {
        String segment = NON_ALNUM.matcher(value).replaceAll("").toLowerCase(Locale.ROOT);
        if (segment.isBlank()) {
            throw new GradleException("Platform key '" + value + "' does not produce a valid Java package segment.");
        }
        return segment;
    }

    private List<String> normalizeFeatures(Set<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = value.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            throw new GradleException("No scaffold features configured. Supported features: " + String.join(", ", ModuleMatrix.FEATURE_KEYS));
        }
        Set<String> unknown = new LinkedHashSet<>(normalized);
        unknown.removeAll(ModuleMatrix.FEATURE_KEYS);
        if (!unknown.isEmpty()) {
            List<String> sorted = new ArrayList<>(unknown);
            sorted.sort(String::compareTo);
            throw new GradleException("Unsupported scaffold feature(s): " + String.join(", ", sorted));
        }

        List<String> ordered = new ArrayList<>();
        for (ModuleMatrix.FeatureModuleSpec spec : ModuleMatrix.FEATURE_SPECS) {
            if (normalized.contains(spec.featureKey())) {
                ordered.add(spec.featureKey());
            }
        }
        return ordered;
    }

    private Set<String> normalizeSharedCoreFeatures(
        Set<String> values,
        List<String> normalizedFeatures,
        String sharedCoreFamily,
        String normalizedPlatformKey
    ) {
        Set<String> defaults = new LinkedHashSet<>(ModuleMatrix.defaultSharedCoreFeatures(sharedCoreFamily, normalizedPlatformKey));
        defaults.retainAll(new LinkedHashSet<>(normalizedFeatures));

        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = value.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            return defaults;
        }

        Set<String> unknown = new LinkedHashSet<>(normalized);
        unknown.removeAll(ModuleMatrix.FEATURE_KEYS);
        if (!unknown.isEmpty()) {
            List<String> sorted = new ArrayList<>(unknown);
            sorted.sort(String::compareTo);
            throw new GradleException("Unsupported scaffold shared-core feature(s): " + String.join(", ", sorted));
        }

        normalized.retainAll(new LinkedHashSet<>(normalizedFeatures));
        return normalized;
    }

    private String toPascalCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (String part : NON_ALNUM.split(value)) {
            if (part.isBlank()) {
                continue;
            }
            String normalized = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(normalized.charAt(0))).append(normalized.substring(1));
        }
        return builder.toString();
    }

    private void write(File file, String content) {
        file.getParentFile().mkdirs();
        try {
            java.nio.file.Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new GradleException("Failed to write scaffold file " + file, ex);
        }
    }
}
