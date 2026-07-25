package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class PaperUserdevModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        target.getPluginManager().apply("io.papermc.paperweight.userdev");
        BuildLogicPluginSupport.addFamilyAndSharedDependencies(target);
        BuildLogicPluginSupport.configurePaperweightUserdev(target);
    }
}
