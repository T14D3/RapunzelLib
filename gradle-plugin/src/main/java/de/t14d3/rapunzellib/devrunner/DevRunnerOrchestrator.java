package de.t14d3.rapunzellib.devrunner;

import de.t14d3.rapunzellib.devrunner.bot.BotManager;
import de.t14d3.rapunzellib.devrunner.bot.BotTcpServer;
import de.t14d3.rapunzellib.devrunner.platform.PlatformAdapter;
import de.t14d3.rapunzellib.devrunner.platform.PlatformRegistry;
import de.t14d3.rapunzellib.devrunner.service.ServiceAdapter;
import de.t14d3.rapunzellib.devrunner.service.ServiceRegistry;
import de.t14d3.rapunzellib.serverrunner.FillV3Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DevRunnerOrchestrator {
    private final DevRunnerConfig cfg;
    private final DevRunnerWorkspace workspace;
    private final DevRunnerConsole console;
    private final FillV3Client fillClient;
    private final PlatformRegistry platformRegistry;
    private final ServiceRegistry serviceRegistry;
    private final List<Process> serverProcesses = new ArrayList<>();
    private final Map<String, String> runningContainerNames = new LinkedHashMap<>();
    private final BotManager botManager = new BotManager();
    private BotTcpServer botTcpServer;

    public DevRunnerOrchestrator(DevRunnerConfig cfg) {
        this.cfg = cfg;
        this.workspace = DevRunnerWorkspace.resolve(cfg);
        this.console = new DevRunnerConsole();
        this.fillClient = new FillV3Client();
        this.platformRegistry = PlatformRegistry.getInstance();
        this.serviceRegistry = ServiceRegistry.getInstance();
    }

    public int run() throws Exception {
        workspace.createDirectories();
        registerShutdownHook();

        startServices();
        waitForServicesReady();

        // Start the bot TCP server before server processes so the RpcBotService
        // can connect as soon as the plugin initializes.
        int botTcpPort = startBotTcpServer();

        String mysqlJdbc = cfg.mysqlJdbc(null);
        Map<String, Path> resolvedJars = resolveJars();
        String forwardingSecret = startServers(resolvedJars, mysqlJdbc, botTcpPort);

        if (serverProcesses.isEmpty()) {
            System.err.println("[devrunner] No servers configured.");
            return 2;
        }

        console.startInputRouting();
        runLiveTestsIfEnabled(console);

        int exitCode = awaitAnyProcessExit();
        shutdownAll();
        return exitCode;
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownAll();
            cleanupServices();
        }));
    }

    private void startServices() throws Exception {
        for (var entry : cfg.services().entrySet()) {
            String name = entry.getKey();
            DevRunnerConfig.ServiceSpec spec = entry.getValue();

            ServiceAdapter adapter = serviceRegistry.get(spec.type());
            String containerName = spec.containerName() != null ? spec.containerName() : "devrunner-" + name;
            runningContainerNames.put(name, containerName);

            if (adapter.containerRunning(workspace.baseDir(), containerName)) {
                System.out.println("[devrunner] Service '" + name + "' already running.");
                continue;
            }

            System.out.println("[devrunner] Starting service '" + name + "'...");
            adapter.startContainer(containerName, toServiceSpec(spec, name), workspace.baseDir());
        }
    }

    private void waitForServicesReady() throws Exception {
        for (var entry : cfg.services().entrySet()) {
            String name = entry.getKey();
            DevRunnerConfig.ServiceSpec spec = entry.getValue();
            String containerName = runningContainerNames.get(name);
            if (containerName == null) continue;

            ServiceAdapter adapter = serviceRegistry.get(spec.type());
            System.out.println("[devrunner] Waiting for service '" + name + "'...");
            boolean ready = adapter.waitForReady(containerName, workspace.baseDir(), 60_000L);
            if (!ready) {
                System.out.println("[devrunner] Service '" + name + "' may not be fully ready (proceeding anyway).");
            } else {
                System.out.println("[devrunner] Service '" + name + "' ready.");
            }
        }
    }

    private Map<String, Path> resolveJars() throws Exception {
        Map<String, Path> jars = new LinkedHashMap<>();

        for (var entry : cfg.servers().entrySet()) {
            String name = entry.getKey();
            DevRunnerConfig.ServerSpec spec = entry.getValue();

            if (jars.containsKey(spec.platform())) continue;

            PlatformAdapter adapter = platformRegistry.get(spec.platform());
            String fillProject = adapter.fillProject();
            if (fillProject == null) {
                System.out.println("[devrunner] Platform '" + spec.platform() + "' has no Fill v3 project. JAR must be provided externally.");
                continue;
            }

            String version = spec.version();
            if (version == null || version.isBlank() || version.equalsIgnoreCase("latest")) {
                version = adapter.defaultVersion();
            }

            System.out.println("[devrunner] Resolving " + spec.platform() + " " + version + "...");
            FillV3Client.ResolvedBuild build = fillClient.resolveLatestBuild(fillProject, version);
            Path jar = fillClient.downloadJar(fillProject, version, build, workspace.cacheDir());
            jars.put(spec.platform(), jar);
        }

        return jars;
    }

    private String startServers(Map<String, Path> resolvedJars, String mysqlJdbc, int botTcpPort) throws Exception {
        String forwardingSecret = null;
        PlatformAdapter velocityAdapter = null;
        Path velocityJar = null;

        // Find velocity first (proxy starts before backends)
        for (var entry : cfg.servers().entrySet()) {
            if ("velocity".equals(entry.getValue().platform())) {
                velocityAdapter = platformRegistry.get("velocity");
                velocityJar = resolvedJars.get("velocity");
                break;
            }
        }

        // Start velocity if present
        if (velocityAdapter != null && velocityJar != null) {
            for (var entry : cfg.servers().entrySet()) {
                if (!"velocity".equals(entry.getValue().platform())) continue;
                forwardingSecret = startSingleServer(entry.getKey(), entry.getValue(), velocityAdapter, velocityJar, null, mysqlJdbc, 0, botTcpPort);
                break;
            }
        }

        // Start all non-velocity servers
        int paperIndex = 1;
        for (var entry : cfg.servers().entrySet()) {
            if ("velocity".equals(entry.getValue().platform())) continue;

            PlatformAdapter adapter = platformRegistry.get(entry.getValue().platform());
            Path jar = resolvedJars.get(entry.getValue().platform());
            if (jar == null) {
                System.out.println("[devrunner] No JAR for platform '" + entry.getValue().platform() + "', skipping " + entry.getKey());
                continue;
            }

            startSingleServer(entry.getKey(), entry.getValue(), adapter, jar, forwardingSecret, mysqlJdbc, paperIndex, botTcpPort);
            paperIndex++;
            sleepBetweenStarts();
        }

        return forwardingSecret;
    }

    private String startSingleServer(
        String name,
        DevRunnerConfig.ServerSpec spec,
        PlatformAdapter adapter,
        Path jar,
        String forwardingSecret,
        String mysqlJdbc,
        int serverIndex,
        int botTcpPort
    ) throws Exception {
        System.out.println("[devrunner] Setting up " + name + " (" + spec.platform() + ")...");

        Path instanceDir = workspace.instanceDir(name);
        Path instanceJar = instanceDir.resolve(jar.getFileName());
        Files.copy(jar, instanceJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Install project plugin JAR
        if (spec.pluginJar() != null && Files.exists(spec.pluginJar())) {
            adapter.installPlugin(instanceDir, spec.pluginJar());
        }

        // Install extra plugins
        for (Path extraPlugin : spec.extraPlugins()) {
            if (Files.exists(extraPlugin)) {
                adapter.installPlugin(instanceDir, extraPlugin);
            }
        }

        // Bootstrap
        Map<String, String> variables = DevRunnerBootstrap.buildServerVariables(
            spec, name, serverIndex, cfg, forwardingSecret, mysqlJdbc
        );

        PlatformAdapter.BootstrapContext bootstrapCtx = new PlatformAdapter.BootstrapContext(
            instanceDir, instanceJar, cfg.javaBin(), cfg.jvmArgs(), variables
        );
        adapter.bootstrapOnce(bootstrapCtx);

        // Write file overrides (plugin configs, etc.)
        Map<String, String> serverOverrides = cfg.fileOverrides().get(name);
        if (serverOverrides != null && !serverOverrides.isEmpty()) {
            for (var entry : serverOverrides.entrySet()) {
                Path targetFile = instanceDir.resolve(entry.getKey()).normalize();
                if (!targetFile.startsWith(instanceDir)) {
                    System.err.println("[devrunner] File override path escapes instance dir: " + entry.getKey());
                    continue;
                }
                Files.createDirectories(targetFile.getParent());
                Files.writeString(targetFile, DevRunnerBootstrap.substitute(entry.getValue(), variables));
                System.out.println("[devrunner] Wrote override file: " + entry.getKey());
            }
        }

        // Generate forwarding secret for velocity
        if ("velocity".equals(spec.platform())) {
            Path secretFile = instanceDir.resolve("forwarding.secret");
            DevRunnerBootstrap.touchFile(secretFile);
            forwardingSecret = DevRunnerBootstrap.readOrGenerateSecret(secretFile);
            variables.put("velocity_secret", forwardingSecret);
        }

        // Apply patches
        DevRunnerBootstrap.applyRegexReplaces(instanceDir, cfg.regexReplaces(), variables);

        // Post-bootstrap
        PlatformAdapter.PostBootstrapContext postCtx = new PlatformAdapter.PostBootstrapContext(
            instanceDir, name, serverIndex, variables
        );
        adapter.postBootstrap(postCtx);

        // Build command
        List<String> command = new ArrayList<>();
        command.add(cfg.javaBin());
        if (!cfg.jvmArgs().isEmpty()) command.addAll(cfg.jvmArgs());

        // Pass the bot TCP server port to the server plugin
        if (botTcpPort > 0) {
            command.add("-Drapunzellib.bot.rpc.port=" + botTcpPort);
        }

        // JFR
        if (cfg.jfrEnabled()) {
            String safeName = name.replaceAll("[^a-zA-Z0-9_-]", "_");
            String runId = Long.toString(System.currentTimeMillis());
            String settings = cfg.jfrSettings() != null ? cfg.jfrSettings() : "profile";
            Files.createDirectories(instanceDir.resolve("jfr"));
            command.add("-XX:StartFlightRecording=name=" + safeName + ",settings=" + settings
                + ",filename=jfr/" + safeName + "-" + runId + ".jfr,dumponexit=true");
            command.add("-XX:FlightRecorderOptions=stackdepth=128");
        }

        command.add("-jar");
        command.add(instanceJar.getFileName().toString());
        command.addAll(adapter.programArgs());

        // Start
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(instanceDir.toAbsolutePath().normalize().toFile());
        pb.redirectErrorStream(false);
        Process process = pb.start();
        serverProcesses.add(process);
        console.registerSource(name, process);

        System.out.println("[devrunner] Started " + name + " (PID " + process.pid() + ")");
        return forwardingSecret;
    }

    private int awaitAnyProcessExit() throws Exception {
        List<CompletableFuture<Process>> futures = serverProcesses.stream()
            .map(Process::onExit)
            .toList();
        CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();

        for (int i = 0; i < serverProcesses.size(); i++) {
            Process process = serverProcesses.get(i);
            if (!process.isAlive()) {
                return process.exitValue();
            }
        }
        return 0;
    }

    private void shutdownAll() {
        // Disconnect all bots
        botManager.disconnectAll();

        // Close the bot TCP server
        if (botTcpServer != null) {
            try { botTcpServer.close(); } catch (Exception ignored) {}
        }

        console.shutdown();

        for (Process process : serverProcesses) {
            if (!process.isAlive()) continue;
            process.destroy();
        }

        // Wait a bit for graceful shutdown
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Force kill remaining
        for (Process process : serverProcesses) {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }

        // Wait for all to exit
        for (Process process : serverProcesses) {
            try {
                process.onExit().get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
    }

    private void cleanupServices() {
        for (var entry : runningContainerNames.entrySet()) {
            String name = entry.getKey();
            String containerName = entry.getValue();
            try {
                DevRunnerConfig.ServiceSpec spec = cfg.services().get(name);
                if (spec != null) {
                    ServiceAdapter adapter = serviceRegistry.get(spec.type());
                    adapter.cleanup(containerName, workspace.baseDir());
                    System.out.println("[devrunner] Cleaned up service container: " + containerName);
                }
            } catch (Exception e) {
                System.err.println("[devrunner] Failed to cleanup service '" + name + "': " + e.getMessage());
            }
        }
    }

    private void runLiveTestsIfEnabled(DevRunnerConsole console) {
        if (!cfg.liveTests().enabled() || !cfg.liveTests().autoRun()) return;

        System.out.println("[devrunner] Running live tests...");

        // Wait for servers to finish loading before sending commands
        for (var entry : cfg.servers().entrySet()) {
            String name = entry.getKey();
            DevRunnerConfig.ServerSpec spec = entry.getValue();

            // Give the server time to fully initialize before sending commands
            try {
                // Look for "Done" in the server output to know it's ready
                // Simple heuristic: poll for process to print "Done"
                long deadline = System.currentTimeMillis() + cfg.liveTests().timeoutMs();
                while (System.currentTimeMillis() < deadline) {
                    // Check if the server process is still alive
                    boolean alive = false;
                    for (Process p : serverProcesses) {
                        if (p.isAlive()) { alive = true; break; }
                    }
                    if (!alive) break;
                    // Just wait - the console will have captured output by then
                    Thread.sleep(500);
                    // Simple heuristic: if we've waited 15 seconds, assume server is ready
                    if (System.currentTimeMillis() > deadline - cfg.liveTests().timeoutMs() + 15_000L) break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Determine the livetest command based on platform
            String testCommand = switch (spec.platform()) {
                case "paper", "fabric", "neoforge", "sponge" -> "/livetest runall";
                case "velocity" -> "/livetest runall";
                default -> null;
            };

            if (testCommand != null) {
                console.routeInput("-" + name + " " + testCommand);
            }
        }

        // Track servers that have completed testing via line listener
        java.util.Set<String> serversCompleted = java.util.concurrent.ConcurrentHashMap.newKeySet();
        DevRunnerConsole.LineListener completionListener = (sourceName, line, isError) -> {
            if (!isError && line != null && line.contains("All tests completed")) {
                serversCompleted.add(sourceName);
                System.out.println("[devrunner] Server '" + sourceName + "' completed all tests.");
            }
        };
        console.addLineListener(completionListener);

        try {
            // Wait for all non-velocity servers to complete (allow generous time for
            // server startup + bot tests). If the deadline expires, proceed anyway.
            long testDeadline = System.currentTimeMillis() + cfg.liveTests().runTimeoutMs();
            int targetServers = 0;
            for (var entry : cfg.servers().entrySet()) {
                if (!"velocity".equals(entry.getValue().platform())) {
                    targetServers++;
                }
            }

            while (System.currentTimeMillis() < testDeadline && serversCompleted.size() < targetServers) {
                // Check if any process has died (server may have crashed)
                boolean anyAlive = serverProcesses.stream().anyMatch(Process::isAlive);
                if (!anyAlive) break;
                Thread.sleep(200);
            }

            // Send shutdown to all servers
            System.out.println("[devrunner] Tests complete, shutting down servers...");
            for (var entry : cfg.servers().entrySet()) {
                String name = entry.getKey();
                var spec = entry.getValue();
                if (!"velocity".equals(spec.platform())) {
                    PlatformAdapter adapter = platformRegistry.get(spec.platform());
                    String shutdownCmd = adapter != null ? adapter.shutdownCommand() : "stop";
                    console.routeInput("-" + name + " " + shutdownCmd);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            console.removeLineListener(completionListener);
        }
    }

    // ── Bot management ─────────────────────────────────────────────────────

    private int startBotTcpServer() {
        try {
            botTcpServer = new BotTcpServer(botManager, 0, serverName -> {
                DevRunnerConfig.ServerSpec spec = cfg.servers().get(serverName);
                if (spec == null) return null;
                return "127.0.0.1:" + spec.port();
            });
            int port = botTcpServer.start();
            System.out.println("[devrunner] Bot TCP server started on port " + port);
            return port;
        } catch (Exception e) {
            System.err.println("[devrunner] Failed to start bot TCP server: " + e.getMessage());
            return 0;
        }
    }

    private ServiceAdapter.ServiceSpec toServiceSpec(DevRunnerConfig.ServiceSpec spec, String name) {
        return new ServiceAdapter.ServiceSpec(
            spec.type(), spec.image(), spec.ports(), spec.env(), spec.containerName()
        );
    }

    private void sleepBetweenStarts() {
        try {
            Thread.sleep(2_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
