package de.t14d3.rapunzellib.gradle.conventions;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class PlatformNeoForgeModuleConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getPluginManager().apply("de.t14d3.rapunzellib.backend-platform-module-conventions");
        target.getPluginManager().apply("de.t14d3.rapunzellib.neoforge-module-conventions");
    }
}
