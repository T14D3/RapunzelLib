package de.t14d3.rapunzellib.devrunner.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ServiceAdapter {

    String key();

    String defaultImage();

    List<String> dockerRunCommand(String containerName, ServiceSpec spec, Path workDir);

    boolean containerRunning(Path workDir, String containerName);

    void startContainer(String containerName, ServiceSpec spec, Path workDir) throws Exception;

    boolean waitForReady(String containerName, Path workDir, long timeoutMs);

    void cleanup(String containerName, Path workDir);

    record ServiceSpec(
        String type,
        String image,
        Map<String, String> ports,
        Map<String, String> env,
        String containerName
    ) {}
}
