package de.t14d3.rapunzellib.devrunner;

import de.t14d3.rapunzellib.devrunner.bot.BotManager;
import de.t14d3.rapunzellib.devrunner.bot.BotTcpServer;
import de.t14d3.rapunzellib.devrunner.platform.PlatformAdapter;
import de.t14d3.rapunzellib.devrunner.platform.PlatformRegistry;
import de.t14d3.rapunzellib.devrunner.service.ServiceAdapter;
import de.t14d3.rapunzellib.devrunner.service.ServiceRegistry;
import de.t14d3.rapunzellib.serverrunner.FillV3Client;

import java.io.IOException;
import java.net.InetAddress;
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

        // Project policy: players (bots) must ALWAYS connect through the proxy.
        // Fail at config time when no velocity server is configured and the
        // config did not explicitly opt out (allowDirectConnections).
        if (!verifyProxyRequirement()) {
            return 2;
        }

        startServices();
        waitForServicesReady();

        // Start the bot TCP server before server processes so the RpcBotService
        // can connect as soon as the plugin initializes.
        int botTcpPort = startBotTcpServer();

        // Fail loudly BEFORE any server starts when the velocity forced-host
        // DNS entries the bot harness relies on are missing (see /etc/hosts).
        if (!verifyForcedHosts()) {
            return 2;
        }

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

        // Track servers that have completed testing via line listener.
        // NOTE: this listener MUST be registered before the runall commands are
        // dispatched below, otherwise a fast-completing test suite can finish
        // before the listener attaches and the orchestrator would wait for the
        // full run timeout.
        java.util.Set<String> serversCompleted = java.util.concurrent.ConcurrentHashMap.newKeySet();
        DevRunnerConsole.LineListener completionListener = (sourceName, line, isError) -> {
            if (!isError && line != null && line.contains("All tests completed")) {
                if (serversCompleted.add(sourceName)) {
                    System.out.println("[devrunner] Server '" + sourceName + "' completed all tests.");
                }
            }
        };
        console.addLineListener(completionListener);

        int targetServers = 0;
        for (var entry : cfg.servers().entrySet()) {
            if (!"velocity".equals(entry.getValue().platform())) {
                targetServers++;
            }
        }

        try {
            // Run each server's test suite SEQUENTIALLY. Every live-test bot
            // connects through the shared velocity proxy, and velocity enforces
            // unique player identities - two instances running the same bot
            // names (BotAlice/BotBob) concurrently would kick each other with
            // "You are already connected to this proxy!". Serializing the
            // suites keeps the same-named bots from ever being connected at
            // the same time. Servers stay up between suites, so the lobby's
            // cross-server scenarios can still connect bots to survival.
            long suiteBudgetMs = Math.max(1, cfg.liveTests().runTimeoutMs() / Math.max(1, targetServers));
            for (var entry : cfg.servers().entrySet()) {
                String name = entry.getKey();
                DevRunnerConfig.ServerSpec spec = entry.getValue();
                if ("velocity".equals(spec.platform())) continue;

                // Give the server time to fully initialize before sending commands.
                long readyDeadline = System.currentTimeMillis() + cfg.liveTests().timeoutMs();
                while (System.currentTimeMillis() < readyDeadline) {
                    boolean alive = serverProcesses.stream().anyMatch(Process::isAlive);
                    if (!alive) break;
                    Thread.sleep(500);
                    // Simple heuristic: if we've waited 15 seconds, assume server is ready.
                    if (System.currentTimeMillis() > readyDeadline - cfg.liveTests().timeoutMs() + 15_000L) break;
                }

                String testCommand = switch (spec.platform()) {
                    case "paper", "fabric", "neoforge", "sponge" -> "/livetest runall";
                    default -> null;
                };
                if (testCommand == null) continue;

                console.routeInput("-" + name + " " + testCommand);

                // Wait for THIS server's suite to complete before starting the
                // next one (see the serialization rationale above).
                long suiteDeadline = System.currentTimeMillis() + suiteBudgetMs;
                while (System.currentTimeMillis() < suiteDeadline && !serversCompleted.contains(name)) {
                    boolean anyAlive = serverProcesses.stream().anyMatch(Process::isAlive);
                    if (!anyAlive) break;
                    Thread.sleep(200);
                }
                if (!serversCompleted.contains(name)) {
                    System.out.println("[devrunner] Server '" + name + "' did not report test completion within "
                            + suiteBudgetMs + "ms; proceeding to the next server.");
                }
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

    /**
     * Verifies that the velocity forced-host DNS names used to route bots to
     * backends ({@code <server>.example.com}) resolve before any server starts.
     *
     * <p>The bot harness connects every bot through the velocity proxy using
     * these hostnames (see {@link #startBotTcpServer()}); when the corresponding
     * /etc/hosts entries (or DNS records) are missing, every backend bot fails
     * with "Unknown host" and the whole devrun dies slowly and confusingly.
     * This check fails fast with an actionable message instead.</p>
     *
     * @return true when the forced hosts resolve (or no velocity proxy is used)
     */
    private boolean verifyForcedHosts() {
        boolean hasVelocity = cfg.servers().values().stream()
                .anyMatch(spec -> "velocity".equals(spec.platform()));
        if (!hasVelocity) {
            return true;
        }

        List<String> missing = new ArrayList<>();
        for (var entry : cfg.servers().entrySet()) {
            String name = entry.getKey();
            if ("velocity".equals(entry.getValue().platform())) continue;
            String host = forcedHostFor(name);
            try {
                InetAddress.getAllByName(host);
            } catch (Exception e) {
                missing.add(host);
            }
        }

        if (missing.isEmpty()) {
            return true;
        }

        System.err.println("[devrunner] FATAL: velocity topology requires forced-host DNS entries that do NOT resolve:");
        for (String host : missing) {
            System.err.println("  - " + host);
        }
        System.err.println("Add the following lines to /etc/hosts (or configure DNS), then re-run:");
        for (String host : missing) {
            System.err.println("  127.0.0.1 " + host);
        }
        return false;
    }

    /**
     * Verifies the velocity-proxy requirement.
     *
     * <p>Per project policy every player (dev bot) must connect through the
     * velocity proxy, never directly to a backend. A topology without a
     * velocity server is only accepted when the config explicitly opts out
     * ({@code allowDirectConnections=true}, e.g. single-server devruns).</p>
     *
     * @return true when the requirement is satisfied
     */
    private boolean verifyProxyRequirement() {
        boolean hasVelocity = cfg.servers().values().stream()
                .anyMatch(spec -> "velocity".equals(spec.platform()));
        if (hasVelocity || cfg.allowDirectConnections()) {
            return true;
        }

        System.err.println("[devrunner] FATAL: no velocity server configured.");
        System.err.println("[devrunner] Every player/bot must connect through the velocity proxy;");
        System.err.println("[devrunner] direct backend connections are not allowed by default.");
        System.err.println("[devrunner] Fix one of:");
        System.err.println("[devrunner]   - add a server with platform \"velocity\" to the devRunner servers DSL, or");
        System.err.println("[devrunner]   - explicitly opt out: devRunner { allowDirectConnections.set(true) }");
        return false;
    }

    /** Returns the velocity forced-host name used for the given backend server. */
    private static String forcedHostFor(String serverName) {
        return serverName + ".example.com";
    }

    // ── Bot management ─────────────────────────────────────────────────────

    private int startBotTcpServer() {
        try {
            // Resolve the velocity proxy spec once: bots connect through the proxy
            // (never directly to a backend). Each backend is addressed by its
            // velocity forced-host hostname so the proxy routes the bot to the
            // intended server; /etc/hosts maps those hostnames to 127.0.0.1.
            Map<String, DevRunnerConfig.ServerSpec> servers = cfg.servers();
            DevRunnerConfig.ServerSpec velocitySpec = servers.values().stream()
                    .filter(s -> "velocity".equals(s.platform()))
                    .findFirst()
                    .orElse(null);
            botTcpServer = new BotTcpServer(botManager, 0, serverName ->
                    resolveBotAddress(servers, serverName, velocitySpec, cfg.allowDirectConnections()));
            int port = botTcpServer.start();
            System.out.println("[devrunner] Bot TCP server started on port " + port);
            return port;
        } catch (Exception e) {
            System.err.println("[devrunner] Failed to start bot TCP server: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Resolves the connection address the bot harness should use for the given
     * backend server.
     *
     * <p>Bots always connect through the velocity proxy (policy): the backend
     * is addressed by its velocity forced-host hostname on the proxy's port.
     * When no proxy is configured the topology must have opted out via
     * {@code allowDirectConnections}; the backend is then addressed directly
     * on its own port. Returns {@code null} for unknown servers and for the
     * proxy itself.</p>
     *
     * @param servers              the configured server specs
     * @param serverName           the backend to connect to
     * @param velocitySpec         the velocity spec, or null when none is configured
     * @param allowDirectConnections opt-out for proxy-less topologies
     * @return the bot connection address, or null when the server is not connectable
     * @throws IllegalStateException when no proxy is configured and direct connections are not allowed
     */
    static String resolveBotAddress(
            Map<String, DevRunnerConfig.ServerSpec> servers,
            String serverName,
            DevRunnerConfig.ServerSpec velocitySpec,
            boolean allowDirectConnections
    ) {
        DevRunnerConfig.ServerSpec spec = servers.get(serverName);
        if (spec == null) {
            return null;
        }
        // Bots never target the proxy itself as a backend.
        if ("velocity".equals(spec.platform())) {
            return null;
        }
        if (velocitySpec == null) {
            if (!allowDirectConnections) {
                throw new IllegalStateException(
                        "no velocity server configured; bots cannot connect directly to backend '"
                                + serverName + "' (set allowDirectConnections=true to opt out)");
            }
            return "127.0.0.1:" + spec.port();
        }
        return forcedHostFor(serverName) + ":" + velocitySpec.port();
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
