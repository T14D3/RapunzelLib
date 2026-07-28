package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class BackendPlatformModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        BuildLogicPluginSupport.addProjectDependency(target, "implementation", ":common");
        BuildLogicPluginSupport.addProjectDependency(target, "implementation", ":network");
        BuildLogicPluginSupport.addProjectDependency(target, "implementation", ":database-spool");
        BuildLogicPluginSupport.addProjectDependency(target, "api", ":platform-shared");
    }
}
