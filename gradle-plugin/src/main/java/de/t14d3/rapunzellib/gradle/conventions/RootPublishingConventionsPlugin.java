package de.t14d3.rapunzellib.gradle.conventions;

import de.t14d3.rapunzellib.gradle.ConventionPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

import java.util.Objects;

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
            task.dependsOn(target.provider(() ->
                target.getSubprojects().stream()
                    .map(project -> project.getTasks().findByName("publishAllPublicationsToReposiliteRepository"))
                    .filter(Objects::nonNull)
                    .toList()
            ));
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
