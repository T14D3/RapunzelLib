package de.t14d3.rapunzellib.devrunner.platform;

import de.t14d3.rapunzellib.serverrunner.ServerProcess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class VelocityPlatformAdapter extends FillV3PlatformAdapter {

    @Override
    public String key() {
        return "velocity";
    }

    @Override
    public String fillProject() {
        return "velocity";
    }

    @Override
    public String defaultVersion() {
        return "latest";
    }

    @Override
    public String modDirectory() {
        return "plugins";
    }

    @Override
    public List<String> bootstrapFiles() {
        return List.of("velocity.toml");
    }

    @Override
    public List<String> programArgs() {
        return List.of();
    }

    @Override
    public String shutdownCommand() {
        return "shutdown";
    }

    @Override
    public void bootstrapOnce(BootstrapContext ctx) throws Exception {
        Path instanceDir = ctx.instanceDir();
        Path velocityToml = instanceDir.resolve("velocity.toml");

        List<String> command = new ArrayList<>();
        command.add(ctx.javaBin());
        if (ctx.jvmArgs() != null) command.addAll(ctx.jvmArgs());
        command.add("-jar");
        command.add(ctx.jarPath().getFileName().toString());

        ServerProcess process = ServerProcess.start("velocity-bootstrap", instanceDir, command, null);

        long deadline = System.currentTimeMillis() + 30_000L;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) break;
            if (Files.exists(velocityToml)) break;
            Thread.sleep(250);
        }

        process.destroy();
        process.waitFor();

        if (!Files.exists(velocityToml)) {
            throw new IOException("Velocity did not generate velocity.toml within timeout");
        }
    }

    @Override
    public void postBootstrap(PostBootstrapContext ctx) throws Exception {
        // Post-bootstrap patching is handled by DevRunnerBootstrap
    }
}
