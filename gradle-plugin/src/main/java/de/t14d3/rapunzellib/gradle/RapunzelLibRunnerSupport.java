package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.tasks.RunServersTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class RapunzelLibRunnerSupport {
    private RapunzelLibRunnerSupport() {
    }

    public static void applyRunnerConventions(
        RunServersTask task,
        RunnerGradleProperties runnerProperties,
        boolean mysqlEnabledDefault
    ) {
        task.getPaperVersion().convention(runnerProperties.paperVersion());
        task.getPaperCount().convention(runnerProperties.paperCount());
        task.getPaperBasePort().convention(runnerProperties.paperBasePort());

        task.getVelocityEnabled().convention(runnerProperties.velocityEnabled());
        task.getVelocityVersion().convention(runnerProperties.velocityVersion());
        task.getVelocityPort().convention(runnerProperties.velocityPort());

        task.getJavaBin().convention(runnerProperties.javaBin());
        task.getJvmArgs().convention(runnerProperties.jvmArgs());

        task.getMysqlEnabled().convention(runnerProperties.mysqlEnabled(mysqlEnabledDefault));
        task.getMysqlPort().convention(runnerProperties.mysqlPort());
        task.getMysqlDatabase().convention(runnerProperties.mysqlDatabase());
        task.getMysqlRootPassword().convention(runnerProperties.mysqlRootPassword());
        task.getMysqlImage().convention(runnerProperties.mysqlImage());
        task.getMysqlContainerName().convention(runnerProperties.mysqlContainerName());
    }

    public static Provider<List<String>> perfAdditionalArgs(Project project, boolean forceJfr) {
        return project.provider(() -> {
            List<String> args = new ArrayList<>();

            boolean jfrEnabled = forceJfr || Boolean.parseBoolean((String) project.findProperty("multiJfr"));
            if (jfrEnabled) {
                args.add("--jfr");
                String settings = trimToNull((String) project.findProperty("multiJfrSettings"));
                if (settings != null) {
                    args.add("--jfr-settings");
                    args.add(settings);
                }
            }

            List<String> paperExtraPlugins = csvPaths((String) project.findProperty("multiPaperExtraPlugins"));
            List<String> velocityExtraPlugins = csvPaths((String) project.findProperty("multiVelocityExtraPlugins"));
            String sparkPaper = trimToEmpty((String) project.findProperty("multiSparkPaperPlugin"));
            String sparkVelocity = trimToEmpty((String) project.findProperty("multiSparkVelocityPlugin"));

            List<String> paperPlugins = new ArrayList<>(paperExtraPlugins);
            if (!sparkPaper.isBlank()) {
                paperPlugins.add(sparkPaper);
            }
            for (String rawPath : paperPlugins) {
                File jarFile = project.file(rawPath);
                if (!jarFile.isFile()) {
                    throw new GradleException("Paper extra plugin jar does not exist: " + rawPath);
                }
                args.add("--paper-extra-plugin");
                args.add(jarFile.getAbsolutePath());
            }

            List<String> velocityPlugins = new ArrayList<>(velocityExtraPlugins);
            if (!sparkVelocity.isBlank()) {
                velocityPlugins.add(sparkVelocity);
            }
            for (String rawPath : velocityPlugins) {
                File jarFile = project.file(rawPath);
                if (!jarFile.isFile()) {
                    throw new GradleException("Velocity extra plugin jar does not exist: " + rawPath);
                }
                args.add("--velocity-extra-plugin");
                args.add(jarFile.getAbsolutePath());
            }

            return args;
        });
    }

    public static Provider<Boolean> booleanGradleProperty(Project project, String name, boolean defaultValue) {
        return project.getProviders().gradleProperty(name).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    static List<String> csvPaths(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

final class RunnerGradleProperties {
    private final Project project;

    RunnerGradleProperties(Project project) {
        this.project = project;
    }

    Provider<String> paperVersion() {
        return stringGradleProperty("multiPaperVersion", "1.21.10");
    }

    Provider<Integer> paperCount() {
        return intGradleProperty("multiPaperCount", 2);
    }

    Provider<Integer> paperBasePort() {
        return intGradleProperty("multiPaperBasePort", 25566);
    }

    Provider<Boolean> velocityEnabled() {
        return RapunzelLibRunnerSupport.booleanGradleProperty(project, "multiVelocityEnabled", true);
    }

    Provider<String> velocityVersion() {
        return stringGradleProperty("multiVelocityVersion", "latest");
    }

    Provider<Integer> velocityPort() {
        return intGradleProperty("multiVelocityPort", 25565);
    }

    Provider<String> javaBin() {
        return stringGradleProperty("multiRunnerJava", "");
    }

    Provider<List<String>> jvmArgs() {
        return csvGradleProperty("multiRunnerJvmArgs");
    }

    Provider<Integer> mysqlPort() {
        return intGradleProperty("multiMysqlPort", 3307);
    }

    Provider<String> mysqlDatabase() {
        return stringGradleProperty("multiMysqlDatabase", "rapunzellib");
    }

    Provider<String> mysqlRootPassword() {
        return stringGradleProperty("multiMysqlRootPassword", "root");
    }

    Provider<String> mysqlImage() {
        return stringGradleProperty("multiMysqlImage", "mysql:latest");
    }

    Provider<String> mysqlContainerName() {
        return stringGradleProperty("multiMysqlContainerName", "");
    }

    Provider<Boolean> mysqlEnabled(boolean defaultValue) {
        return RapunzelLibRunnerSupport.booleanGradleProperty(project, "multiMysql", defaultValue);
    }

    private Provider<String> stringGradleProperty(String name, String defaultValue) {
        return project.getProviders().gradleProperty(name).orElse(defaultValue);
    }

    private Provider<Integer> intGradleProperty(String name, int defaultValue) {
        return project.getProviders().gradleProperty(name).map(Integer::parseInt).orElse(defaultValue);
    }

    private Provider<List<String>> csvGradleProperty(String name) {
        return project.getProviders().gradleProperty(name).map(RapunzelLibRunnerSupport::csvPaths).orElse(List.of());
    }
}
