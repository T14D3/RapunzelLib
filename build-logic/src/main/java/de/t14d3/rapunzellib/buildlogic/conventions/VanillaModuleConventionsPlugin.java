package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class VanillaModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        target.getPluginManager().apply("org.spongepowered.gradle.vanilla");
        BuildLogicPluginSupport.addFamilyAndSharedDependencies(target);
        BuildLogicPluginSupport.configureVanillaMinecraft(target);
    }
}
