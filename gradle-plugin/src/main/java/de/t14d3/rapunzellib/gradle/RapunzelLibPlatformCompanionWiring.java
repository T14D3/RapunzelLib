package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RapunzelLibPlatformCompanionWiring {
    private static final String RAPUNZELLIB_GROUP = "de.t14d3.rapunzellib";
    private static final String PLATFORM_FABRIC = "platform-fabric";
    private static final String PLATFORM_NEOFORGE = "platform-neoforge";
    private static final String COMPANION_CLASSIFIER = "companion";
    private static final String NEOFORGE_DEV_RUNTIME = "rapunzellibDevRuntime";

    private RapunzelLibPlatformCompanionWiring() {
    }

    public static void wire(Project project) {
        project.getPluginManager().withPlugin("fabric-loom", ignored ->
            project.afterEvaluate(evalProject -> configureFabricCompanionDependencies(project))
        );
        project.getPluginManager().withPlugin("net.neoforged.moddev", ignored ->
            project.afterEvaluate(evalProject -> configureNeoForgeCompanionDependencies(project))
        );
    }

    public static void configureFabricCompanionDependencies(Project project) {
        String version = findRapunzelLibVersion(project);
        if (version == null || !dependsOnRapunzelArtifact(project, PLATFORM_FABRIC)) {
            return;
        }

        var include = project.getConfigurations().findByName("include");
        if (include == null) {
            project.getLogger().warn(
                "RapunzelLib detected {} but Fabric Loom has no include configuration in {}",
                PLATFORM_FABRIC,
                project.getPath()
            );
            return;
        }

        addDependencyIfMissing(project, include.getName(), PLATFORM_FABRIC, version, null);
        addDependencyIfMissing(project, include.getName(), PLATFORM_FABRIC, version, COMPANION_CLASSIFIER);

        for (String name : List.of("localRuntime", "modLocalRuntime", "runtimeOnly")) {
            var runtimeConfig = project.getConfigurations().findByName(name);
            if (runtimeConfig != null) {
                addDependencyIfMissing(project, runtimeConfig.getName(), PLATFORM_FABRIC, version, COMPANION_CLASSIFIER);
                break;
            }
        }
    }

    public static void configureNeoForgeCompanionDependencies(Project project) {
        String version = findRapunzelLibVersion(project);
        if (version == null || !dependsOnRapunzelArtifact(project, PLATFORM_NEOFORGE)) {
            return;
        }

        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        String jarJarConfigurationName =
            sourceSets != null && sourceSets.findByName("main") != null
                ? sourceSets.findByName("main").getTaskName(null, "jarJar")
                : "jarJar";
        var jarJar = project.getConfigurations().findByName(jarJarConfigurationName);
        if (jarJar == null) {
            project.getLogger().warn(
                "RapunzelLib detected {} but NeoForge ModDev has no {} configuration in {}",
                PLATFORM_NEOFORGE,
                jarJarConfigurationName,
                project.getPath()
            );
            return;
        }

        addDependencyIfMissing(project, jarJar.getName(), PLATFORM_NEOFORGE, version, null);
        addDependencyIfMissing(project, jarJar.getName(), PLATFORM_NEOFORGE, version, COMPANION_CLASSIFIER);

        if (sourceSets != null && sourceSets.findByName("main") != null) {
            var mainSourceSet = sourceSets.findByName("main");
            var devRuntime = project.getConfigurations().findByName(NEOFORGE_DEV_RUNTIME);
            if (devRuntime == null) {
                devRuntime = project.getConfigurations().create(NEOFORGE_DEV_RUNTIME, configuration -> {
                    configuration.setCanBeConsumed(false);
                    configuration.setCanBeResolved(false);
                    configuration.setDescription("RapunzelLib NeoForge companion mods for development runtime only.");
                });
            }
            var finalDevRuntime = devRuntime;
            project.getConfigurations().named(mainSourceSet.getRuntimeClasspathConfigurationName()).configure(configuration -> {
                configuration.extendsFrom(finalDevRuntime);
            });
            addDependencyIfMissing(project, devRuntime.getName(), PLATFORM_NEOFORGE, version, COMPANION_CLASSIFIER);
        } else {
            var runtimeOnly = project.getConfigurations().findByName("runtimeOnly");
            if (runtimeOnly != null) {
                addDependencyIfMissing(project, runtimeOnly.getName(), PLATFORM_NEOFORGE, version, COMPANION_CLASSIFIER);
            }
        }
    }

    private static String findRapunzelLibVersion(Project project) {
        Set<String> versions = new LinkedHashSet<>();
        project.getConfigurations().forEach(configuration ->
            configuration.getDependencies().withType(ExternalModuleDependency.class).forEach(dependency -> {
                if (RAPUNZELLIB_GROUP.equals(dependency.getGroup())) {
                    String version = dependencyVersion(dependency);
                    if (version != null) {
                        versions.add(version);
                    }
                }
            })
        );
        if (versions.size() > 1) {
            project.getLogger().warn(
                "Multiple RapunzelLib versions detected in {}: {}. Using {}",
                project.getPath(),
                versions,
                versions.iterator().next()
            );
        }
        return versions.stream().findFirst().orElse(null);
    }

    private static boolean dependsOnRapunzelArtifact(Project project, String artifact) {
        return project.getConfigurations().stream().anyMatch(configuration ->
            configuration.getDependencies().withType(ExternalModuleDependency.class).stream().anyMatch(dependency ->
                RAPUNZELLIB_GROUP.equals(dependency.getGroup()) && artifact.equals(dependency.getName())
            )
        );
    }

    private static String dependencyVersion(ExternalModuleDependency dependency) {
        if (dependency.getVersion() != null && !dependency.getVersion().isBlank()) {
            return dependency.getVersion();
        }
        if (!dependency.getVersionConstraint().getStrictVersion().isBlank()) {
            return dependency.getVersionConstraint().getStrictVersion();
        }
        if (!dependency.getVersionConstraint().getRequiredVersion().isBlank()) {
            return dependency.getVersionConstraint().getRequiredVersion();
        }
        if (!dependency.getVersionConstraint().getPreferredVersion().isBlank()) {
            return dependency.getVersionConstraint().getPreferredVersion();
        }
        return null;
    }

    private static void addDependencyIfMissing(
        Project project,
        String configurationName,
        String artifact,
        String version,
        String classifier
    ) {
        var configuration = project.getConfigurations().findByName(configurationName);
        if (configuration == null) {
            return;
        }
        boolean exists = configuration.getDependencies().withType(ExternalModuleDependency.class).stream().anyMatch(dependency ->
            RAPUNZELLIB_GROUP.equals(dependency.getGroup())
                && artifact.equals(dependency.getName())
                && version.equals(dependencyVersion(dependency))
                && java.util.Objects.equals(
                    dependency.getArtifacts().size() == 1 ? dependency.getArtifacts().iterator().next().getClassifier() : null,
                    classifier
                )
        );
        if (exists) {
            return;
        }

        Map<String, String> notation = new LinkedHashMap<>();
        notation.put("group", RAPUNZELLIB_GROUP);
        notation.put("name", artifact);
        notation.put("version", version);
        if (classifier != null) {
            notation.put("classifier", classifier);
        }
        project.getDependencies().add(configurationName, notation);
    }
}
