package de.t14d3.rapunzellib.gradle.conventions;

import de.t14d3.rapunzellib.gradle.ConventionPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class CommonModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        ConventionPluginSupport.applyBaseJavaModule(target);
        ConventionPluginSupport.addProjectDependency(target, "api", ":api");
        ConventionPluginSupport.addLibraryDependency(target, "implementation", "snakeyaml");
        ConventionPluginSupport.addLibraryDependency(target, "implementation", "adventure-minimessage");
    }
}
