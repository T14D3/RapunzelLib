package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class PaperApiModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        ConventionPluginSupport.applyBaseJavaModule(target);
        ConventionPluginSupport.addFamilyAndSharedDependencies(target);
        ConventionPluginSupport.addLibraryDependency(target, "compileOnly", "paper-api");
        ConventionPluginSupport.addLibraryDependency(target, "testImplementation", "paper-api");
    }
}
