package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.RegistryCatalogSpec.NamedRegistryCatalogSourceSpec;
import de.t14d3.rapunzellib.gradle.RegistryCatalogSpec.RegistryCatalogSourceSpec;
import de.t14d3.rapunzellib.gradle.catalog.RegistryCatalogSourceDefinition;
import de.t14d3.rapunzellib.gradle.tasks.GenerateRegistryCatalogTask;
import de.t14d3.rapunzellib.gradle.tasks.VerifyRegistryCatalogParityTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RapunzelLibRegistryCatalogSupport {
    static final String WIRED_SOURCE_DIRS_KEY = "rapunzellib.wiredSourceDirs";
    private RapunzelLibRegistryCatalogSupport() {
    }

    @SuppressWarnings("unchecked")
    public static RegistryCatalogTasks registerRegistryCatalogTasks(Project project, RapunzelLibExtension extension) {
        TaskProvider<Task> generateAll = project.getTasks().register("rapunzellibGenerateRegistryCatalogs", task -> {
            task.setGroup("rapunzellib");
            task.setDescription("Generates typed Java registry-ref catalogs from configured catalog specs.");
        });

        TaskProvider<Task> verifyParity = project.getTasks().register("rapunzellibVerifyRegistryCatalogParity", task -> {
            task.setGroup("verification");
            task.setDescription("Verifies configured registry catalogs stay aligned across native sources.");
        });

        Provider<Boolean> includeVerifyParityInCheck =
            RapunzelLibRunnerSupport.booleanGradleProperty(project, "rapunzellib.registryParityInCheck", false);

        project.getExtensions().getExtraProperties().set(WIRED_SOURCE_DIRS_KEY, new HashSet<File>());

        extension.getRegistryCatalogs().configureEach(spec -> {
            TaskProvider<GenerateRegistryCatalogTask> generateCatalog = registerRegistryCatalogGenerationTask(project, spec);
            generateAll.configure(task -> task.dependsOn(generateCatalog));

            TaskProvider<VerifyRegistryCatalogParityTask> verifyCatalogParity = registerRegistryCatalogParityTask(project, spec);
            verifyParity.configure(task -> task.dependsOn(verifyCatalogParity));

            project.getPlugins().withId("java", ignored -> wireRegistryCatalogSourceDirectory(project, generateCatalog));
        });

        return new RegistryCatalogTasks(generateAll, verifyParity, includeVerifyParityInCheck);
    }

    private static TaskProvider<GenerateRegistryCatalogTask> registerRegistryCatalogGenerationTask(
        Project project,
        RegistryCatalogSpec spec
    ) {
        String taskName = "rapunzellibGenerate" + RapunzelLibPluginDefaults.defaultRegistryCatalogClassName(spec.getName());
        return project.getTasks().register(taskName, GenerateRegistryCatalogTask.class, task -> {
            task.setGroup("rapunzellib");
            task.setDescription("Generates the '" + spec.getName() + "' registry catalog.");

            task.getPackageName().convention(spec.getPackageName());
            task.getClassName().convention(spec.getClassName());
            task.getDomainName().convention(spec.getDomainName());
            task.getRegistryValueType().convention(spec.getRegistryValueType());
            task.getRegistryKeyOwnerType().convention(spec.getRegistryKeyOwnerType());
            task.getRegistryKeyFieldName().convention(spec.getRegistryKeyFieldName());
            task.getOutputDir().convention(spec.getOutputDir());

            RegistryCatalogSourceSpec source = spec.getSource();
            task.getSourceType().convention(source.getType());
            task.getNativeSourceClasspath().from(source.getClasspath());
            task.getNormalizationProfile().convention(source.getNormalizationProfile());
            task.getNativeEnumClassName().convention(source.getEnumClassName());
            task.getNativeStaticFieldOwnerClassName().convention(source.getStaticFieldOwnerClassName());
            task.getNativeStaticFieldValueTypeName().convention(source.getStaticFieldValueTypeName());
            task.getNativeIncludePredicateMethods().convention(source.getIncludePredicateMethods());
            task.getNativeExcludePredicateMethods().convention(source.getExcludePredicateMethods());
            task.getNativeKeyAccessorMethodName().convention(source.getKeyAccessorMethodName());
            task.getNativeExcludedEnumConstants().convention(source.getExcludedEnumConstants());
        });
    }

    private static TaskProvider<VerifyRegistryCatalogParityTask> registerRegistryCatalogParityTask(
        Project project,
        RegistryCatalogSpec spec
    ) {
        String taskName = "rapunzellibVerify" + RapunzelLibPluginDefaults.defaultRegistryCatalogClassName(spec.getName()) + "Parity";
        return project.getTasks().register(taskName, VerifyRegistryCatalogParityTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Verifies the '" + spec.getName() + "' registry catalog stays aligned across configured native sources.");

            task.getCatalogName().convention(spec.getDomainName());
            task.getSourceDefinitions().set(project.provider(() -> {
                List<String> encoded = new ArrayList<>();
                encoded.add(RegistryCatalogSourceDefinition.fromSpec("canonical", spec.getSource()).encode());
                for (NamedRegistryCatalogSourceSpec paritySource : spec.getParitySources()) {
                    encoded.add(RegistryCatalogSourceDefinition.fromSpec(paritySource.getName(), paritySource).encode());
                }
                return encoded;
            }));
            task.getParityClasspath().from(project.provider(() -> {
                List<File> files = new ArrayList<>(spec.getSource().getClasspath().getFiles());
                for (NamedRegistryCatalogSourceSpec paritySource : spec.getParitySources()) {
                    files.addAll(paritySource.getClasspath().getFiles());
                }
                return files;
            }));
        });
    }

    @SuppressWarnings("unchecked")
    private static void wireRegistryCatalogSourceDirectory(
        Project project,
        TaskProvider<GenerateRegistryCatalogTask> generateCatalog
    ) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        File outputDir = generateCatalog.flatMap(GenerateRegistryCatalogTask::getOutputDir).get().getAsFile();
        Set<File> wiredDirs = (Set<File>) project.getExtensions().getExtraProperties().get(WIRED_SOURCE_DIRS_KEY);
        if (wiredDirs.add(outputDir)) {
            main.getJava().srcDir(outputDir);
        }

        project.getTasks().named(main.getCompileJavaTaskName()).configure(task -> task.dependsOn(generateCatalog));
    }
}

record RegistryCatalogTasks(
    TaskProvider<Task> generateAll,
    TaskProvider<Task> verifyParity,
    Provider<Boolean> includeVerifyParityInCheck
) {
}
