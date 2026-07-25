package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class NetworkModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        BuildLogicPluginSupport.addProjectDependency(target, "api", ":api");
        BuildLogicPluginSupport.addProjectDependency(target, "implementation", ":common");
        BuildLogicPluginSupport.addLibraryDependency(target, "implementation", "gson");
    }
}
