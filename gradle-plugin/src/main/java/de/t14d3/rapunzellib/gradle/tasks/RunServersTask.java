package de.t14d3.rapunzellib.gradle.tasks;

import de.t14d3.rapunzellib.serverrunner.ServerRunnerMain;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@DisableCachingByDefault
public abstract class RunServersTask extends DefaultTask {
    public RunServersTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    @Input
    public abstract Property<String> getPaperVersion();

    @Input
    public abstract Property<Integer> getPaperCount();

    @Input
    public abstract Property<Integer> getPaperBasePort();

    @InputFile
    @Optional
    @CompileClasspath
    public abstract RegularFileProperty getPaperPluginJar();

    @Input
    public abstract Property<Boolean> getVelocityEnabled();

    @Input
    public abstract Property<String> getVelocityVersion();

    @Input
    public abstract Property<Integer> getVelocityPort();

    @InputFile
    @Optional
    @CompileClasspath
    public abstract RegularFileProperty getVelocityPluginJar();

    @Input
    public abstract Property<String> getJavaBin();

    @Input
    public abstract ListProperty<String> getJvmArgs();

    @Input
    public abstract Property<Boolean> getMysqlEnabled();

    @Input
    public abstract Property<Integer> getMysqlPort();

    @Input
    public abstract Property<String> getMysqlDatabase();

    @Input
    public abstract Property<String> getMysqlRootPassword();

    @Input
    public abstract Property<String> getMysqlImage();

    @Input
    public abstract Property<String> getMysqlContainerName();

    @Input
    public abstract ListProperty<String> getRegexReplaces();

    @Input
    public abstract ListProperty<String> getAdditionalArgs();

    @OutputDirectory
    public abstract DirectoryProperty getBaseDir();

    public void replace(String relativePath, String regex, String replacement) {
        getRegexReplaces().add(relativePath);
        getRegexReplaces().add(regex);
        getRegexReplaces().add(replacement);
    }

    List<String> buildCliArgs() {
        List<String> cliArgs = new ArrayList<>();

        cliArgs.add("--paper-version");
        cliArgs.add(getPaperVersion().get());
        cliArgs.add("--paper-count");
        cliArgs.add(getPaperCount().get().toString());
        cliArgs.add("--paper-base-port");
        cliArgs.add(getPaperBasePort().get().toString());

        if (getPaperPluginJar().isPresent()) {
            File jar = getPaperPluginJar().get().getAsFile();
            cliArgs.add("--paper-plugin");
            cliArgs.add(jar.getAbsolutePath());
        }

        if (getVelocityEnabled().get()) {
            cliArgs.add("--velocity-version");
            cliArgs.add(getVelocityVersion().get());
            cliArgs.add("--velocity-port");
            cliArgs.add(getVelocityPort().get().toString());

            if (getVelocityPluginJar().isPresent()) {
                File jar = getVelocityPluginJar().get().getAsFile();
                cliArgs.add("--velocity-plugin");
                cliArgs.add(jar.getAbsolutePath());
            }
        }

        String javaBinValue = getJavaBin().getOrNull();
        if (javaBinValue != null && !javaBinValue.trim().isEmpty()) {
            cliArgs.add("--java");
            cliArgs.add(javaBinValue.trim());
        }

        for (String arg : getJvmArgs().get()) {
            cliArgs.add("--jvm-arg");
            cliArgs.add(arg);
        }

        cliArgs.add("--base-dir");
        cliArgs.add(getBaseDir().get().getAsFile().getAbsolutePath());

        if (getMysqlEnabled().get()) {
            cliArgs.add("--mysql");
            cliArgs.add("--mysql-port");
            cliArgs.add(getMysqlPort().get().toString());
            cliArgs.add("--mysql-database");
            cliArgs.add(getMysqlDatabase().get());
            cliArgs.add("--mysql-root-password");
            cliArgs.add(getMysqlRootPassword().get());
            cliArgs.add("--mysql-image");
            cliArgs.add(getMysqlImage().get());

            String containerName = getMysqlContainerName().getOrNull();
            if (containerName != null && !containerName.trim().isEmpty()) {
                cliArgs.add("--mysql-container-name");
                cliArgs.add(containerName.trim());
            }
        }

        List<String> replaceArgs = getRegexReplaces().get();
        if (replaceArgs.size() % 3 != 0) {
            throw new GradleException("regexReplaces must be configured in groups of 3: <path>, <regex>, <replacement>");
        }
        for (int index = 0; index < replaceArgs.size(); index += 3) {
            cliArgs.add("--replace");
            cliArgs.add(replaceArgs.get(index));
            cliArgs.add(replaceArgs.get(index + 1));
            cliArgs.add(replaceArgs.get(index + 2));
        }

        cliArgs.addAll(getAdditionalArgs().get());
        return cliArgs;
    }

    @TaskAction
    public void runServers() {
        List<String> cliArgs = buildCliArgs();
        int exitCode;
        try {
            exitCode = ServerRunnerMain.run(cliArgs.toArray(String[]::new));
        } catch (Throwable throwable) {
            throw new GradleException("server-runner failed: " + throwable.getMessage(), throwable);
        }
        if (exitCode != 0) {
            throw new GradleException("server-runner exited with code " + exitCode);
        }
    }
}
