package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class FabricModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        BuildLogicPluginSupport.applyBaseJavaModule(target);
        if (BuildLogicPluginSupport.isFabricLoomEnabled(target)) {
            target.getPluginManager().apply(BuildLogicPluginSupport.fabricLoomPluginId(target));
            BuildLogicPluginSupport.configureFabricLoomProperties(target);
        }
        BuildLogicPluginSupport.configureFabricDependencyRouting(target);
        BuildLogicPluginSupport.addFamilyAndSharedDependencies(target);
        if (BuildLogicPluginSupport.isFabricLoomEnabled(target)) {
            BuildLogicPluginSupport.configureFabricLoom(target);
        } else {
            BuildLogicPluginSupport.configureFabricWithoutLoom(target);
        }
    }
}
