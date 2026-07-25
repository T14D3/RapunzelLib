package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class CommonModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        BuildLogicPluginSupport.addProjectDependency(target, "api", ":api");
        BuildLogicPluginSupport.addLibraryDependency(target, "implementation", "snakeyaml");
        BuildLogicPluginSupport.addLibraryDependency(target, "implementation", "adventure-minimessage");
    }
}
