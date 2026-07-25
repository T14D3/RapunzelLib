package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class RootSubprojectConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.subprojects(project ->
            project.getPluginManager().withPlugin("java", ignored -> {
                BuildLogicPluginSupport.configureJavaToolchain(project);
                BuildLogicPluginSupport.addLibraryDependency(project, "compileOnly", "annotations");
                BuildLogicPluginSupport.addLibraryDependency(project, "testCompileOnly", "annotations");
                BuildLogicPluginSupport.addLibraryDependency(project, "testImplementation", "junit-jupiter");
                BuildLogicPluginSupport.addLibraryDependency(project, "testRuntimeOnly", "junit-platform-launcher");
            })
        );
    }
}
