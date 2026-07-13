package de.t14d3.rapunzellib.serverrunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @deprecated Use {@link de.t14d3.rapunzellib.devrunner.DevRunnerOrchestrator} instead.
 */
@Deprecated
final class ServerRunnerOrchestrator {
    private final ServerRunnerMain.Config cfg;
    private final ServerRunnerWorkspace workspace;
    private final FillV3Client fillClient;
    private final List<ServerProcess> processes = new ArrayList<>();

    private volatile String mysqlContainerName;

    ServerRunnerOrchestrator(ServerRunnerMain.Config cfg) {
        this.cfg = cfg;
        this.workspace = ServerRunnerWorkspace.resolve(cfg);
        this.fillClient = new FillV3Client();
    }

    int run() throws Exception {
        workspace.createDirectories();

        registerShutdownHook();

        String mysqlJdbc = startMysqlIfEnabled();
        ResolvedJars jars = resolveJars();
        validateResolvedJars(jars);

        String profilingRunId = Long.toString(System.currentTimeMillis());
        String velocityForwardingSecret = startVelocityIfConfigured(jars.velocityJar(), mysqlJdbc, profilingRunId);
        startPaperInstances(jars.paperJar(), velocityForwardingSecret, mysqlJdbc, profilingRunId);

        if (processes.isEmpty()) {
            System.err.println("Nothing to run. Provide --paper-version/--paper-count and/or --velocity-version.");
            return 2;
        }

        int exitCode = awaitAnyProcessExit();
        ServerBootstrapSupport.shutdownServersGracefully(processes, 15_000L);
        return exitCode;
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ServerBootstrapSupport.shutdownServersGracefully(processes, 20_000L);
            if (mysqlContainerName != null) {
                MysqlDockerSupport.cleanupContainer(workspace.baseDir(), mysqlContainerName);
            }
        }));
    }

    private String startMysqlIfEnabled() throws Exception {
        if (!cfg.mysqlEnabled()) return null;

        mysqlContainerName = cfg.mysqlContainerName() != null ? cfg.mysqlContainerName() : "rapunzellib-mysql";
        MysqlDockerSupport.ensureContainerRunning(cfg, workspace.baseDir(), mysqlContainerName);
        MysqlDockerSupport.waitForPortOpen("127.0.0.1", cfg.mysqlPort(), 60_000L);

        boolean ready = MysqlDockerSupport.waitForMysqlAdminPing(cfg, workspace.baseDir(), mysqlContainerName, 60_000L);
        if (!ready) {
            try {
                System.out.println("[mysql] mysqladmin ping did not succeed. Waiting 5s warmup anyway...");
                Thread.sleep(5_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String mysqlJdbc = cfg.mysqlJdbc();
        System.out.println("[mysql] Ready. Set database.jdbc to: " + mysqlJdbc);
        return mysqlJdbc;
    }

    private ResolvedJars resolveJars() throws Exception {
        Path velocityJar = null;
        if (cfg.velocityVersion() != null) {
            FillV3Client.ResolvedBuild build = fillClient.resolveLatestBuild("velocity", cfg.velocityVersion());
            velocityJar = fillClient.downloadJar("velocity", cfg.velocityVersion(), build, workspace.cacheDir());
        }

        Path paperJar = null;
        if (cfg.paperVersion() != null) {
            FillV3Client.ResolvedBuild build = fillClient.resolveLatestBuild("paper", cfg.paperVersion());
            paperJar = fillClient.downloadJar("paper", cfg.paperVersion(), build, workspace.cacheDir());
        }

        return new ResolvedJars(velocityJar, paperJar);
    }

    private void validateResolvedJars(ResolvedJars jars) {
        if (cfg.paperCount() > 0 && jars.paperJar() == null) {
            throw new IllegalStateException("Paper jar resolution failed");
        }
        if (cfg.velocityVersion() != null && jars.velocityJar() == null) {
            throw new IllegalStateException("Velocity jar resolution failed");
        }
    }

    private String startVelocityIfConfigured(Path velocityJar, String mysqlJdbc, String profilingRunId) throws Exception {
        if (velocityJar == null) return null;

        Path instanceDir = ServerBootstrapSupport.createInstanceDir(workspace.instancesDir(), "velocity");
        Files.copy(velocityJar, instanceDir.resolve("velocity.jar"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        installPlugins(instanceDir, cfg.velocityPlugin(), cfg.velocityExtraPlugins());

        Path velocityToml = instanceDir.resolve("velocity.toml");
        List<Path> bootstrapWaitForPaths = buildVelocityBootstrapWaitFor(instanceDir);
        ServerBootstrapSupport.bootstrapVelocityTomlIfNeeded(
            instanceDir,
            cfg.javaBin(),
            cfg.jvmArgs(),
            velocityToml,
            bootstrapWaitForPaths
        );

        Path secretFile = instanceDir.resolve("forwarding.secret");
        ServerBootstrapSupport.touchFile(secretFile);
        String forwardingSecret = ServerBootstrapSupport.readOrGenerateSecret(secretFile);

        ServerBootstrapSupport.applyRegexReplacesForServer(instanceDir, "velocity", 0, cfg, forwardingSecret, mysqlJdbc);

        if (cfg.velocityPlugin() != null) {
            ServerBootstrapSupport.bootstrapServerOnce(
                "velocity",
                instanceDir,
                cfg.javaBin(),
                cfg.jvmArgs(),
                "velocity.jar",
                List.of(),
                bootstrapWaitForPaths,
                30_000L
            );
            ServerBootstrapSupport.applyRegexReplacesForServer(instanceDir, "velocity", 0, cfg, forwardingSecret, mysqlJdbc);
        }

        List<String> command = ServerProcess.javaCommand(
            cfg.javaBin(),
            ServerBootstrapSupport.jvmArgsForMainServer(cfg, "velocity", instanceDir, profilingRunId),
            "velocity.jar",
            List.of()
        );
        processes.add(ServerProcess.start("velocity", instanceDir, command, null));
        return forwardingSecret;
    }

    private void startPaperInstances(Path paperJar, String velocityForwardingSecret, String mysqlJdbc, String profilingRunId)
        throws Exception {
        for (int i = 0; i < cfg.paperCount(); i++) {
            String name = "paper-" + (i + 1);
            Path instanceDir = ServerBootstrapSupport.createInstanceDir(workspace.instancesDir(), name);
            Files.copy(paperJar, instanceDir.resolve("paper.jar"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            installPlugins(instanceDir, cfg.paperPlugin(), cfg.paperExtraPlugins());

            ServerBootstrapSupport.touchFile(instanceDir.resolve("eula.txt"));
            ServerBootstrapSupport.touchFile(instanceDir.resolve("server.properties"));
            Files.createDirectories(instanceDir.resolve("config"));
            ServerBootstrapSupport.touchFile(instanceDir.resolve("config").resolve("paper-global.yml"));

            ServerBootstrapSupport.applyRegexReplacesForServer(
                instanceDir,
                "paper",
                i + 1,
                cfg,
                velocityForwardingSecret,
                mysqlJdbc
            );

            if (cfg.paperPlugin() != null) {
                List<Path> bootstrapWaitForPaths = buildPaperBootstrapWaitFor(instanceDir);
                ServerBootstrapSupport.bootstrapServerOnce(
                    name,
                    instanceDir,
                    cfg.javaBin(),
                    cfg.jvmArgs(),
                    "paper.jar",
                    List.of("--nogui"),
                    bootstrapWaitForPaths,
                    45_000L
                );
                ServerBootstrapSupport.applyRegexReplacesForServer(
                    instanceDir,
                    "paper",
                    i + 1,
                    cfg,
                    velocityForwardingSecret,
                    mysqlJdbc
                );
            }

            List<String> command = ServerProcess.javaCommand(
                cfg.javaBin(),
                ServerBootstrapSupport.jvmArgsForMainServer(cfg, name, instanceDir, profilingRunId),
                "paper.jar",
                List.of("--nogui")
            );
            processes.add(ServerProcess.start(name, instanceDir, command, null));
            sleepBetweenPaperStarts();
        }
    }

    private int awaitAnyProcessExit() throws Exception {
        List<CompletableFuture<Process>> exits = processes.stream().map(ServerProcess::onExit).toList();
        CompletableFuture.anyOf(exits.toArray(new CompletableFuture[0])).join();

        for (int i = 0; i < processes.size(); i++) {
            ServerProcess process = processes.get(i);
            if (!process.isAlive()) {
                return exits.get(i).get().exitValue();
            }
        }
        return 0;
    }

    private void installPlugins(Path instanceDir, Path primaryPlugin, List<Path> extraPlugins) throws java.io.IOException {
        if (primaryPlugin != null) {
            ServerBootstrapSupport.installPlugin(instanceDir, primaryPlugin);
        }
        for (Path plugin : extraPlugins) {
            ServerBootstrapSupport.installPlugin(instanceDir, plugin);
        }
    }

    private List<Path> buildVelocityBootstrapWaitFor(Path instanceDir) {
        if (cfg.velocityPlugin() == null) return List.of();
        String pluginId = ServerBootstrapSupport.tryReadVelocityPluginId(cfg.velocityPlugin());
        if (pluginId == null || pluginId.isBlank()) return List.of();
        Path pluginDir = instanceDir.resolve("plugins").resolve(pluginId);
        return List.of(pluginDir, pluginDir.resolve("config.yml"));
    }

    private List<Path> buildPaperBootstrapWaitFor(Path instanceDir) {
        String pluginName = ServerBootstrapSupport.tryReadPaperPluginName(cfg.paperPlugin());
        if (pluginName == null || pluginName.isBlank()) return List.of();
        Path pluginDir = instanceDir.resolve("plugins").resolve(pluginName);
        return List.of(pluginDir, pluginDir.resolve("config.yml"));
    }

    private void sleepBetweenPaperStarts() {
        try {
            Thread.sleep(2_000L);
        } catch (InterruptedException e) {
            System.err.println("Failed to sleep: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private record ResolvedJars(Path velocityJar, Path paperJar) {
    }
}
