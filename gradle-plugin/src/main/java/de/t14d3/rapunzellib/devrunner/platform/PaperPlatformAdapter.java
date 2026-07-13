package de.t14d3.rapunzellib.devrunner.platform;

import de.t14d3.rapunzellib.serverrunner.ServerProcess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PaperPlatformAdapter extends FillV3PlatformAdapter {

    @Override
    public String key() {
        return "paper";
    }

    @Override
    public String fillProject() {
        return "paper";
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
        // eula.txt and server.properties must exist before the server starts.
        // paper-global.yml is handled via fileOverrides (template with {{velocity_*}}).
        return List.of("eula.txt", "server.properties");
    }

    @Override
    public List<String> programArgs() {
        return List.of("--nogui");
    }

    @Override
    public String shutdownCommand() {
        return "stop";
    }

    @Override
    public void bootstrapOnce(BootstrapContext ctx) throws Exception {
        Path instanceDir = ctx.instanceDir();
        Map<String, String> vars = ctx.variables();
        String port = vars.getOrDefault("server_port", "25565");
        String serverName = vars.getOrDefault("server_name", "paper");

        // Write EULA
        Path eulaFile = instanceDir.resolve("eula.txt");
        Files.writeString(eulaFile, "eula=true\n");

        // Write default server.properties
        Path serverProps = instanceDir.resolve("server.properties");
        if (!Files.exists(serverProps)) {
            Files.writeString(serverProps,
                "server-port=" + port + "\n" +
                "online-mode=false\n" +
                "enable-rcon=false\n" +
                "enable-query=false\n" +
                "enforce-secure-profile=false\n" +
                "motd=RapunzelDev " + serverName + "\n"
            );
        }

        List<String> command = new ArrayList<>();
        command.add(ctx.javaBin());
        if (ctx.jvmArgs() != null) command.addAll(ctx.jvmArgs());
        command.add("-jar");
        command.add(ctx.jarPath().getFileName().toString());
        command.addAll(programArgs());

        // The bootstrap server will generate config/paper-global.yml.
        // We wait for it so we know plugins have been loaded.
        Path configDir = instanceDir.resolve("config");
        Path paperGlobal = configDir.resolve("paper-global.yml");

        ServerProcess process = ServerProcess.start("paper-bootstrap", ctx.instanceDir(), command, null);

        long deadline = System.currentTimeMillis() + 45_000L;
        boolean ready = false;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) break;
            if (Files.exists(paperGlobal) && hasPluginDataDir(instanceDir)) {
                ready = true;
                break;
            }
            Thread.sleep(250);
        }

        if (ready) {
            // Wait a moment for the server to fully initialize its command system
            Thread.sleep(3_000L);
            // Clean shutdown via "stop" command
            process.sendLine("stop");
            process.waitFor(15_000L);
        }
        if (process.isAlive()) {
            process.destroy();
            process.waitFor(5_000L);
        }

        // Remove world data created by bootstrap so the real server starts fresh
        deleteDirectoryRecursive(instanceDir.resolve("world"));
        deleteDirectoryRecursive(instanceDir.resolve("world_nether"));
        deleteDirectoryRecursive(instanceDir.resolve("world_the_end"));
        // Also remove any session lock that may remain
        Files.deleteIfExists(instanceDir.resolve("world/session.lock"));
        // Nuke any SQLite databases created during bootstrap so the real server
        // starts with a completely clean slate (avoids Spool migration conflicts)
        try (var stream = Files.walk(instanceDir, 6)) {
            stream.filter(p -> p.toString().endsWith(".db"))
                  .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    @Override
    public void postBootstrap(PostBootstrapContext ctx) throws Exception {
        // Post-bootstrap patching is handled by DevRunnerBootstrap and fileOverrides
    }

    private boolean hasPluginDataDir(Path instanceDir) {
        Path pluginsDir = instanceDir.resolve("plugins");
        if (!Files.isDirectory(pluginsDir)) return false;
        try (var stream = Files.list(pluginsDir)) {
            return stream.anyMatch(Files::isDirectory);
        } catch (IOException e) {
            return false;
        }
    }

    private void deleteDirectoryRecursive(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
        }
    }
}
