package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.spongepowered.gradle.vanilla.MinecraftExtension;
import org.spongepowered.gradle.vanilla.repository.MinecraftPlatform;

import java.lang.reflect.Method;

final class ConventionPluginSupport {
    private static final int JAVA_VERSION = 21;

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
