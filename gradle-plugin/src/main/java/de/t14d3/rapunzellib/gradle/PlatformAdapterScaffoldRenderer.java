package de.t14d3.rapunzellib.gradle;

import org.gradle.api.GradleException;

import java.util.ArrayList;
import java.util.List;

public final class PlatformAdapterScaffoldRenderer {
    private PlatformAdapterScaffoldRenderer() {
    }

    public static List<GeneratedTextFile> render(PlatformAdapterScaffoldSpec spec) {
        List<GeneratedTextFile> files = new ArrayList<>();
        files.addAll(renderPlatformBootstrapModule(spec));
        for (String featureKey : spec.featureKeys()) {
            files.addAll(renderFeatureModule(spec, featureKey));
        }
        files.add(new GeneratedTextFile("snippets/settings.gradle.kts", renderSettingsSnippet(spec)));
        files.add(new GeneratedTextFile("snippets/build.gradle.kts", renderBuildSnippet(spec)));
        files.add(new GeneratedTextFile("snippets/architecture-notes.md", renderArchitectureNotesSnippet(spec)));
        return files;
    }

    private static List<GeneratedTextFile> renderPlatformBootstrapModule(PlatformAdapterScaffoldSpec spec) {
        String modulePath = "platform-" + spec.platformKey();
        String bootstrapClassName = spec.platformClassPrefix() + "RapunzelBootstrap";
        String attachmentClassName = spec.platformClassPrefix() + "AttachmentFeatureInstaller";
        String attachmentPackage = spec.basePackageName() + ".platform." + spec.platformPackageSegment() + ".attachments";

        return List.of(
            new GeneratedTextFile(
                "%s/src/main/java/%s/platform/%s/%s.java".formatted(
                    modulePath,
                    spec.packagePath(),
                    spec.platformPackageSegment(),
                    bootstrapClassName
                ),
                renderBootstrapSource(spec, bootstrapClassName)
            ),
            new GeneratedTextFile(
                "%s/src/main/java/%s/platform/%s/attachments/%s.java".formatted(
                    modulePath,
                    spec.packagePath(),
                    spec.platformPackageSegment(),
                    attachmentClassName
                ),
                renderAttachmentInstallerSource(spec, attachmentClassName, attachmentPackage)
            ),
            new GeneratedTextFile(
                modulePath + "/src/main/resources/META-INF/services/" + ModuleMatrix.ATTACHMENT_INSTALLER_TYPE,
                attachmentPackage + "." + attachmentClassName + "\n"
            )
        );
    }

    private static List<GeneratedTextFile> renderFeatureModule(PlatformAdapterScaffoldSpec spec, String featureKey) {
        String installerClassName = installerClassName(featureKey, spec.platformClassPrefix());
        String installerType = ModuleMatrix.featureSpec(featureKey).installerType();
        String modulePath = featureKey + "-" + spec.platformKey();
        String implementation = spec.basePackageName() + "." + featureKey + "." + spec.platformPackageSegment() + "." + installerClassName;

        return List.of(
            new GeneratedTextFile(
                "%s/src/main/java/%s/%s/%s/%s.java".formatted(
                    modulePath,
                    spec.packagePath(),
                    featureKey,
                    spec.platformPackageSegment(),
                    installerClassName
                ),
                renderFeatureInstallerSource(spec, featureKey, installerClassName)
            ),
            new GeneratedTextFile(
                modulePath + "/src/main/resources/META-INF/services/" + installerType,
                implementation + "\n"
            )
        );
    }

    private static String renderBootstrapSource(PlatformAdapterScaffoldSpec spec, String bootstrapClassName) {
        return """
            package %s.platform.%s;

            import de.t14d3.rapunzellib.context.RapunzelContext;

            public final class %s {
                private %s() {
                }

                public static RapunzelContext bootstrap(Object bootstrapOwner) {
                    throw new UnsupportedOperationException(
                        "TODO: implement platform-%s bootstrap wiring for the current runtime entrypoint"
                    );
                }
            }
            """.formatted(
            spec.basePackageName(),
            spec.platformPackageSegment(),
            bootstrapClassName,
            bootstrapClassName,
            spec.platformKey()
        ) + "\n";
    }

    private static String renderAttachmentInstallerSource(
        PlatformAdapterScaffoldSpec spec,
        String installerClassName,
        String installerPackage
    ) {
        return """
            package %s;

            import de.t14d3.rapunzellib.PlatformId;
            import de.t14d3.rapunzellib.attachments.AttachmentFeatureInstaller;
            import de.t14d3.rapunzellib.context.RapunzelContext;
            import org.jetbrains.annotations.NotNull;

            public final class %s implements AttachmentFeatureInstaller {
                @Override
                public @NotNull PlatformId platformId() {
                    %s
                }

                @Override
                public void install(@NotNull RapunzelContext context) {
                    %s
                }
            }
            """.formatted(
            installerPackage,
            installerClassName,
            platformIdResolver(spec.platformKey()),
            minimalShimComment(spec.sharedCoreFamily(), "register attachment support")
        ) + "\n";
    }

