package de.t14d3.rapunzellib.gradle.conventions;

import de.t14d3.rapunzellib.gradle.ConventionPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class RootSubprojectConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.subprojects(project ->
            project.getPluginManager().withPlugin("java", ignored -> {
                ConventionPluginSupport.configureJavaToolchain(project);
                ConventionPluginSupport.addLibraryDependency(project, "compileOnly", "annotations");
                ConventionPluginSupport.addLibraryDependency(project, "testCompileOnly", "annotations");
                ConventionPluginSupport.addLibraryDependency(project, "testImplementation", "junit-jupiter");
                ConventionPluginSupport.addLibraryDependency(project, "testRuntimeOnly", "junit-platform-launcher");
            })
        );
    }
}
