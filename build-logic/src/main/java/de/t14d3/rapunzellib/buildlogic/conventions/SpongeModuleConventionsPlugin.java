package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class SpongeModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        BuildLogicPluginSupport.addFamilyAndSharedDependencies(target);
        BuildLogicPluginSupport.addLibraryDependency(target, "compileOnly", "sponge-api");
        BuildLogicPluginSupport.addLibraryDependency(target, "testImplementation", "sponge-api");
    }
}
