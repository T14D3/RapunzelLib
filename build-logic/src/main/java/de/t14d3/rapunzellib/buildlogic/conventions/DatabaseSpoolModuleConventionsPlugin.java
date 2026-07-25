package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class DatabaseSpoolModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        BuildLogicPluginSupport.addProjectDependency(target, "api", ":network");
        BuildLogicPluginSupport.addProjectDependency(target, "implementation", ":common");
        BuildLogicPluginSupport.addLibraryDependency(target, "implementation", "spool");
    }
}
