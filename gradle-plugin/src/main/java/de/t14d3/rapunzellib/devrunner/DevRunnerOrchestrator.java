package de.t14d3.rapunzellib.devrunner;

import de.t14d3.rapunzellib.devrunner.bot.BotClient;
import de.t14d3.rapunzellib.devrunner.bot.BotConstants;
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
import java.util.concurrent.ConcurrentHashMap;

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
    // Tracks which server requested each bot (botName -> serverName)
    private final Map<String, String> botToServer = new ConcurrentHashMap<>();

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
        startBotManagement();
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
            System.err.println("[devrunner] Falling back to stdout-based bot protocol");
            return 0;
        }
    }

    private void startBotManagement() {
        // Register a line listener that processes [BOT_*] protocol messages from server output
        console.addLineListener((sourceName, line, isError) -> {
            if (isError) return; // Only process stdout

            // Debug: log every line for diagnosis
            if (line.contains("[BOT_")) {
                System.out.println("[devrunner] Bot line from " + sourceName + ": " + line);
            }

            BotConstants.BotCommand cmd = BotConstants.parseBotMessage(line);
            if (cmd == null) return;

            try {
                switch (cmd.type()) {
                    case "CONNECT" -> handleBotConnect(sourceName, cmd);
                    case "DISCONNECT" -> handleBotDisconnect(cmd);
                    case "EXEC" -> handleBotExec(cmd);
                    case "DIG" -> handleBotDig(cmd);
                    case "USE" -> handleBotUse(cmd);
                    case "QUERY_POSITION" -> handleBotQueryPosition(cmd);
                    case "QUERY_HEALTH" -> handleBotQueryHealth(cmd);
                    case "QUERY_HELD_ITEM" -> handleBotQueryHeldItem(cmd);
                    case "QUERY_GAMEMODE" -> handleBotQueryGameMode(cmd);
                    case "QUERY_OPEN_CONTAINER" -> handleBotQueryOpenContainer(cmd);
                    case "QUERY_ENTITIES" -> handleBotQueryEntities(cmd);
                    case "MOVE_TO" -> handleBotMoveTo(cmd);
                    case "ATTACK" -> handleBotAttack(cmd);
                    case "INTERACT" -> handleBotInteract(cmd);
                    case "SWING" -> handleBotSwing(cmd);
                    case "SET_SLOT" -> handleBotSetSlot(cmd);
                }
            } catch (Exception e) {
                System.err.println("[devrunner] Bot error: " + e.getMessage());
            }
        });

        System.out.println("[devrunner] Bot management started");
    }

    private void handleBotConnect(String sourceServer, BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String serverArg = cmd.args().length > 0 ? cmd.args()[0] : "";
        // Remove any stale entry first - a previous test may have disconnected
        // but the [BOT_DISCONNECT] line hasn't been processed yet.
        String previous = botToServer.remove(botName);
        if (previous != null) {
            System.out.println("[devrunner] Bot '" + botName + "' stale entry removed from " + previous);
        }
        if (botToServer.putIfAbsent(botName, sourceServer) != null) {
            // Shouldn't happen after the remove above, but guard thread safety
            System.out.println("[devrunner] Bot '" + botName + "' is already connected or requested (from " + botToServer.get(botName) + ")");
            return;
        }

        DevRunnerConfig cfg = this.cfg;
        String address = findServerAddress(serverArg, cfg);
        if (address == null) {
            System.err.println("[devrunner] Unknown server '" + serverArg + "' for bot connect");
            console.sendToServer(sourceServer, "/botcallback ERROR " + botName + " Unknown server: " + serverArg);
            return;
        }

        String[] hostPort = address.split(":");
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        final String finalSourceServer = sourceServer;

        // Connect the bot in a separate thread to avoid blocking the line listener
        new Thread(() -> {
            try {
                // Track which server requested this bot
                botToServer.put(botName, finalSourceServer);

                // Connect the bot (blocking call)
                System.out.println("[devrunner] Connecting bot '" + botName + "' to " + host + ":" + port + "...");
                botManager.connectBot(botName, host, port);

                System.out.println("[devrunner] Bot '" + botName + "' connected to " + host + ":" + port);

                // Register a chat callback so received messages are forwarded to the server
                BotClient client = botManager.getBot(botName);
                if (client != null) {
                    client.addChatCallback(message -> {
                        String server = botToServer.get(botName);
                        if (server != null) {
                            String safeMessage = message.replace("\n", "\\n").replace("\r", "\\r");
                            console.sendToServer(server, "/botcallback CHAT " + botName + " " + safeMessage);
                        }
                    });
                }

                // Signal the server that the bot is ready
                console.sendToServer(finalSourceServer, "/botcallback READY " + botName);
            } catch (Exception e) {
                System.err.println("[devrunner] Bot '" + botName + "' connection error: " + e.getMessage());
                e.printStackTrace();
                console.sendToServer(finalSourceServer, "/botcallback ERROR " + botName + " " + e.getMessage());
            }
        }).start();
    }

    private void handleBotDisconnect(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String server = botToServer.remove(botName);
        botManager.disconnectBot(botName);
        if (server != null) {
            console.sendToServer(server, "/botcallback DISCONNECT " + botName);
        }
    }

    private void handleBotExec(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String command = cmd.args().length > 0 ? cmd.args()[0] : "";
        if (command.isEmpty()) {
            System.err.println("[devrunner] Empty command for bot '" + botName + "'");
            return;
        }
        botManager.execute(botName, command);
    }

    private void handleBotDig(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        if (cmd.args().length < 4) {
            System.err.println("[devrunner] Invalid DIG command for bot '" + botName + "'");
            return;
        }
        try {
            int x = Integer.parseInt(cmd.args()[0]);
            int y = Integer.parseInt(cmd.args()[1]);
            int z = Integer.parseInt(cmd.args()[2]);
            int direction = Integer.parseInt(cmd.args()[3]);
            botManager.digBlock(botName, x, y, z, direction);
        } catch (NumberFormatException e) {
            System.err.println("[devrunner] Invalid DIG coordinates for bot '" + botName + "': " + e.getMessage());
        }
    }

    private void handleBotUse(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        if (cmd.args().length < 5) {
            System.err.println("[devrunner] Invalid USE command for bot '" + botName + "'");
            return;
        }
        try {
            int x = Integer.parseInt(cmd.args()[0]);
            int y = Integer.parseInt(cmd.args()[1]);
            int z = Integer.parseInt(cmd.args()[2]);
            int hand = Integer.parseInt(cmd.args()[3]);
            int direction = Integer.parseInt(cmd.args()[4]);
            botManager.useItemOn(botName, x, y, z, hand, direction);
        } catch (NumberFormatException e) {
            System.err.println("[devrunner] Invalid USE arguments for bot '" + botName + "': " + e.getMessage());
        }
    }

    private void handleBotQueryPosition(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String server = botToServer.get(botName);
        if (server == null) return;
        double[] pos = botManager.queryPosition(botName);
        if (pos != null) {
            console.sendToServer(server, String.format("/botcallback POSITION %s %.2f %.2f %.2f %.2f %.2f",
                botName, pos[0], pos[1], pos[2], pos[3], pos[4]));
        }
    }

    private void handleBotQueryHealth(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String server = botToServer.get(botName);
        if (server == null) return;
        float[] health = botManager.queryHealth(botName);
        if (health != null) {
            console.sendToServer(server, String.format("/botcallback HEALTH %s %.1f %d %.1f",
                botName, health[0], (int) health[1], health[2]));
        }
    }

    private void handleBotQueryHeldItem(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String server = botToServer.get(botName);
        if (server == null) return;
        int[] item = botManager.queryHeldItem(botName);
        if (item != null) {
            console.sendToServer(server, String.format("/botcallback HELD_ITEM %s %d unknown",
                botName, item[0]));
        }
    }

    private void handleBotQueryGameMode(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String server = botToServer.get(botName);
        if (server == null) return;
        String gm = botManager.queryGameMode(botName);
        if (gm != null) {
            console.sendToServer(server, "/botcallback GAMEMODE " + botName + " " + gm);
        }
    }

    private void handleBotQueryOpenContainer(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String server = botToServer.get(botName);
        if (server == null) return;
        int containerId = botManager.queryOpenContainerId(botName);
        console.sendToServer(server, "/botcallback OPEN_CONTAINER " + botName + " " + containerId);
    }

    private void handleBotQueryEntities(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        String server = botToServer.get(botName);
        if (server == null) return;
        String typeName = cmd.args().length > 0 ? cmd.args()[0] : "";
        int[] entityIds = botManager.findEntities(botName, typeName);
        StringBuilder sb = new StringBuilder("/botcallback ENTITIES " + botName);
        for (int id : entityIds) {
            sb.append(" ").append(id);
        }
        console.sendToServer(server, sb.toString());
    }

    private void handleBotMoveTo(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        if (cmd.args().length < 3) {
            System.err.println("[devrunner] Invalid MOVE_TO command for bot '" + botName + "'");
            return;
        }
        try {
            int x = Integer.parseInt(cmd.args()[0]);
            int y = Integer.parseInt(cmd.args()[1]);
            int z = Integer.parseInt(cmd.args()[2]);
            botManager.moveTo(botName, x, y, z);
        } catch (NumberFormatException e) {
            System.err.println("[devrunner] Invalid MOVE_TO coordinates for bot '" + botName + "': " + e.getMessage());
        }
    }

    private void handleBotAttack(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        if (cmd.args().length < 1) {
            System.err.println("[devrunner] Invalid ATTACK command for bot '" + botName + "'");
            return;
        }
        try {
            int entityId = Integer.parseInt(cmd.args()[0]);
            botManager.attackEntity(botName, entityId);
        } catch (NumberFormatException e) {
            System.err.println("[devrunner] Invalid ATTACK entityId for bot '" + botName + "': " + e.getMessage());
        }
    }

    private void handleBotInteract(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        if (cmd.args().length < 2) {
            System.err.println("[devrunner] Invalid INTERACT command for bot '" + botName + "'");
            return;
        }
        try {
            int entityId = Integer.parseInt(cmd.args()[0]);
            int hand = Integer.parseInt(cmd.args()[1]);
            botManager.interactEntity(botName, entityId, hand);
        } catch (NumberFormatException e) {
            System.err.println("[devrunner] Invalid INTERACT arguments for bot '" + botName + "': " + e.getMessage());
        }
    }

    private void handleBotSwing(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        if (cmd.args().length < 1) {
            System.err.println("[devrunner] Invalid SWING command for bot '" + botName + "'");
            return;
        }
        try {
            int hand = Integer.parseInt(cmd.args()[0]);
            botManager.swingHand(botName, hand);
        } catch (NumberFormatException e) {
            System.err.println("[devrunner] Invalid SWING hand for bot '" + botName + "': " + e.getMessage());
        }
    }

    private void handleBotSetSlot(BotConstants.BotCommand cmd) {
        String botName = cmd.botName();
        if (cmd.args().length < 1) {
            System.err.println("[devrunner] Invalid SET_SLOT command for bot '" + botName + "'");
            return;
        }
        try {
            int slot = Integer.parseInt(cmd.args()[0]);
            botManager.setHeldItemSlot(botName, slot);
        } catch (NumberFormatException e) {
            System.err.println("[devrunner] Invalid SET_SLOT slot for bot '" + botName + "': " + e.getMessage());
        }
    }

    /**
     * Looks up the address (host:port) for a given server name from the config.
     *
     * @param serverName the logical server name
     * @param config     the DevRunner configuration
     * @return "host:port" string, or null if the server is not found
     */
    static String findServerAddress(String serverName, DevRunnerConfig config) {
        DevRunnerConfig.ServerSpec spec = config.servers().get(serverName);
        if (spec == null) return null;
        return "127.0.0.1:" + spec.port();
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