    private static String renderFeatureInstallerSource(
        PlatformAdapterScaffoldSpec spec,
        String featureKey,
        String installerClassName
    ) {
        String packageName = spec.basePackageName() + "." + featureKey + "." + spec.platformPackageSegment();
        String installComment = renderInstallComment(spec, featureKey);

        return switch (featureKey) {
            case "commands" -> renderVoidInstallerSource(
                packageName,
                installerClassName,
                "de.t14d3.rapunzellib.commands.CommandFeatureInstaller",
                "CommandFeatureInstaller",
                platformIdResolver(spec.platformKey()),
                installComment
            );
            case "events" -> renderEventInstallerSource(packageName, installerClassName, spec.platformKey(), installComment);
            case "gui" -> renderVoidInstallerSource(
                packageName,
                installerClassName,
                "de.t14d3.rapunzellib.gui.GuiFeatureInstaller",
                "GuiFeatureInstaller",
                platformIdResolver(spec.platformKey()),
                installComment
            );
            case "inventory" -> renderVoidInstallerSource(
                packageName,
                installerClassName,
                "de.t14d3.rapunzellib.inventory.InventoryFeatureInstaller",
                "InventoryFeatureInstaller",
                platformIdResolver(spec.platformKey()),
                installComment
            );
            case "nbt" -> renderVoidInstallerSource(
                packageName,
                installerClassName,
                "de.t14d3.rapunzellib.nbt.NbtFeatureInstaller",
                "NbtFeatureInstaller",
                platformIdResolver(spec.platformKey()),
                installComment
            );
            default -> throw new GradleException("Unsupported scaffold feature: " + featureKey);
        };
    }

    private static String renderVoidInstallerSource(
        String packageName,
        String className,
        String interfaceImport,
        String interfaceName,
        String platformIdStatement,
        String installComment
    ) {
        return """
            package %s;

            import de.t14d3.rapunzellib.PlatformId;
            import de.t14d3.rapunzellib.context.RapunzelContext;
            import %s;
            import org.jetbrains.annotations.NotNull;

            public final class %s implements %s {
                @Override
                public @NotNull PlatformId platformId() {
                    %s
                }

                @Override
                public void install(@NotNull RapunzelContext context) {
                    %s
                }
            }
            """.formatted(packageName, interfaceImport, className, interfaceName, platformIdStatement, installComment) + "\n";
    }

    private static String renderEventInstallerSource(
        String packageName,
        String className,
        String platformKey,
        String installComment
    ) {
        return """
            package %s;

            import de.t14d3.rapunzellib.PlatformId;
            import de.t14d3.rapunzellib.context.RapunzelContext;
            import de.t14d3.rapunzellib.events.GameEventBridge;
            import de.t14d3.rapunzellib.events.GameEventBridgeInstaller;
            import de.t14d3.rapunzellib.events.GameEventBus;
            import org.jetbrains.annotations.NotNull;

            public final class %s implements GameEventBridgeInstaller {
                @Override
                public @NotNull PlatformId platformId() {
                    %s
                }

                @Override
                public @NotNull GameEventBridge install(
                    @NotNull RapunzelContext context,
                    @NotNull GameEventBus bus
                ) {
                    %s
                    return () -> {
                    };
                }
            }
            """.formatted(packageName, className, platformIdResolver(platformKey), installComment) + "\n";
    }

    private static String renderSettingsSnippet(PlatformAdapterScaffoldSpec spec) {
        List<String> modules = new ArrayList<>();
        modules.add("platform-" + spec.platformKey());
        for (String featureKey : spec.featureKeys()) {
            modules.add(featureKey + "-" + spec.platformKey());
        }
        String includeLines = modules.stream().map(module -> "    \"" + module + "\"").reduce((a, b) -> a + ",\n" + b).orElse("");
        return """
            // Generated by rapunzellibGeneratePlatformAdapterScaffold
            include(
            %s
            )
            """.formatted(includeLines) + "\n";
    }

