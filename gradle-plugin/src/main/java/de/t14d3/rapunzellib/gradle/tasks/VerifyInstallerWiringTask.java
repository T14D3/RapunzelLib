package de.t14d3.rapunzellib.gradle.tasks;

import de.t14d3.rapunzellib.gradle.ModuleMatrix;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class VerifyInstallerWiringTask extends DefaultTask {
    private static final String GAME_EVENT_SUPPORT_CONTRIBUTOR_TYPE =
        "de.t14d3.rapunzellib.events.GameEventSupportContributor";

    @TaskAction
    public void verify() {
        List<String> missing = new ArrayList<>();

        for (ModuleMatrix.InstallerExpectation expectation : ModuleMatrix.installerExpectations(getProject().getRootProject())) {
            var module = getProject().getRootProject().findProject(":" + expectation.moduleName());
            if (module == null) {
                continue;
            }
            Path descriptor = module.getProjectDir().toPath().resolve("src/main/resources/META-INF/services/" + expectation.installerType());
            if (!hasDescriptorContent(descriptor)) {
                String relative = getProject().getRootProject().getProjectDir().toPath().relativize(descriptor).toString();
                missing.add(expectation.moduleName() + " -> " + relative);
            }
        }

        for (ModuleMatrix.FeatureModuleSpec spec : ModuleMatrix.FEATURE_SPECS) {
            if (!"gui".equals(spec.featureKey())) {
                continue;
            }
            getProject().getRootProject().getSubprojects().stream()
                .filter(module -> {
                    var parsed = ModuleMatrix.parseFeatureModule(module.getName());
                    return parsed != null && "gui".equals(parsed.featureKey());
                })
                .forEach(module -> {
                    Path descriptor = module.getProjectDir().toPath()
                        .resolve("src/main/resources/META-INF/services/" + GAME_EVENT_SUPPORT_CONTRIBUTOR_TYPE);
                    if (!hasDescriptorContent(descriptor)) {
                        String relative = getProject().getRootProject().getProjectDir().toPath().relativize(descriptor).toString();
                        missing.add(module.getName() + " -> " + relative);
                    }
                });
        }

        if (!missing.isEmpty()) {
            throw new GradleException("Missing installer service wiring:\n" + missing.stream().map(value -> "- " + value).collect(java.util.stream.Collectors.joining("\n")));
        }
        getLogger().lifecycle("Installer service wiring verified for the supported module matrix.");
    }

    private boolean hasDescriptorContent(Path descriptor) {
        try {
            return Files.isRegularFile(descriptor)
                && Files.readAllLines(descriptor).stream().map(String::trim).anyMatch(value -> !value.isEmpty() && !value.startsWith("#"));
        } catch (Exception ex) {
            return false;
        }
    }
}
