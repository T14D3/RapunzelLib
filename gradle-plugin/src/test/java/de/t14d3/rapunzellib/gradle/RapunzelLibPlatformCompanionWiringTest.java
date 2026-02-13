package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RapunzelLibPlatformCompanionWiringTest {
    @Test
    void fabricWiringNestsMainAndCompanionAndKeepsCompanionInLocalRuntime() {
        Project project = newJavaProject();
        project.getConfigurations().create("include");
        project.getConfigurations().create("localRuntime");
        project.getDependencies().add("implementation", "de.t14d3.rapunzellib:platform-fabric:0.3.0-SNAPSHOT");

        RapunzelLibPlatformCompanionWiring.configureFabricCompanionDependencies(project);

        assertDependency("include", project, "platform-fabric", "0.3.0-SNAPSHOT", null);
        assertDependency("include", project, "platform-fabric", "0.3.0-SNAPSHOT", "companion");
        assertDependency("localRuntime", project, "platform-fabric", "0.3.0-SNAPSHOT", "companion");
    }

    @Test
    void fabricWiringCanResolveVersionFromBom() {
        Project project = newJavaProject();
        project.getConfigurations().create("include");
        project.getConfigurations().create("localRuntime");
        project.getDependencies().add("implementation", "de.t14d3.rapunzellib:bom:0.3.0-SNAPSHOT");
        project.getDependencies().add(
            "implementation",
            project.getDependencies().create(java.util.Map.of("group", "de.t14d3.rapunzellib", "name", "platform-fabric"))
        );

        RapunzelLibPlatformCompanionWiring.configureFabricCompanionDependencies(project);

        assertDependency("include", project, "platform-fabric", "0.3.0-SNAPSHOT", null);
        assertDependency("include", project, "platform-fabric", "0.3.0-SNAPSHOT", "companion");
    }

    @Test
    void fabricWiringIsIdempotent() {
        Project project = newJavaProject();
        project.getConfigurations().create("include");
        project.getConfigurations().create("localRuntime");
        project.getDependencies().add("implementation", "de.t14d3.rapunzellib:platform-fabric:0.3.0-SNAPSHOT");

        RapunzelLibPlatformCompanionWiring.configureFabricCompanionDependencies(project);
        RapunzelLibPlatformCompanionWiring.configureFabricCompanionDependencies(project);

        assertEquals(1, countDependencies("include", project, "platform-fabric", "0.3.0-SNAPSHOT", null));
        assertEquals(1, countDependencies("include", project, "platform-fabric", "0.3.0-SNAPSHOT", "companion"));
        assertEquals(1, countDependencies("localRuntime", project, "platform-fabric", "0.3.0-SNAPSHOT", "companion"));
    }

    @Test
    void neoforgeWiringEmbedsMainAndCompanionAndKeepsCompanionInDevRuntimeOnly() {
        Project project = newJavaProject();
        project.getConfigurations().create("jarJar");
        project.getDependencies().add("implementation", "de.t14d3.rapunzellib:platform-neoforge:0.3.0-SNAPSHOT");

        RapunzelLibPlatformCompanionWiring.configureNeoForgeCompanionDependencies(project);

        assertDependency("jarJar", project, "platform-neoforge", "0.3.0-SNAPSHOT", null);
        assertDependency("jarJar", project, "platform-neoforge", "0.3.0-SNAPSHOT", "companion");
        assertDependency("rapunzellibDevRuntime", project, "platform-neoforge", "0.3.0-SNAPSHOT", "companion");
        assertTrue(project.getConfigurations().getByName("runtimeClasspath").getExtendsFrom().stream().anyMatch(configuration -> configuration.getName().equals("rapunzellibDevRuntime")));
        assertFalse(project.getConfigurations().getByName("runtimeOnly").getDependencies().stream().anyMatch(dependency ->
            dependency instanceof ExternalModuleDependency external
                && "de.t14d3.rapunzellib".equals(external.getGroup())
                && "platform-neoforge".equals(external.getName())
                && external.getArtifacts().size() == 1
                && "companion".equals(external.getArtifacts().iterator().next().getClassifier())
        ));
    }

    private Project newJavaProject() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("java-library");
        return project;
    }

    private void assertDependency(String configurationName, Project project, String artifact, String version, String classifier) {
        assertEquals(
            1,
            countDependencies(configurationName, project, artifact, version, classifier),
            "Expected " + configurationName + " to contain " + artifact + ":" + version + (classifier != null ? ":" + classifier : "")
        );
    }

    private int countDependencies(String configurationName, Project project, String artifact, String version, String classifier) {
        return (int) project.getConfigurations().getByName(configurationName).getDependencies().withType(ExternalModuleDependency.class).stream()
            .filter(dependency ->
                "de.t14d3.rapunzellib".equals(dependency.getGroup())
                    && artifact.equals(dependency.getName())
                    && version.equals(dependency.getVersion())
                    && java.util.Objects.equals(
                    dependency.getArtifacts().size() == 1 ? dependency.getArtifacts().iterator().next().getClassifier() : null,
                    classifier
                )
            )
            .count();
    }
}