    private static String renderBuildSnippet(PlatformAdapterScaffoldSpec spec) {
        List<String> lines = new ArrayList<>();
        lines.add("// Generated by rapunzellibGeneratePlatformAdapterScaffold");
        lines.add("project(\":platform-" + spec.platformKey() + "\") {");
        lines.add("    dependencies {");
        lines.add("        api(project(\":api\"))");
        lines.add("        implementation(project(\":common\"))");
        lines.add("        implementation(project(\":network\"))");
        lines.add("        implementation(project(\":database-spool\"))");
        String sharedPlatformModule = ModuleMatrix.sharedCoreModuleForPlatformFamily(spec.sharedCoreFamily());
        if (sharedPlatformModule != null) {
            lines.add("        api(project(\"" + sharedPlatformModule + "\"))");
        }
        lines.add("    }");
        lines.add("}");

        for (String featureKey : spec.featureKeys()) {
            lines.add("");
            lines.add("project(\":" + featureKey + "-" + spec.platformKey() + "\") {");
            lines.add("    dependencies {");
            lines.add("        api(project(\":" + featureKey + "\"))");
            lines.add("        implementation(project(\":platform-" + spec.platformKey() + "\"))");
            if (spec.sharedCoreFeatures().contains(featureKey)) {
                String sharedCoreModule = ModuleMatrix.sharedCoreModuleForFeature(featureKey, spec.sharedCoreFamily());
                if (sharedCoreModule != null) {
                    lines.add("        implementation(project(\"" + sharedCoreModule + "\"))");
                }
            }
            if (ModuleMatrix.featureSpec(featureKey).additionalPlatformModuleDependency()) {
                lines.add("        implementation(project(\":nbt-" + spec.platformKey() + "\"))");
            }
            lines.add("    }");
            lines.add("}");
        }

        return String.join("\n", lines) + "\n";
    }

    private static String renderArchitectureNotesSnippet(PlatformAdapterScaffoldSpec spec) {
        List<String> lines = new ArrayList<>();
        lines.add("# Generated scaffold notes");
        lines.add("");
        lines.add("- Keep `platform-" + spec.platformKey() + "` focused on runtime/bootstrap wiring.");
        if (!ModuleMatrix.SHARED_CORE_FAMILY_NONE.equals(spec.sharedCoreFamily())) {
            String sharedPlatformModule = ModuleMatrix.sharedCoreModuleForPlatformFamily(spec.sharedCoreFamily());
            if (sharedPlatformModule != null) {
                lines.add("- Put reusable platform logic in `" + sharedPlatformModule + "`; keep platform-specific adapters thin.");
            }
        }

        for (String featureKey : spec.featureKeys()) {
            if (spec.sharedCoreFeatures().contains(featureKey)) {
                String sharedCoreModule = ModuleMatrix.sharedCoreModuleForFeature(featureKey, spec.sharedCoreFamily());
                if (sharedCoreModule != null) {
                    lines.add(
                        "- Implement heavy `" + featureKey + "` logic in `" + sharedCoreModule
                            + "`; generated `" + featureKey + "-" + spec.platformKey() + "` stays as a shim/installer layer."
                    );
                }
            }
        }

        return String.join("\n", lines) + "\n";
    }

    private static String renderInstallComment(PlatformAdapterScaffoldSpec spec, String featureKey) {
        String sharedCoreFamily = spec.sharedCoreFeatures().contains(featureKey)
            ? spec.sharedCoreFamily()
            : ModuleMatrix.SHARED_CORE_FAMILY_NONE;
        return minimalShimComment(sharedCoreFamily, installAction(featureKey, spec.platformKey()));
    }

    private static String installerClassName(String featureKey, String platformClassPrefix) {
        return switch (featureKey) {
            case "commands" -> platformClassPrefix + "CommandFeatureInstaller";
            case "events" -> platformClassPrefix + "GameEventBridgeInstaller";
            case "gui" -> platformClassPrefix + "GuiFeatureInstaller";
            case "inventory" -> platformClassPrefix + "InventoryFeatureInstaller";
            case "nbt" -> platformClassPrefix + "NbtFeatureInstaller";
            default -> throw new GradleException("Unsupported scaffold feature: " + featureKey);
        };
    }

    private static String installAction(String featureKey, String platformKey) {
        return switch (featureKey) {
            case "commands" -> "register command adapters/services";
            case "events" -> "hook platform-" + platformKey + " events into bus";
            case "gui" -> "register GUI renderer/providers";
            case "inventory" -> "register inventory adapters/providers";
            case "nbt" -> "register NBT serializers/adapters";
            default -> throw new GradleException("Unsupported scaffold feature: " + featureKey);
        };
    }

    private static String platformIdResolver(String platformKey) {
        return switch (platformKey) {
            case "paper" -> "return PlatformId.PAPER;";
            case "velocity" -> "return PlatformId.VELOCITY;";
            case "fabric" -> "return PlatformId.FABRIC;";
            case "neoforge" -> "return PlatformId.NEOFORGE;";
            case "sponge" -> "return PlatformId.SPONGE;";
            default -> """
                throw new UnsupportedOperationException(
                    "TODO: add PlatformId enum constant for '%s' and return it here"
                );
                """.formatted(platformKey).stripIndent();
        };
    }

    private static String minimalShimComment(String sharedCoreFamily, String action) {
        if (ModuleMatrix.SHARED_CORE_FAMILY_NONE.equals(sharedCoreFamily)) {
            return "// TODO: " + action + " for this platform.";
        }
        return "// TODO: keep this shim thin; delegate reusable logic to the "
            + sharedCoreFamily
            + " shared-core module and only "
            + action
            + " here.";
    }
}
