package de.t14d3.rapunzellib.devrunner.platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface PlatformAdapter {

    String key();

    String fillProject();

    String defaultVersion();

    String modDirectory();

    List<String> bootstrapFiles();

    List<String> defaultJvmArgs(Path instanceDir);

    List<String> programArgs();

    String shutdownCommand();

    void bootstrapOnce(BootstrapContext ctx) throws Exception;

    void postBootstrap(PostBootstrapContext ctx) throws Exception;

    void installPlugin(Path instanceDir, Path pluginJar) throws IOException;

    record BootstrapContext(
        Path instanceDir,
        Path jarPath,
        String javaBin,
        List<String> jvmArgs,
        Map<String, String> variables
    ) {}

    record PostBootstrapContext(
        Path instanceDir,
        String serverName,
        int serverIndex,
        Map<String, String> variables
    ) {}
}
