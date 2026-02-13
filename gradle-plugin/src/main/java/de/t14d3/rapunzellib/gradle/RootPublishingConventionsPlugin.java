package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class RootPublishingConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.subprojects(project ->
            project.getPluginManager().withPlugin("java", ignored ->
                ConventionPluginSupport.configureMavenPublishing(project)
            )
        );
    }
}
