package de.t14d3.rapunzellib.buildlogic;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.spongepowered.gradle.vanilla.MinecraftExtension;
import org.spongepowered.gradle.vanilla.repository.MinecraftPlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import de.t14d3.rapunzellib.buildlogic.tasks.CheckReposiliteConfigTask;
import org.gradle.plugins.signing.SigningExtension;

public final class BuildLogicPluginSupport {
    private static final int JAVA_VERSION = 25;
    private static final String REPOSILITE_REPOSITORY_NAME = "reposilite";
    private static final String DEFAULT_REPOSILITE_BASE_URL = "https://maven.t14d3.de";

    private BuildLogicPluginSupport() {
    }

    public static void applyBaseJavaModule(Project project) {
        project.getPluginManager().apply("java-library");
        project.getPluginManager().apply("de.t14d3.rapunzellib");
        configureJavaToolchain(project);
    }

    public static void configureJavaToolchain(Project project) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        if (java != null) {
            java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(JAVA_VERSION));
            java.withSourcesJar();
            java.withJavadocJar();
        }
        project.getTasks().withType(JavaCompile.class).configureEach(task -> task.getOptions().getRelease().set(JAVA_VERSION));
        project.getTasks().withType(Test.class).configureEach(Test::useJUnitPlatform);
    }

    public static void configureMavenPublishing(Project project) {
        project.getPluginManager().apply("maven-publish");
        if (project.getComponents().findByName("java") == null) {
            return;
        }

        PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
        if (publishing.getPublications().findByName("mavenJava") != null) {
            return;
        }
        if (publishing.getPublications().findByName("pluginMaven") != null) {
            return;
        }

        MavenPublication publication = publishing.getPublications().create("mavenJava", MavenPublication.class);
        publication.from(project.getComponents().getByName("java"));
    }

    public static TaskProvider<CheckReposiliteConfigTask> registerReposiliteConfigCheck(Project rootProject) {
        return rootProject.getTasks().register("checkReposiliteConfig", CheckReposiliteConfigTask.class, task -> {
            task.setGroup("publishing");
            task.setDescription("Validates the Reposilite configuration required for remote publishing.");
            task.getReposiliteBaseUrl().set(rootProject.getProviders().gradleProperty("reposiliteBaseUrl")
                .orElse(rootProject.getProviders().environmentVariable("REPOSILITE_BASE_URL"))
                .orElse(DEFAULT_REPOSILITE_BASE_URL));
            task.getReposiliteUsername().set(rootProject.getProviders().gradleProperty("reposiliteUsername")
                .orElse(rootProject.getProviders().environmentVariable("REPOSILITE_USERNAME")));
            task.getReposilitePassword().set(rootProject.getProviders().gradleProperty("reposilitePassword")
                .orElse(rootProject.getProviders().environmentVariable("REPOSILITE_PASSWORD")));
        });
    }

    public static TaskProvider<Task> registerPublishToReposilite(Project rootProject) {
        return rootProject.getTasks().register("publishToReposilite", task -> {
            task.setGroup("publishing");
            task.setDescription("Publishes all Reposilite-targeted Maven publications from publishable subprojects.");
        });
    }

    public static void configureReposilitePublishing(Project project, TaskProvider<CheckReposiliteConfigTask> checkReposiliteConfig) {
        PublishingExtension publishing = project.getExtensions().findByType(PublishingExtension.class);
        if (publishing == null) {
            return;
        }
        if (publishing.getRepositories().findByName(REPOSILITE_REPOSITORY_NAME) == null) {
            publishing.getRepositories().maven(repository -> {
                repository.setName(REPOSILITE_REPOSITORY_NAME);
                repository.setUrl(project.uri(reposiliteRepositoryUrl(project)));
                repository.credentials(PasswordCredentials.class, credentials -> {
                    credentials.setUsername(optionalProperty(project, "reposiliteUsername", "REPOSILITE_USERNAME"));
                    credentials.setPassword(optionalProperty(project, "reposilitePassword", "REPOSILITE_PASSWORD"));
                });
            });
        }

        project.getTasks().withType(PublishToMavenRepository.class).configureEach(task -> {
            if (!isReposilitePublishTask(task)) {
                return;
            }
            task.dependsOn(checkReposiliteConfig);
        });
    }

    // ── Signing ────────────────────────────────────────────────────────────
    /**
     * Configures in-memory GPG signing for the project's Maven publications.
     *
     * <p>Reads the signing key from the {@code GPG_PRIVATE} environment variable
     * (base64-encoded ASCII-armored private key). If the variable is not set,
     * signing is skipped with a warning.</p>
     *
     * <p>An optional {@code GPG_PASSPHRASE} environment variable provides the
     * key's passphrase. If not set, an empty passphrase is assumed.</p>
     *
     * <p>The signatory is configured in {@code afterEvaluate} so it is available
     * when the signing tasks resolve their signatory at execution time.</p>
     */
    public static void configureSigning(Project project) {
        String privateKey = optionalProperty(project, "gpgPrivateKey", "GPG_PRIVATE");
        if (privateKey == null || privateKey.trim().isEmpty()) {
            project.getLogger().warn("");
            project.getLogger().warn("  ⚠  {} - Artifact signing DISABLED", project.getPath());
            project.getLogger().warn("     Set GPG_PRIVATE environment variable (base64-armored private key).");
            project.getLogger().warn("     Without signing, Maven Central will reject the upload.");
            project.getLogger().warn("");
            return;
        }

        String passphrase = optionalProperty(project, "gpgPassphrase", "GPG_PASSPHRASE");

        // Apply the signing plugin and hook publications immediately
        project.getPluginManager().apply("signing");
        SigningExtension signing = project.getExtensions().getByType(SigningExtension.class);

        PublishingExtension publishing = project.getExtensions().findByType(PublishingExtension.class);
        if (publishing != null) {
            signing.sign(publishing.getPublications());
        }

        // Gradle expects the raw ASCII-armored PGP key (with BEGIN/END markers),
        // but the env var is base64-encoded (to keep it single-line). Decode first.
        String armoredKey = decodeBase64OrPassthrough(privateKey);
        if (armoredKey == null) {
            project.getLogger().warn("  ⚠  {} - Signing key is not valid base64 or ASCII-armored PGP", project.getPath());
            return;
        }

        // Set the actual signatory after project evaluation so it is guaranteed
        // to be available when signing tasks execute.
        String effectivePassphrase = passphrase != null ? passphrase : "";
        project.afterEvaluate(p -> {
            SigningExtension s = p.getExtensions().getByType(SigningExtension.class);
            s.useInMemoryPgpKeys(armoredKey, effectivePassphrase);
            s.setRequired(true);

            // Gradle 9 requires explicit task dependencies when tasks consume
            // the outputs of other tasks. Nmcp publish tasks read the .asc
            // signature files produced by Sign tasks. Wire the dependency so
            // Gradle doesn't flag the missing cross-task relationship.
            p.getTasks().matching(task ->
                task.getName().contains("ToNmcpRepository")
            ).configureEach(nmcpTask ->
                nmcpTask.dependsOn(p.getTasks().withType(org.gradle.plugins.signing.Sign.class))
            );
        });

        project.getLogger().lifecycle(
            "  ✓  {} - Signing configured ({} byte key loaded)",
            project.getPath(), privateKey.length()
        );
    }

    /**
     * Decodes a base64-encoded ASCII-armored PGP key back to the armored text form.
     * If the input is not valid base64, returns it as-is (passthrough for raw armored keys).
     */
    private static String decodeBase64OrPassthrough(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String trimmed = value.trim();
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(trimmed);
            String result = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            // Sanity check: decoded text must contain PGP armor markers
            if (result.contains("-----BEGIN PGP")) {
                return result;
            }
            // Armor already raw (not base64-encoded) - passthrough unchanged
        } catch (IllegalArgumentException ignored) {
            // Armor already raw (not base64-encoded) - passthrough unchanged
        }
        // Passthrough
        return trimmed;
    }

    public static void addLibraryDependency(Project project, String configuration, String alias) {
        project.getDependencies().add(configuration, library(project, alias));
    }

    public static void addProjectDependency(Project project, String configuration, String path) {
        if (path.equals(project.getPath())) {
            return;
        }
        if (project.getRootProject().findProject(path) != null) {
            project.getDependencies().add(configuration, project.project(path));
        }
    }

    public static void addFamilyAndSharedDependencies(Project project) {
        String family = project.getName().substring(0, project.getName().indexOf('-'));
        addProjectDependency(project, "api", ":" + family);
        // Modules can opt out by setting rapunzellib.disableFamilySharedDependency=true
        // in their own gradle.properties (loaded before any plugin applies).
        Object skip = project.findProperty("rapunzellib.disableFamilySharedDependency");
        if (skip != null && "true".equals(skip.toString())) {
            return;
        }
        addProjectDependency(project, "implementation", ":" + family + "-shared");
    }

    public static void configurePaperweightUserdev(Project project) {
        Object extension = ((ExtensionAware) project.getDependencies()).getExtensions().findByName("paperweight");
        if (extension == null) {
            return;
        }
        Method method = firstMethod(extension.getClass(), "paperDevBundle", String.class);
        if (method == null) {
            return;
        }
        invoke(method, extension, version(project, "paper-api"));
    }

    public static void configureFabricLoom(Project project) {
        Object loom = project.getExtensions().findByName("loom");
        if (loom == null) {
            project.getLogger().error("Loom not found!");
            return;
        }
        project.getConfigurations().maybeCreate("minecraft");
        project.getDependencies().add("minecraft", library(project, "minecraft"));
        if (usesOfficialMojangMappings(project)) {
            Method mappingsMethod = firstZeroArgMethod(loom.getClass(), "officialMojangMappings");
            if (mappingsMethod != null) {
                Object mappings = invoke(mappingsMethod, loom);
                if (mappings != null) {
                    project.getConfigurations().maybeCreate("mappings");
                    project.getDependencies().add("mappings", mappings);
                }
            }
        }
        String targetConfig = isFabricObfuscationDisabled(project) ? "implementation" : "modImplementation";
        project.getConfigurations().maybeCreate(targetConfig);
        project.getDependencies().add(targetConfig, library(project, "fabric-loader"));
    }

    public static boolean isFabricLoomEnabled(Project project) {
        return booleanProperty(project, "rapunzellib.fabric.loom-enabled", "RAPUNZELLIB_FABRIC_LOOM_ENABLED", "fabric.loom-enabled", true);
    }

    public static void configureFabricDependencyRouting(Project project) {
        Configuration fabricImplementation = project.getConfigurations().maybeCreate("fabricImplementation");
        fabricImplementation.setDescription("Fabric dependencies routed to the target-appropriate implementation configuration.");

        Configuration implementation = project.getConfigurations().findByName("implementation");
        if (implementation == null) {
            return;
        }

        if (isFabricObfuscationDisabled(project)) {
            implementation.extendsFrom(fabricImplementation);
            return;
        }

        Configuration modImpl = project.getConfigurations().maybeCreate("modImplementation");
        modImpl.extendsFrom(fabricImplementation);
    }

    public static void configureFabricLoomProperties(Project project) {
        project.getExtensions().getExtraProperties().set(
            "fabric.loom.disableObfuscation",
            String.valueOf(isFabricObfuscationDisabled(project))
        );
    }

    /**
     * Returns the appropriate Fabric Loom plugin ID for the current Minecraft target.
     *
     * <p>Loom 1.16+ ships two plugin variants:
     * <ul>
     *   <li>{@code net.fabricmc.fabric-loom} - for non-obfuscated versions (Minecraft 26.1+)
     *   <li>{@code net.fabricmc.fabric-loom-remap} - for obfuscated versions (1.21.11 or older)
     * </ul>
     */
    public static String fabricLoomPluginId(Project project) {
        if (requiresOfficialMojangMappings(version(project, "minecraft"))) {
            return "net.fabricmc.fabric-loom-remap";
        }
        return "net.fabricmc.fabric-loom";
    }

    public static void configureFabricWithoutLoom(Project project) {
        Configuration modImplementation = project.getConfigurations().maybeCreate("modImplementation");
        Configuration implementation = project.getConfigurations().findByName("implementation");
        if (implementation != null) {
            implementation.extendsFrom(modImplementation);
        }
        project.getDependencies().add("modImplementation", library(project, "fabric-loader"));
    }

    public static void configureNeoForge(Project project) {
        Object extension = project.getExtensions().findByName("neoForge");
        if (extension == null) {
            return;
        }
        Method method = firstMethod(extension.getClass(), "setVersion", String.class);
        if (method != null) {
            invoke(method, extension, version(project, "neoforge"));
        }
    }

    public static void configureVanillaMinecraft(Project project) {
        MinecraftExtension extension = project.getExtensions().findByType(MinecraftExtension.class);
        if (extension == null) {
            return;
        }
        extension.version(version(project, "minecraft"));
        extension.platform(MinecraftPlatform.SERVER);
    }

    private static VersionCatalog libs(Project project) {
        return project.getExtensions().getByType(VersionCatalogsExtension.class).named("libs");
    }

    private static Object library(Project project, String alias) {
        MinimalExternalModuleDependency dependency = libs(project).findLibrary(alias).orElseThrow().get();
        String overrideVersion = versionOverride(project, alias);
        if (overrideVersion == null) {
            return dependency;
        }
        return dependency.getModule().getGroup() + ":" + dependency.getModule().getName() + ":" + overrideVersion;
    }

    private static String version(Project project, String alias) {
        String overrideVersion = versionOverride(project, alias);
        if (overrideVersion != null) {
            return overrideVersion;
        }
        return libs(project).findVersion(alias).map(version -> version.getRequiredVersion()).orElseThrow();
    }

    /**
     * Determines whether a Minecraft version needs the explicit {@code officialMojangMappings()} call
     * and the {@code net.fabricmc.fabric-loom-remap} plugin.
     *
     * <p>Loom 1.16+ ships two plugin IDs:
     * <ul>
     *   <li>{@code net.fabricmc.fabric-loom} - for non-obfuscated versions (Minecraft 26.1+)
     *   <li>{@code net.fabricmc.fabric-loom-remap} - for obfuscated versions (1.21.11 or older)
     * </ul>
     *
     * Older versions (1.21.11 and below) still ship an obfuscated game JAR and need the
     * remap-based pipeline.  Newer versions ship merged/official-mapped JARs where Loom
     * auto-configures Mojang mappings natively.
     */
    static boolean requiresOfficialMojangMappings(String minecraftVersion) {
        String[] segments = minecraftVersion.split("\\.");
        int[] parts = new int[segments.length];
        for (int i1 = 0; i1 < segments.length; i1++) {
            parts[i1] = Integer.parseInt(segments[i1]);
        }
        int[] cutoff = {1, 21, 11};
        for (int i = 0; i < cutoff.length; i++) {
            int currentPart = i < parts.length ? parts[i] : 0;
            if (currentPart != cutoff[i]) {
                return currentPart < cutoff[i];
            }
        }
        return true;
    }

    private static boolean usesOfficialMojangMappings(Project project) {
        return booleanProperty(
            project,
            "rapunzellib.fabric.official-mojang-mappings",
            "RAPUNZELLIB_FABRIC_OFFICIAL_MOJANG_MAPPINGS",
            "fabric.official-mojang-mappings",
            requiresOfficialMojangMappings(version(project, "minecraft"))
        );
    }

    private static boolean isFabricObfuscationDisabled(Project project) {
        return booleanProperty(
            project,
            "rapunzellib.fabric.disable-obfuscation",
            "RAPUNZELLIB_FABRIC_DISABLE_OBFUSCATION",
            "fabric.disable-obfuscation",
            !requiresOfficialMojangMappings(version(project, "minecraft"))
        );
    }

    private static boolean booleanProperty(Project project, String propertyName, String environmentName, String matrixKey, boolean defaultValue) {
        String configured = optionalProperty(project, propertyName, environmentName);
        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }
        String activeTarget = activeMinecraftTarget(project);
        if (activeTarget != null) {
            String matrixValue = targetMatrixProperty(project, activeTarget, matrixKey);
            if (matrixValue != null) {
                return Boolean.parseBoolean(matrixValue);
            }
        }
        return defaultValue;
    }

    private static String versionOverride(Project project, String alias) {
        String activeTarget = activeMinecraftTarget(project);
        if (activeTarget != null) {
            String targetOverride = optionalProperty(project, "rapunzellib.version." + activeTarget + "." + alias, null);
            if (targetOverride != null) {
                return targetOverride;
            }

            String globalOverride = optionalProperty(project, "rapunzellib.version." + alias, null);
            if (globalOverride != null) {
                return globalOverride;
            }

            String targetMatrixOverride = targetMatrixProperty(project, activeTarget, "version." + alias);
            if (targetMatrixOverride != null) {
                return targetMatrixOverride;
            }
        }

        String globalOverride = optionalProperty(project, "rapunzellib.version." + alias, null);
        if (globalOverride != null) {
            return globalOverride;
        }

        if ("minecraft".equals(alias) && activeTarget != null) {
            return activeTarget;
        }
        if ("paper-api".equals(alias) && activeTarget != null) {
            return activeTarget + "-R0.1-SNAPSHOT";
        }

        return null;
    }

    private static String activeMinecraftTarget(Project project) {
        String activeTarget = optionalProperty(project, "rapunzellib.minecraftTarget", "RAPUNZELLIB_MINECRAFT_TARGET");
        if (activeTarget != null) {
            return activeTarget;
        }
        activeTarget = optionalProperty(project, "rapunzellib.minecraftCoreVersion", "RAPUNZELLIB_MINECRAFT_CORE_VERSION");
        if (activeTarget != null) {
            return activeTarget;
        }
        Properties properties = targetMatrixProperties(project);
        if (properties == null) {
            return null;
        }
        return properties.getProperty("core");
    }

    private static String targetMatrixProperty(Project project, String activeTarget, String key) {
        Properties properties = targetMatrixProperties(project);
        if (properties == null) {
            return null;
        }
        return properties.getProperty("target." + targetToken(activeTarget) + "." + key);
    }

    private static String targetToken(String target) {
        return target.replaceAll("[^A-Za-z0-9]", "_");
    }

    private static Properties targetMatrixProperties(Project project) {
        File rootMatrix = project.getRootProject().file("gradle/minecraft-targets.properties");
        File includedBuildMatrix = project.getRootProject().file("../gradle/minecraft-targets.properties");
        File matrixFile = rootMatrix.isFile() ? rootMatrix : includedBuildMatrix;
        if (!matrixFile.isFile()) {
            return null;
        }
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(matrixFile)) {
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load Minecraft target matrix from " + matrixFile, ex);
        }
        return properties;
    }

    private static boolean isReposilitePublishTask(PublishToMavenRepository task) {
        if (task.getName().contains("ToReposiliteRepository")) {
            return true;
        }
        return task.getRepository() != null && REPOSILITE_REPOSITORY_NAME.equals(task.getRepository().getName());
    }

    private static String reposiliteRepositoryUrl(Project project) {
        String baseUrl = optionalProperty(project, "reposiliteBaseUrl", "REPOSILITE_BASE_URL");
        if (baseUrl == null) {
            baseUrl = DEFAULT_REPOSILITE_BASE_URL;
        }
        String repository = project.getVersion().toString().endsWith("SNAPSHOT") ? "snapshots" : "releases";
        return baseUrl.replaceAll("/+$", "") + "/" + repository;
    }

    private static String optionalProperty(Project project, String propertyName, String environmentName) {
        Object propertyValue = project.findProperty(propertyName);
        if (propertyValue instanceof String stringValue && !stringValue.trim().isEmpty()) {
            return stringValue.trim();
        }

        if (environmentName == null) {
            return null;
        }
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }
        return null;
    }

    private static Method firstMethod(Class<?> type, String name, Class<?> parameterType) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1 && method.getParameterTypes()[0] == parameterType) {
                return method;
            }
        }
        return null;
    }

    private static Method firstZeroArgMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                return method;
            }
        }
        return null;
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
