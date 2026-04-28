package de.t14d3.rapunzellib.gradle.conventions;

import de.t14d3.rapunzellib.gradle.ConventionPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class BackendPlatformModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        ConventionPluginSupport.applyBaseJavaModule(target);
        ConventionPluginSupport.addProjectDependency(target, "implementation", ":common");
        ConventionPluginSupport.addProjectDependency(target, "implementation", ":network");
        ConventionPluginSupport.addProjectDependency(target, "implementation", ":database-spool");
    }
}
