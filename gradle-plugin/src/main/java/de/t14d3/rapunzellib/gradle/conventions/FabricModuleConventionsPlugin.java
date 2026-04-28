package de.t14d3.rapunzellib.gradle.conventions;

import de.t14d3.rapunzellib.gradle.ConventionPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class FabricModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        ConventionPluginSupport.applyBaseJavaModule(target);
        target.getPluginManager().apply("fabric-loom");
        ConventionPluginSupport.addFamilyAndSharedDependencies(target);
        ConventionPluginSupport.configureFabricLoom(target);
    }
}
