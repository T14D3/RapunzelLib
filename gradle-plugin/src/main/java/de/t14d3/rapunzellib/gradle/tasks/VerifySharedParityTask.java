package de.t14d3.rapunzellib.gradle.tasks;

import de.t14d3.rapunzellib.gradle.ModuleMatrix;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class VerifySharedParityTask extends DefaultTask {
    @TaskAction
    public void verify() {
        Project root = getProject().getRootProject();
        List<Project> fabricProjects = root.getSubprojects().stream().filter(project -> isParityManagedModule(project.getName(), "fabric")).toList();
        List<Project> neoforgeProjects = root.getSubprojects().stream().filter(project -> isParityManagedModule(project.getName(), "neoforge")).toList();

        if (fabricProjects.isEmpty() && neoforgeProjects.isEmpty()) {
            getLogger().lifecycle("No -fabric or -neoforge modules detected; skipping Shared parity verification.");
            return;
        }

        List<String> missingCounterparts = buildMissingCounterparts(root);
        if (!missingCounterparts.isEmpty()) {
            throw new GradleException(
                "Fabric/NeoForge module matrix drift detected:\n"
                    + missingCounterparts.stream().map(value -> "- " + value).collect(java.util.stream.Collectors.joining("\n"))
            );
        }

        VersionCatalog libs = root.getExtensions().findByType(VersionCatalogsExtension.class) != null
            ? root.getExtensions().getByType(VersionCatalogsExtension.class).named("libs")
            : null;
        String catalogMinecraftVersion = versionFromCatalog(libs, "minecraft");
        String catalogNeoForgeVersion = versionFromCatalog(libs, "neoforge");

        Set<String> minecraftVersions = new LinkedHashSet<>();
        for (Project fabricProject : fabricProjects) {
            var minecraftConfig = fabricProject.getConfigurations().findByName("minecraft");
            if (minecraftConfig == null) {
                getLogger().info("Skipping explicit minecraft configuration assertion for {}.", fabricProject.getPath());
                continue;
            }

            var minecraftDeps = minecraftConfig.getAllDependencies().stream()
                .filter(dependency -> "com.shared".equals(dependency.getGroup())
                    && "minecraft".equals(dependency.getName())
                    && dependency.getVersion() != null
                    && !dependency.getVersion().isBlank())
                .toList();
            String dependencyVersion = !minecraftDeps.isEmpty() ? minecraftDeps.getFirst().getVersion() : catalogMinecraftVersion;
            if (dependencyVersion != null && !dependencyVersion.isBlank()) {
                minecraftVersions.add(dependencyVersion);
                if (catalogMinecraftVersion != null && !catalogMinecraftVersion.isBlank() && !dependencyVersion.equals(catalogMinecraftVersion)) {
                    throw new GradleException(
                        fabricProject.getPath() + " minecraft dependency '" + dependencyVersion
                            + "' is not aligned with libs.minecraft=" + catalogMinecraftVersion + "."
                    );
                }
            }

            var mappingsConfig = fabricProject.getConfigurations().findByName("mappings");
            if (mappingsConfig == null) {
                getLogger().info("Skipping mappings configuration assertion for {}.", fabricProject.getPath());
                continue;
            }
            if (mappingsConfig.getAllDependencies().isEmpty() && (catalogMinecraftVersion == null || catalogMinecraftVersion.isBlank())) {
                getLogger().info(
                    "Skipping mappings dependency assertion for {}; no explicit mappings dependency is present.",
                    fabricProject.getPath()
                );
            }
        }

        if (minecraftVersions.size() > 1) {
            throw new GradleException(
                "Fabric modules are not aligned on a single minecraft version: " + String.join(", ", new java.util.TreeSet<>(minecraftVersions)) + "."
            );
        }

        if (!neoforgeProjects.isEmpty()) {
            String minecraftVersion = catalogMinecraftVersion != null ? catalogMinecraftVersion : minecraftVersions.stream().findFirst().orElse(null);
            String neoforgeVersion = catalogNeoForgeVersion != null ? catalogNeoForgeVersion : detectNeoForgeVersion(neoforgeProjects);

            if (minecraftVersion == null || minecraftVersion.isBlank()) {
                getLogger().lifecycle("Skipping minecraft/neoforge version alignment check because no minecraft version could be determined.");
                return;
            }
            if (neoforgeVersion == null || neoforgeVersion.isBlank()) {
                getLogger().lifecycle("Skipping minecraft/neoforge version alignment check because no NeoForge version could be determined.");
                return;
            }

            String expectedPrefix = minecraftVersion.replaceFirst("^1\\.", "") + ".";
            if (!neoforgeVersion.startsWith(expectedPrefix)) {
                throw new GradleException("NeoForge '" + neoforgeVersion + "' is not aligned with minecraft '" + minecraftVersion + "'.");
            }
        }

        getLogger().lifecycle("Fabric/NeoForge Shared parity assumptions verified.");
    }

    private String detectNeoForgeVersion(List<Project> neoforgeProjects) {
        Set<String> versions = new LinkedHashSet<>();
        for (Project module : neoforgeProjects) {
            module.getConfigurations().forEach(configuration ->
                configuration.getAllDependencies().stream()
                    .filter(dependency -> "net.neoforged".equals(dependency.getGroup())
                        && "neoforge".equals(dependency.getName())
                        && dependency.getVersion() != null
                        && !dependency.getVersion().isBlank())
                    .forEach(dependency -> versions.add(dependency.getVersion()))
            );
        }
        if (versions.size() > 1) {
            throw new GradleException("NeoForge modules are not aligned on a single neoforge version: " + String.join(", ", new java.util.TreeSet<>(versions)));
        }
        return versions.stream().findFirst().orElse(null);
    }

    private boolean isParityManagedModule(String projectName, String platformKey) {
        return switch (platformKey) {
            case "fabric" ->
                "fabric".equals(ModuleMatrix.parsePlatformModule(projectName))
                    || (ModuleMatrix.parseFeatureModule(projectName) != null
                    && "fabric".equals(ModuleMatrix.parseFeatureModule(projectName).platformKey()));
            case "neoforge" ->
                "neoforge".equals(ModuleMatrix.parsePlatformModule(projectName))
                    || (ModuleMatrix.parseFeatureModule(projectName) != null
                    && "neoforge".equals(ModuleMatrix.parseFeatureModule(projectName).platformKey()));
            default -> false;
        };
    }

    private List<String> buildMissingCounterparts(Project root) {
        Set<String> bases = new LinkedHashSet<>();
        bases.add("platform");
        bases.addAll(ModuleMatrix.FEATURE_KEYS);

        List<String> missing = new ArrayList<>();
        for (String base : bases) {
            String fabricPath = "platform".equals(base) ? ":platform-fabric" : ":" + base + "-fabric";
            String neoforgePath = "platform".equals(base) ? ":platform-neoforge" : ":" + base + "-neoforge";
            boolean hasFabric = root.findProject(fabricPath) != null;
            boolean hasNeoForge = root.findProject(neoforgePath) != null;
            if (hasFabric && !hasNeoForge) {
                missing.add(neoforgePath + " is missing while " + fabricPath + " exists.");
            } else if (hasNeoForge && !hasFabric) {
                missing.add(fabricPath + " is missing while " + neoforgePath + " exists.");
            }
        }
        return missing;
    }

    private String versionFromCatalog(VersionCatalog catalog, String alias) {
        if (catalog == null) {
            return null;
        }
        return catalog.findVersion(alias).map(version -> version.getRequiredVersion()).orElse(null);
    }
}
