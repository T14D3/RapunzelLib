package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class NeoForgeModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        target.getPluginManager().apply("net.neoforged.moddev");
        BuildLogicPluginSupport.addFamilyAndSharedDependencies(target);
        BuildLogicPluginSupport.configureNeoForge(target);
    }
}
