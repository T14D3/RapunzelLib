package de.t14d3.rapunzellib.buildlogic;

import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ModuleMatrix {
    public static final String ATTACHMENT_INSTALLER_TYPE = "de.t14d3.rapunzellib.attachments.AttachmentFeatureInstaller";
    public static final String SHARED_CORE_FAMILY_NONE = "none";
    public static final String SHARED_CORE_FAMILY_AUTO = "auto";
    public static final String SHARED_CORE_FAMILY_MOJANG = "shared";

    private static final String PLATFORM_MODULE_PREFIX = "platform-";
    private static final String SHARED_MODULE_SUFFIX = "shared";
    private static final Pattern SHARED_CORE_FAMILY_PATTERN = Pattern.compile("[a-z0-9-]+");
    private static final Set<String> MOJANG_DEFAULT_SHARED_CORE_FEATURES =
        new LinkedHashSet<>(List.of("events", "gui", "inventory", "nbt"));
    private static final Set<String> PAPER_DEFAULT_SHARED_CORE_FEATURES =
        new LinkedHashSet<>(List.of("nbt"));

    public static final List<FeatureModuleSpec> FEATURE_SPECS = List.of(
        new FeatureModuleSpec("commands", "de.t14d3.rapunzellib.commands.CommandFeatureInstaller", Map.of(), false),
        new FeatureModuleSpec(
            "events",
            "de.t14d3.rapunzellib.events.GameEventBridgeInstaller",
            Map.of(SHARED_CORE_FAMILY_MOJANG, ":events-shared"),
            false
        ),
        new FeatureModuleSpec(
            "gui",
            "de.t14d3.rapunzellib.gui.GuiFeatureInstaller",
            Map.of(SHARED_CORE_FAMILY_MOJANG, ":gui-shared"),
            true
        ),
        new FeatureModuleSpec(
            "inventory",
            "de.t14d3.rapunzellib.inventory.InventoryFeatureInstaller",
            Map.of(SHARED_CORE_FAMILY_MOJANG, ":inventory-shared"),
            true
        ),
        new FeatureModuleSpec(
            "nbt",
            "de.t14d3.rapunzellib.nbt.NbtFeatureInstaller",
            Map.of(SHARED_CORE_FAMILY_MOJANG, ":nbt-shared"),
            false
        ),
        new FeatureModuleSpec(
            "visuals",
            "de.t14d3.rapunzellib.visuals.VisualFeatureInstaller",
            Map.of(SHARED_CORE_FAMILY_MOJANG, ":visuals-shared"),
            false
        )
    );

    public static final Set<String> FEATURE_KEYS = linkedSet(FEATURE_SPECS.stream().map(FeatureModuleSpec::featureKey).toList());
    public static final Set<String> MOJANG_PARITY_PLATFORMS = Set.of("fabric", "neoforge");
    public static final Set<String> MOJANG_PLATFORM_KEYS = Set.of("paper", "fabric", "neoforge");
    public static final Map<String, String> PLATFORM_SHARED_CORE_MODULE_BY_FAMILY = Map.of(SHARED_CORE_FAMILY_MOJANG, ":platform-shared");
    public static final Set<String> MOJANG_SHARED_CORE_FEATURE_KEYS = linkedSet(
        FEATURE_SPECS.stream()
            .filter(spec -> spec.sharedCoreModuleByFamily().containsKey(SHARED_CORE_FAMILY_MOJANG))
            .map(FeatureModuleSpec::featureKey)
            .toList()
    );

    private static final Map<String, FeatureModuleSpec> FEATURE_SPECS_BY_KEY;

    static {
        Map<String, FeatureModuleSpec> byKey = new LinkedHashMap<>();
        for (FeatureModuleSpec spec : FEATURE_SPECS) {
            byKey.put(spec.featureKey(), spec);
        }
        FEATURE_SPECS_BY_KEY = Map.copyOf(byKey);
    }

    private ModuleMatrix() {
    }

    public static FeatureModuleSpec featureSpec(String featureKey) {
        FeatureModuleSpec spec = FEATURE_SPECS_BY_KEY.get(featureKey);
        if (spec == null) {
            throw new IllegalArgumentException("Unsupported feature '" + featureKey + "'.");
        }
        return spec;
    }

    public static FeatureModuleId parseFeatureModule(String projectName) {
        int separatorIndex = projectName.lastIndexOf('-');
        if (separatorIndex <= 0 || separatorIndex == projectName.length() - 1) {
            return null;
        }

        String featureKey = projectName.substring(0, separatorIndex);
        String platformKey = projectName.substring(separatorIndex + 1);
        if (!FEATURE_KEYS.contains(featureKey) || SHARED_MODULE_SUFFIX.equals(platformKey)) {
            return null;
        }

        return new FeatureModuleId(featureKey, platformKey);
    }

    public static String parsePlatformModule(String projectName) {
        if (!projectName.startsWith(PLATFORM_MODULE_PREFIX) || (PLATFORM_MODULE_PREFIX + "shared").equals(projectName)) {
            return null;
        }
        String platformKey = projectName.substring(PLATFORM_MODULE_PREFIX.length());
        return (!platformKey.isBlank() && !SHARED_MODULE_SUFFIX.equals(platformKey)) ? platformKey : null;
    }

    public static List<InstallerExpectation> installerExpectations(Project rootProject) {
        List<InstallerExpectation> expectations = new ArrayList<>();
        for (Project subproject : rootProject.getSubprojects()) {
            InstallerExpectation expectation = installerExpectationForProject(subproject.getName());
            if (expectation != null) {
                expectations.add(expectation);
            }
        }
        return expectations;
    }

    public static InstallerExpectation installerExpectationForProject(String projectName) {
        String platformKey = parsePlatformModule(projectName);
        if (platformKey != null) {
            return new InstallerExpectation(projectName, ATTACHMENT_INSTALLER_TYPE);
        }

        FeatureModuleId featureModule = parseFeatureModule(projectName);
        if (featureModule == null) {
            return null;
        }
        return new InstallerExpectation(projectName, featureSpec(featureModule.featureKey()).installerType());
    }

    public static String normalizeSharedCoreFamily(String rawValue, String platformKey) {
        String normalized = rawValue.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case SHARED_CORE_FAMILY_AUTO ->
                MOJANG_PLATFORM_KEYS.contains(platformKey) ? SHARED_CORE_FAMILY_MOJANG : SHARED_CORE_FAMILY_NONE;
            case "", SHARED_CORE_FAMILY_NONE -> SHARED_CORE_FAMILY_NONE;
            case SHARED_CORE_FAMILY_MOJANG -> SHARED_CORE_FAMILY_MOJANG;
            default -> {
                if (!SHARED_CORE_FAMILY_PATTERN.matcher(normalized).matches()) {
                    throw new IllegalArgumentException(
                        "Invalid shared-core family '" + rawValue + "'. Use lowercase letters, digits, and dashes only."
                    );
                }
                yield normalized;
            }
        };
    }

    public static Set<String> defaultSharedCoreFeatures(String sharedCoreFamily, String platformKey) {
        if (!Objects.equals(sharedCoreFamily, SHARED_CORE_FAMILY_MOJANG)) {
            return Set.of();
        }
        return switch (platformKey) {
            case "paper" -> PAPER_DEFAULT_SHARED_CORE_FEATURES;
            case "fabric", "neoforge" -> MOJANG_DEFAULT_SHARED_CORE_FEATURES;
            default -> MOJANG_SHARED_CORE_FEATURE_KEYS;
        };
    }

    public static String sharedCoreModuleForPlatformFamily(String sharedCoreFamily) {
        if (Objects.equals(sharedCoreFamily, SHARED_CORE_FAMILY_NONE)) {
            return null;
        }
        return PLATFORM_SHARED_CORE_MODULE_BY_FAMILY.getOrDefault(sharedCoreFamily, ":platform-" + sharedCoreFamily);
    }

    public static String sharedCoreModuleForFeature(String featureKey, String sharedCoreFamily) {
        if (Objects.equals(sharedCoreFamily, SHARED_CORE_FAMILY_NONE)) {
            return null;
        }
        return featureSpec(featureKey).sharedCoreModuleByFamily().getOrDefault(sharedCoreFamily, ":" + featureKey + "-" + sharedCoreFamily);
    }

    private static <T> LinkedHashSet<T> linkedSet(List<T> values) {
        return new LinkedHashSet<>(values);
    }

    public record InstallerExpectation(String moduleName, String installerType) {
    }

    public record FeatureModuleSpec(
        String featureKey,
        String installerType,
        Map<String, String> sharedCoreModuleByFamily,
        boolean additionalPlatformModuleDependency
    ) {
    }

    public record FeatureModuleId(String featureKey, String platformKey) {
    }
}
