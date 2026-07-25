package de.t14d3.rapunzellib.buildlogic.conventions;

import de.t14d3.rapunzellib.buildlogic.ModuleMatrix;
import de.t14d3.rapunzellib.buildlogic.catalog.RegistryCatalogSourceDefinition;
import de.t14d3.rapunzellib.buildlogic.tasks.CheckReposiliteConfigTask;
import de.t14d3.rapunzellib.buildlogic.tasks.GeneratePlatformAdapterScaffoldTask;
import de.t14d3.rapunzellib.buildlogic.tasks.VerifyInstallerWiringTask;
import de.t14d3.rapunzellib.buildlogic.tasks.VerifyRegistryCatalogParityTask;
import de.t14d3.rapunzellib.buildlogic.tasks.VerifySharedParityTask;
import de.t14d3.rapunzellib.gradle.RapunzelLibExtension;
import de.t14d3.rapunzellib.gradle.RegistryCatalogSpec;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal-only plugin that registers verification and scaffold tasks
 * on the root project and per-spec parity tasks on projects with the
 * RapunzelLib plugin applied.
 *
 * <p>This plugin is NOT part of the public gradle-plugin. It lives in
 * the internal build-logic included build and is only applied within
 * the RapunzelLib build itself.
 */
public final class InternalTasksPlugin implements Plugin<Project> {
    @Override
    public void apply(Project root) {
        // ── Root-level verification tasks ────────────────────────────────
        root.getTasks().register("rapunzellibVerifyInstallerWiring", VerifyInstallerWiringTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Verifies installer service descriptors are present for detected feature/platform modules.");
        });

        root.getTasks().register("rapunzellibVerifySharedParity", VerifySharedParityTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Checks Fabric/NeoForge modules stay aligned on minecraft + mappings assumptions.");
        });

        // ── Scaffold generation task (root-level) ────────────────────────
        root.getTasks().register("rapunzellibGeneratePlatformAdapterScaffold", GeneratePlatformAdapterScaffoldTask.class, task -> {
            task.setGroup("rapunzellib");
            task.setDescription("Generates scaffold modules for a RapunzelLib platform adapter.");

            task.getOutputDir().convention(root.getLayout().getProjectDirectory().dir("platform-adapter-scaffold"));
            task.getBasePackage().convention("de.t14d3.rapunzellib");
            task.getPlatformKey().convention("custom");
            task.getSharedCoreFamily().convention(ModuleMatrix.SHARED_CORE_FAMILY_AUTO);
            task.getSharedCoreFeatures().convention(java.util.Set.of());
            task.getFeatures().convention(java.util.Set.of("commands", "events", "gui", "inventory", "nbt"));
        });

        // ── Per-project registry catalog parity tasks ───────────────────
        root.getTasks().register("rapunzellibVerifyRegistryCatalogParity", task -> {
            task.setGroup("verification");
            task.setDescription("Verifies configured registry catalogs stay aligned across native sources.");
        });

        root.subprojects(project -> {
            project.getPlugins().withId("de.t14d3.rapunzellib", ignored -> {
                registerParityTasksForProject(root, project);
            });
        });
    }

    private void registerParityTasksForProject(Project root, Project project) {
        RapunzelLibExtension extension = project.getExtensions().findByType(RapunzelLibExtension.class);
        if (extension == null) {
            return;
        }

        TaskProvider<Task> aggregateParity = root.getTasks().named("rapunzellibVerifyRegistryCatalogParity");

        extension.getRegistryCatalogs().configureEach(spec -> {
            String className = defaultRegistryCatalogClassName(spec.getName());
            String taskName = "rapunzellibVerify" + className + "Parity";

            TaskProvider<VerifyRegistryCatalogParityTask> verifyTask = project.getTasks().register(
                taskName, VerifyRegistryCatalogParityTask.class, task -> {
                    task.setGroup("verification");
                    task.setDescription("Verifies the '" + spec.getName() + "' registry catalog stays aligned across configured native sources.");

                    task.getCatalogName().convention(spec.getDomainName());
                    task.getSourceDefinitions().set(project.provider(() -> {
                        List<String> encoded = new ArrayList<>();
                        encoded.add(RegistryCatalogSourceDefinition.fromSpec("canonical", spec.getSource()).encode());
                        for (RegistryCatalogSpec.NamedRegistryCatalogSourceSpec paritySource : spec.getParitySources()) {
                            encoded.add(RegistryCatalogSourceDefinition.fromSpec(paritySource.getName(), paritySource).encode());
                        }
                        return encoded;
                    }));
                    task.getParityClasspath().from(project.provider(() -> {
                        List<File> files = new ArrayList<>(spec.getSource().getClasspath().getFiles());
                        for (RegistryCatalogSpec.NamedRegistryCatalogSourceSpec paritySource : spec.getParitySources()) {
                            files.addAll(paritySource.getClasspath().getFiles());
                        }
                        return files;
                    }));
                });

            aggregateParity.configure(task -> task.dependsOn(verifyTask));
        });
    }

    private static String defaultRegistryCatalogClassName(String name) {
        StringBuilder builder = new StringBuilder();
        for (String segment : name.split("[^A-Za-z0-9]+")) {
            if (segment.isBlank()) {
                continue;
            }
            String normalized = segment.toLowerCase(java.util.Locale.ROOT);
            builder.append(Character.toUpperCase(normalized.charAt(0)));
            builder.append(normalized.substring(1));
        }
        return builder.isEmpty() ? "GeneratedRegistryCatalog" : builder.toString();
    }
}
