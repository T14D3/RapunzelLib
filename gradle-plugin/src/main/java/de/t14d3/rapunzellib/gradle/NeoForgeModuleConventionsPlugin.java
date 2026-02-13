package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class NeoForgeModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        ConventionPluginSupport.applyBaseJavaModule(target);
        target.getPluginManager().apply("net.neoforged.moddev");
        ConventionPluginSupport.addFamilyAndSharedDependencies(target);
        ConventionPluginSupport.configureNeoForge(target);
    }
}
