package de.t14d3.rapunzellib.gradle.conventions;

import de.t14d3.rapunzellib.gradle.ConventionPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class FabricModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        ConventionPluginSupport.applyBaseJavaModule(target);
        if (ConventionPluginSupport.isFabricLoomEnabled(target)) {
            ConventionPluginSupport.configureFabricLoomProperties(target);
            target.getPluginManager().apply("net.fabricmc.fabric-loom");
        }
        ConventionPluginSupport.configureFabricDependencyRouting(target);
        ConventionPluginSupport.addFamilyAndSharedDependencies(target);
        if (ConventionPluginSupport.isFabricLoomEnabled(target)) {
            ConventionPluginSupport.configureFabricLoom(target);
        } else {
            ConventionPluginSupport.configureFabricWithoutLoom(target);
        }
    }
}
