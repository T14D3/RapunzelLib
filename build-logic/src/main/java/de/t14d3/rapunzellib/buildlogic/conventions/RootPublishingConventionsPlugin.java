package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.BuildLogicPluginSupport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

import java.util.Objects;

import de.t14d3.rapunzellib.buildlogic.tasks.CheckReposiliteConfigTask;
import org.jetbrains.annotations.NotNull;

public final class RootPublishingConventionsPlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        // ── Reposilite (existing) ──────────────────────────────────────────

        TaskProvider<CheckReposiliteConfigTask> checkReposiliteConfig =
            BuildLogicPluginSupport.registerReposiliteConfigCheck(target);
        TaskProvider<Task> publishToReposilite =
            BuildLogicPluginSupport.registerPublishToReposilite(target);
        publishToReposilite.configure(task -> {
            task.dependsOn(checkReposiliteConfig);
            task.dependsOn(target.provider(() ->
                target.getSubprojects().stream()
                    .map(project -> project.getTasks().findByName("publishAllPublicationsToReposiliteRepository"))
                    .filter(Objects::nonNull)
                    .toList()
            ));
        });

        // ── Central Portal (via Nmcp - New Maven Central Publishing) ──────
        //
        // Nmcp is configured in settings.gradle.kts via the
        // com.gradleup.nmcp.settings plugin. It auto-applies nmcp to all
        // subprojects and nmcp.aggregation to root.
        //
        // Tasks created by Nmcp:
        //   publishAggregationToCentralPortal  - for releases
        //   publishAggregationToCentralSnapshots - for SNAPSHOTs
        // This wrapper provides a consistent entry point:
        //   ./gradlew publishToCentralPortal -Pversion=1.0.0

        TaskProvider<Task> publishToCentralPortal =
            target.getTasks().register("publishToCentralPortal", task -> {
                task.setGroup("publishing");
                task.setDescription("Publishes all Maven publications to Maven Central Portal (via Nmcp).");

                // Wire to the Nmcp aggregation task (handles both releases and SNAPSHOTs)
                task.dependsOn(target.provider(() -> {
                    String nmcpTaskName = target.getVersion().toString().endsWith("-SNAPSHOT")
                        ? "publishAggregationToCentralSnapshots"
                        : "publishAggregationToCentralPortal";
                    Task nmcpTask = target.getTasks().findByName(nmcpTaskName);
                    if (nmcpTask != null) return nmcpTask;
                    target.getLogger().warn(
                        "Nmcp task '{}' not found - is com.gradleup.nmcp.settings applied in settings.gradle.kts?",
                        nmcpTaskName
                    );
                    return target.getTasks().register("nmcpPlaceholder", t -> t.setEnabled(false)).get();
                }));

                task.doLast(t -> {
                    target.getLogger().lifecycle("");
                    target.getLogger().lifecycle("═══════════════════════════════════════════════════════════════");
                    target.getLogger().lifecycle("  Central Portal publish submitted.");
                    if (target.getVersion().toString().endsWith("-SNAPSHOT")) {
                        target.getLogger().lifecycle("  Deployment type: SNAPSHOT");
                        target.getLogger().lifecycle("  It may take a few minutes to appear at:");
                        target.getLogger().lifecycle("  https://central.sonatype.com/repository/maven-snapshots/");
                        target.getLogger().lifecycle("  Browse for groupId=de.t14d3.rapunzellib");
                    } else {
                        target.getLogger().lifecycle("  Deployment type: RELEASE");
                        target.getLogger().lifecycle("  Check the deployment status at:");
                        target.getLogger().lifecycle("  https://central.sonatype.com/publishing/deployments");
                        target.getLogger().lifecycle("  Once it shows 'PUBLISHING' or 'PUBLISHED', the artifacts");
                        target.getLogger().lifecycle("  will be available on Maven Central within ~15 minutes.");
                    }
                    target.getLogger().lifecycle("");
                    target.getLogger().lifecycle("  To see the raw Central Portal API response, re-run with:");
                    target.getLogger().lifecycle("  ./gradlew publishToCentralPortal -Pversion={} --info",
                        target.getVersion());
                    target.getLogger().lifecycle("═══════════════════════════════════════════════════════════════");
                    target.getLogger().lifecycle("");
                });
            });

        // ── Subproject configuration ───────────────────────────────────────

        target.subprojects(project -> {
            project.getPluginManager().withPlugin("java", ignored ->
                BuildLogicPluginSupport.configureMavenPublishing(project)
            );
            project.getPluginManager().withPlugin("maven-publish", ignored -> {
                // Reposilite (existing)
                BuildLogicPluginSupport.configureReposilitePublishing(project, checkReposiliteConfig);
                // Signing with in-memory GPG key
                BuildLogicPluginSupport.configureSigning(project);
            });
        });
    }
}
