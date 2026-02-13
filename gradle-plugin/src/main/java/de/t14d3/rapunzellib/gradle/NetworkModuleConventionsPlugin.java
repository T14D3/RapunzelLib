package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class NetworkModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        ConventionPluginSupport.applyBaseJavaModule(target);
        ConventionPluginSupport.addProjectDependency(target, "api", ":api");
        ConventionPluginSupport.addProjectDependency(target, "implementation", ":common");
        ConventionPluginSupport.addLibraryDependency(target, "implementation", "gson");
    }
}
