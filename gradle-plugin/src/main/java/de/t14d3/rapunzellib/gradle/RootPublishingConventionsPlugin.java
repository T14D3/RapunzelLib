package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

import de.t14d3.rapunzellib.gradle.tasks.CheckReposiliteConfigTask;

public final class RootPublishingConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        TaskProvider<CheckReposiliteConfigTask> checkReposiliteConfig =
            ConventionPluginSupport.registerReposiliteConfigCheck(target);
        TaskProvider<Task> publishToReposilite =
            ConventionPluginSupport.registerPublishToReposilite(target);
        publishToReposilite.configure(task -> {
            task.dependsOn(checkReposiliteConfig);
            target.getSubprojects().stream()
                .filter(ConventionPluginSupport::publishesToReposilite)
                .map(project -> project.getPath() + ":publishAllPublicationsToReposiliteRepository")
                .forEach(task::dependsOn);
        });

        target.subprojects(project -> {
            project.getPluginManager().withPlugin("java", ignored ->
                ConventionPluginSupport.configureMavenPublishing(project)
            );
            project.getPluginManager().withPlugin("maven-publish", ignored -> {
                ConventionPluginSupport.configureReposilitePublishing(project, checkReposiliteConfig);
            });
        });
    }
}
