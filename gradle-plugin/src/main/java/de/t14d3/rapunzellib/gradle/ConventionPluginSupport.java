package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
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

import java.lang.reflect.Method;

import de.t14d3.rapunzellib.gradle.tasks.CheckReposiliteConfigTask;

final class ConventionPluginSupport {
    private static final int JAVA_VERSION = 21;
    private static final String REPOSILITE_REPOSITORY_NAME = "reposilite";
    private static final String DEFAULT_REPOSILITE_BASE_URL = "https://maven.t14d3.de";

    private ConventionPluginSupport() {
    }

    static void applyBaseJavaModule(Project project) {
        project.getPluginManager().apply("java-library");
        project.getPluginManager().apply("de.t14d3.rapunzellib");
        configureJavaToolchain(project);
    }

    static void configureJavaToolchain(Project project) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        if (java != null) {
            java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(JAVA_VERSION));
            java.withSourcesJar();
            java.withJavadocJar();
        }
        project.getTasks().withType(JavaCompile.class).configureEach(task -> task.getOptions().getRelease().set(JAVA_VERSION));
        project.getTasks().withType(Test.class).configureEach(Test::useJUnitPlatform);
    }

    static void configureMavenPublishing(Project project) {
        if (isInternalPublishModule(project.getName())) {
            return;
        }

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
        attachArtifactIfPresent(project, publication, "shadowJar");
        attachArtifactIfPresent(project, publication, "remapShadowJar");
    }

    static TaskProvider<CheckReposiliteConfigTask> registerReposiliteConfigCheck(Project rootProject) {
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

    static TaskProvider<Task> registerPublishToReposilite(Project rootProject) {
        return rootProject.getTasks().register("publishToReposilite", task -> {
            task.setGroup("publishing");
            task.setDescription("Publishes all Reposilite-targeted Maven publications from publishable subprojects.");
        });
    }

    static void configureReposilitePublishing(Project project, TaskProvider<CheckReposiliteConfigTask> checkReposiliteConfig) {
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

    static void addLibraryDependency(Project project, String configuration, String alias) {
        project.getDependencies().add(configuration, library(project, alias));
    }

    static void addProjectDependency(Project project, String configuration, String path) {
        if (path.equals(project.getPath())) {
            return;
        }
        if (project.getRootProject().findProject(path) != null) {
            project.getDependencies().add(configuration, project.project(path));
        }
    }

    static void addFamilyAndSharedDependencies(Project project) {
        String family = project.getName().substring(0, project.getName().indexOf('-'));
        addProjectDependency(project, "api", ":" + family);
        addProjectDependency(project, "implementation", ":" + family + "-shared");
    }

    static void configurePaperweightUserdev(Project project) {
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

    static void configureFabricLoom(Project project) {
        Object loom = project.getExtensions().findByName("loom");
        if (loom == null) {
            return;
        }
        project.getDependencies().add("minecraft", library(project, "minecraft"));
        Method mappingsMethod = firstZeroArgMethod(loom.getClass(), "officialMojangMappings");
        if (mappingsMethod != null) {
            Object mappings = invoke(mappingsMethod, loom);
            if (mappings != null) {
                project.getDependencies().add("mappings", mappings);
            }
        }
        project.getDependencies().add("modImplementation", library(project, "fabric-loader"));
    }

    static void configureNeoForge(Project project) {
        Object extension = project.getExtensions().findByName("neoForge");
        if (extension == null) {
            return;
        }
        Method method = firstMethod(extension.getClass(), "setVersion", String.class);
        if (method != null) {
            invoke(method, extension, version(project, "neoforge"));
        }
    }

    static void configureVanillaMinecraft(Project project) {
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

    private static void attachArtifactIfPresent(Project project, MavenPublication publication, String taskName) {
        Task task = project.getTasks().findByName(taskName);
        if (task != null) {
            publication.artifact(task);
        }
    }

    private static Object library(Project project, String alias) {
        return libs(project).findLibrary(alias).orElseThrow().get();
    }

    private static String version(Project project, String alias) {
        return libs(project).findVersion(alias).orElseThrow().getRequiredVersion();
    }

    private static boolean isInternalPublishModule(String projectName) {
        if ("common".equals(projectName)) {
            return true;
        }
        return "platform-shared".equals(projectName) || projectName.endsWith("-shared");
    }

    static boolean publishesToReposilite(Project project) {
        String projectName = project.getName();
        if ("bom".equals(projectName) || "gradle-plugin".equals(projectName)) {
            return true;
        }
        return !isInternalPublishModule(projectName);
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
