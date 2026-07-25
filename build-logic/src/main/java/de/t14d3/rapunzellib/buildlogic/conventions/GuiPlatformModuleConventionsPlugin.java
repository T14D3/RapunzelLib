package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class GuiPlatformModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getPluginManager().apply("de.t14d3.rapunzellib.feature-platform-module-conventions");
        BuildLogicPluginSupport.addProjectDependency(target, "implementation", ":common");
    }
}
