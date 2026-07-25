package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class PaperApiModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        BuildLogicPluginSupport.addFamilyAndSharedDependencies(target);
        BuildLogicPluginSupport.addLibraryDependency(target, "compileOnly", "paper-api");
        BuildLogicPluginSupport.addLibraryDependency(target, "testImplementation", "paper-api");
    }
}
