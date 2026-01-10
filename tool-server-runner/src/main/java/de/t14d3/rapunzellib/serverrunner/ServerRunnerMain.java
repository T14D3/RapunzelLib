package de.t14d3.rapunzellib.serverrunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.concurrent.CompletableFuture;

public final class ServerRunnerMain {
    private static final String DEFAULT_JAVA = defaultJava();

    private static String defaultJava() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isBlank()) return "java";

        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String exe = isWindows ? "java.exe" : "java";
        Path candidate = Path.of(javaHome, "bin", exe);
        if (Files.isRegularFile(candidate)) return candidate.toString();
        return "java";
    }

    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } catch (Throwable t) {
            System.err.println("[error] Unhandled exception: " + t.getMessage());
            t.printStackTrace(System.err);
            code = 1;
        }
        System.exit(code);
    }

    public static int run(String[] args) throws Exception {
        Config cfg;
        try {
            cfg = Config.parse(args);
        } catch (UsageException e) {
            System.out.println(e.getMessage());
            return e.code;
        }

        Path baseDir = (cfg.baseDir != null ? cfg.baseDir : Path.of("run", "server-runner"))
                .toAbsolutePath()
                .normalize();

        Path cacheDir;
        if (cfg.cacheDir != null) {
            cacheDir = cfg.cacheDir.isAbsolute() ? cfg.cacheDir : baseDir.resolve(cfg.cacheDir).normalize();
        } else {
            cacheDir = baseDir.resolve("cache");
        }
        cacheDir = cacheDir.toAbsolutePath().normalize();

        Path instancesDir;
        if (cfg.instancesDir != null) {
            instancesDir = cfg.instancesDir.isAbsolute() ? cfg.instancesDir : baseDir.resolve(cfg.instancesDir).normalize();
        } else {
            instancesDir = baseDir.resolve("instances");
        }
        instancesDir = instancesDir.toAbsolutePath().normalize();

        Files.createDirectories(baseDir);
        Files.createDirectories(cacheDir);
        Files.createDirectories(instancesDir);

        FillV3Client fill = new FillV3Client();

        List<ServerProcess> processes = new ArrayList<>();
        final String[] mysqlContainerNameForShutdown = new String[]{null};
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownServersGracefully(processes, 20_000L);
            if (mysqlContainerNameForShutdown[0] != null) {
                cleanupMysqlContainer(baseDir, mysqlContainerNameForShutdown[0]);
            }
        }));

        String mysqlJdbc = null;
        if (cfg.mysqlEnabled) {
            String containerName = cfg.mysqlContainerName != null
                    ? cfg.mysqlContainerName
                    : "rapunzellib-mysql";
            mysqlContainerNameForShutdown[0] = containerName;
            ensureMysqlContainerRunning(cfg, baseDir, containerName);
            waitForPortOpen("127.0.0.1", cfg.mysqlPort, 60_000L);

            // Best-effort: port-open doesn't always mean "ready to accept auth/queries" yet.
            // Prefer waiting for mysqladmin ping inside the container; fall back to a short warmup sleep.
            boolean ready = waitForMysqlAdminPing(cfg, baseDir, containerName, 60_000L);
            if (!ready) {
                try {
                    System.out.println("[mysql] mysqladmin ping did not succeed. Waiting 5s warmup anyway...");
                    Thread.sleep(5_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            mysqlJdbc = cfg.mysqlJdbc();
            System.out.println("[mysql] Ready. Set database.jdbc to: " + mysqlJdbc);
        }

        Path velocityJar = null;
        if (cfg.velocityVersion != null) {
            FillV3Client.ResolvedBuild build = fill.resolveLatestBuild("velocity", cfg.velocityVersion);
            velocityJar = fill.downloadJar("velocity", cfg.velocityVersion, build, cacheDir);
        }

        Path paperJar = null;
        if (cfg.paperVersion != null) {
            FillV3Client.ResolvedBuild build = fill.resolveLatestBuild("paper", cfg.paperVersion);
            paperJar = fill.downloadJar("paper", cfg.paperVersion, build, cacheDir);
        }

        if (cfg.paperCount > 0 && paperJar == null) throw new IllegalStateException("Paper jar resolution failed");
        if (velocityJar == null) {
            throw new IllegalStateException("Velocity jar resolution failed");
        }

        final String profilingRunId = Long.toString(System.currentTimeMillis());

        String velocityForwardingSecret;

        Path dir = createInstanceDir(instancesDir, "velocity");
        Files.copy(velocityJar, dir.resolve("velocity.jar"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        if (cfg.velocityPlugin != null) installPlugin(dir, cfg.velocityPlugin);
        for (Path plugin : cfg.velocityExtraPlugins) {
            installPlugin(dir, plugin);
        }

        Path velocityToml = dir.resolve("velocity.toml");
        List<Path> velocityBootstrapWaitFor = new ArrayList<>();
        if (cfg.velocityPlugin != null) {
            String velocityPluginId = tryReadVelocityPluginId(cfg.velocityPlugin);
            if (velocityPluginId != null && !velocityPluginId.isBlank()) {
                velocityBootstrapWaitFor.add(dir.resolve("plugins").resolve(velocityPluginId));
                velocityBootstrapWaitFor.add(dir.resolve("plugins").resolve(velocityPluginId).resolve("config.yml"));
            }
        }
        bootstrapVelocityTomlIfNeeded(dir, cfg.javaBin, cfg.jvmArgs, velocityToml, velocityBootstrapWaitFor);

        Path secretFile = dir.resolve("forwarding.secret");
        touchFile(secretFile);
        velocityForwardingSecret = readOrGenerateSecret(secretFile);

        applyRegexReplacesForServer(
                dir,
                "velocity",
                0,
                cfg,
                velocityForwardingSecret,
                mysqlJdbc
        );

        // Best-effort: boot once so installed plugins can create their configs, then re-apply patches.
        if (cfg.velocityPlugin != null) {
            bootstrapServerOnce(
                    "velocity",
                    dir,
                    cfg.javaBin,
                    cfg.jvmArgs,
                    "velocity.jar",
                    List.of(),
                    velocityBootstrapWaitFor,
                    30_000L
            );
            applyRegexReplacesForServer(
                    dir,
                    "velocity",
                    0,
                    cfg,
                    velocityForwardingSecret,
                    mysqlJdbc
            );
        }

        List<String> velocityCmd = ServerProcess.javaCommand(
            cfg.javaBin,
            jvmArgsForMainServer(cfg, "velocity", dir, profilingRunId),
            "velocity.jar",
            List.of()
        );
        processes.add(ServerProcess.start("velocity", dir, velocityCmd, null));


        for (int i = 0; i < cfg.paperCount; i++) {
            int port = cfg.paperBasePort + i;
            String name = "paper-" + (i + 1);
            Path instanceDir = createInstanceDir(instancesDir, name);
            Files.copy(paperJar, instanceDir.resolve("paper.jar"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (cfg.paperPlugin != null) installPlugin(instanceDir, cfg.paperPlugin);
            for (Path plugin : cfg.paperExtraPlugins) {
                installPlugin(instanceDir, plugin);
            }

            touchFile(instanceDir.resolve("eula.txt"));
            touchFile(instanceDir.resolve("server.properties"));
            Files.createDirectories(instanceDir.resolve("config"));
            touchFile(instanceDir.resolve("config").resolve("paper-global.yml"));

            applyRegexReplacesForServer(
                instanceDir,
                "paper",
                i + 1,
                cfg,
                velocityForwardingSecret,
                mysqlJdbc
            );

            // Best-effort: boot once so installed plugins can create their configs, then re-apply patches.
            if (cfg.paperPlugin != null) {
                List<Path> paperBootstrapWaitFor = new ArrayList<>();
                String paperPluginName = tryReadPaperPluginName(cfg.paperPlugin);
                if (paperPluginName != null && !paperPluginName.isBlank()) {
                    paperBootstrapWaitFor.add(instanceDir.resolve("plugins").resolve(paperPluginName));
                    paperBootstrapWaitFor.add(instanceDir.resolve("plugins").resolve(paperPluginName).resolve("config.yml"));
                }

                bootstrapServerOnce(
                    name,
                    instanceDir,
                    cfg.javaBin,
                    cfg.jvmArgs,
                    "paper.jar",
                    List.of("--nogui"),
                    paperBootstrapWaitFor,
                    45_000L
                );

                applyRegexReplacesForServer(
                    instanceDir,
                    "paper",
                    i + 1,
                    cfg,
                    velocityForwardingSecret,
                    mysqlJdbc
                );
            }

            List<String> programArgs = new ArrayList<>();
            programArgs.add("--nogui");
            List<String> paperCmd = ServerProcess.javaCommand(
                cfg.javaBin,
                jvmArgsForMainServer(cfg, name, instanceDir, profilingRunId),
                "paper.jar",
                programArgs
            );
            processes.add(ServerProcess.start(name, instanceDir, paperCmd, null));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.err.println("Failed to sleep: " + e.getMessage());
            }
        }

        if (processes.isEmpty()) {
            System.err.println("Nothing to run. Provide --paper-version/--paper-count and/or --velocity-version.");
            return 2;
        }

        List<CompletableFuture<Process>> exits = processes.stream().map(ServerProcess::onExit).toList();
        CompletableFuture.anyOf(exits.toArray(new CompletableFuture[0])).join();

        int exitCode = 0;
        for (int i = 0; i < processes.size(); i++) {
            ServerProcess p = processes.get(i);
            if (!p.isAlive()) {
                exitCode = exits.get(i).get().exitValue();
                break;
            }
        }

        shutdownServersGracefully(processes, 15_000L);
        return exitCode;
    }

    private static void shutdownServersGracefully(List<ServerProcess> processes, long gracefulTimeoutMs) {
        if (processes == null || processes.isEmpty()) return;

        // Prefer a clean stop so servers can dump JFR (dumponexit=true) and save state.
        for (ServerProcess p : processes) {
            try {
                if (p == null || !p.isAlive()) continue;
                String name = p.name();
                if (name != null && name.startsWith("paper")) {
                    p.sendLine("stop");
                } else if ("velocity".equals(name)) {
                    p.sendLine("shutdown");
                } else {
                    p.sendLine("stop");
                }
            } catch (Exception e) {
                String name;
                try {
                    name = (p != null) ? String.valueOf(p.name()) : "null";
                } catch (Exception nameError) {
                    name = "unknown";
                }
                System.err.println("[shutdown] Failed to request stop (" + name + "): " + e.getMessage());
            }
        }

        long deadline = System.currentTimeMillis() + Math.max(1_000L, gracefulTimeoutMs);
        while (System.currentTimeMillis() < deadline) {
            boolean anyAlive = false;
            for (ServerProcess p : processes) {
                if (p != null && p.isAlive()) {
                    anyAlive = true;
                    break;
                }
            }
            if (!anyAlive) return;
            try {
                //noinspection BusyWait
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Best-effort fallback: attempt termination, then force-kill lingering processes.
        for (ServerProcess p : processes) {
            if (p != null && p.isAlive()) p.destroy();
        }

        long killDeadline = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < killDeadline) {
            boolean anyAlive = false;
            for (ServerProcess p : processes) {
                if (p != null && p.isAlive()) {
                    anyAlive = true;
                    break;
                }
            }
            if (!anyAlive) return;
            try {
                //noinspection BusyWait
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        for (ServerProcess p : processes) {
            if (p != null && p.isAlive()) p.destroyForcibly();
        }

        for (ServerProcess p : processes) {
            try {
                if (p != null) p.waitFor(5_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void applyRegexReplacesForServer(
        Path serverDir,
        String serverType,
        int paperIndex1Based,
        Config cfg,
        String velocityForwardingSecret,
        String mysqlJdbc
    ) throws IOException {
        if (cfg.regexReplaces.isEmpty()) return;

        Map<String, String> vars = new HashMap<>();
        vars.put("server_type", serverType);
        vars.put("server_name", serverDir.getFileName().toString());

        vars.put("paper_count", Integer.toString(cfg.paperCount));
        vars.put("paper_base_port", Integer.toString(cfg.paperBasePort));
        vars.put("paper_index", Integer.toString(paperIndex1Based));
        vars.put("paper_port", Integer.toString(serverType.equals("paper")
            ? cfg.paperBasePort + (paperIndex1Based - 1)
            : cfg.paperBasePort));

        vars.put("velocity_port", Integer.toString(cfg.velocityPort));
        vars.put("velocity_enabled", velocityForwardingSecret != null ? "true" : "false");
        vars.put("velocity_secret", velocityForwardingSecret != null ? velocityForwardingSecret : "");
        vars.put("velocity_servers_block", buildVelocityServersBlock(cfg.paperBasePort, cfg.paperCount));
        vars.put("velocity_forced_hosts_block", "[forced-hosts]\n\"localhost\" = [\"lobby\"]\n");
        if (mysqlJdbc != null) vars.put("mysql_jdbc", mysqlJdbc);

        ServerRunnerPatches.applyRegexReplaces(serverDir, cfg.regexReplaces, vars);
    }

    private static void touchFile(Path file) throws IOException {
        if (Files.exists(file)) return;
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(file, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    private static void bootstrapVelocityTomlIfNeeded(
        Path serverDir,
        String javaBin,
        List<String> jvmArgs,
        Path velocityToml,
        List<Path> waitForPaths
    )
        throws IOException, InterruptedException {
        if (Files.isRegularFile(velocityToml)) return;

        // Velocity generates velocity.toml on first start. We briefly start it to let it bootstrap config.
        List<String> cmd = ServerProcess.javaCommand(javaBin, jvmArgs, "velocity.jar", List.of());
        ServerProcess p = ServerProcess.start("velocity-bootstrap", serverDir, cmd, null);

        long deadline = System.currentTimeMillis() + 30_000L;
        while (System.currentTimeMillis() < deadline) {
            if (Files.isRegularFile(velocityToml)) break;
            if (!p.isAlive()) break;
            //noinspection BusyWait
            Thread.sleep(250);
        }

        // Best-effort: keep it alive a bit longer so installed plugins can create their configs.
        if (waitForPaths != null && !waitForPaths.isEmpty()) {
            long pluginDeadline = System.currentTimeMillis() + 20_000L;
            while (System.currentTimeMillis() < pluginDeadline) {
                if (!p.isAlive()) break;
                if (allExist(waitForPaths)) break;
                //noinspection BusyWait
                Thread.sleep(250);
            }
        }

        p.destroy();
        p.waitFor();

        if (!Files.isRegularFile(velocityToml)) {
            throw new IOException("Velocity did not generate velocity.toml within timeout");
        }
    }

    private static boolean allExist(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return true;
        for (Path p : paths) {
            if (p == null) continue;
            if (!Files.exists(p)) return false;
        }
        return true;
    }

    private static void bootstrapServerOnce(
        String name,
        Path serverDir,
        String javaBin,
        List<String> jvmArgs,
        String jarName,
        List<String> programArgs,
        List<Path> waitForPaths,
        long timeoutMs
    ) {
        try {
            List<String> cmd = ServerProcess.javaCommand(javaBin, jvmArgs, jarName, programArgs);
            ServerProcess p = ServerProcess.start(name + "-bootstrap", serverDir, cmd, null);

            long deadline = System.currentTimeMillis() + Math.max(1_000L, timeoutMs);
            while (System.currentTimeMillis() < deadline) {
                if (!p.isAlive()) break;
                if (waitForPaths != null && !waitForPaths.isEmpty()) {
                    if (allExist(waitForPaths)) break;
                } else {
                    // Fallback: stop once any plugin data directory appeared (jar files alone don't count).
                    Path pluginsDir = serverDir.resolve("plugins");
                    if (Files.isDirectory(pluginsDir)) {
                        try (var stream = Files.list(pluginsDir)) {
                            boolean hasPluginDir = stream.anyMatch(Files::isDirectory);
                            if (hasPluginDir) break;
                        }
                    }
                }
                //noinspection BusyWait
                Thread.sleep(250);
            }

            p.destroy();
            p.waitFor();
        } catch (Exception e) {
            System.out.println("[bootstrap] " + name + " bootstrap failed (best-effort): " + e.getMessage());
        }
    }

    private static String tryReadVelocityPluginId(Path pluginJar) {
        if (pluginJar == null || !Files.isRegularFile(pluginJar)) return null;
        try (JarFile jar = new JarFile(pluginJar.toFile())) {
            var entry = jar.getEntry("velocity-plugin.json");
            if (entry == null) entry = jar.getEntry("META-INF/velocity-plugin.json");
            if (entry == null) return null;
            try (var in = jar.getInputStream(entry)) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                var el = com.google.gson.JsonParser.parseString(json);
                if (!el.isJsonObject()) return null;
                var obj = el.getAsJsonObject();
                var id = obj.get("id");
                return (id != null && id.isJsonPrimitive()) ? id.getAsString() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String tryReadPaperPluginName(Path pluginJar) {
        if (pluginJar == null || !Files.isRegularFile(pluginJar)) return null;
        try (JarFile jar = new JarFile(pluginJar.toFile())) {
            var entry = jar.getEntry("paper-plugin.yml");
            if (entry == null) entry = jar.getEntry("plugin.yml");
            if (entry == null) return null;
            try (var in = jar.getInputStream(entry)) {
                String yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return extractYamlScalar(yml, "name");
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static String extractYamlScalar(String yml, String key) {
        if (yml == null || key == null) return null;
        String[] lines = yml.split("\r?\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (!trimmed.startsWith(key + ":")) continue;
            String raw = trimmed.substring((key + ":").length()).trim();
            if (raw.isEmpty()) return null;
            if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
                raw = raw.substring(1, raw.length() - 1).trim();
            }
            return raw.isBlank() ? null : raw;
        }
        return null;
    }

    private static String readOrGenerateSecret(Path secretFile) throws IOException {
        if (Files.isRegularFile(secretFile)) {
            String existing = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
            if (!existing.isBlank()) return existing;
        }
        return randomSecret(16);
    }

    private static final java.security.SecureRandom RNG = new java.security.SecureRandom();
    private static final String SECRET_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @SuppressWarnings("SameParameterValue")
    private static String randomSecret(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECRET_ALPHABET.charAt(RNG.nextInt(SECRET_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String buildVelocityServersBlock(int paperBasePort, int paperCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("[servers]\n");
        for (int i = 0; i < paperCount; i++) {
            int port = paperBasePort + i;
            String name = (i == 0) ? "lobby" : ("backend" + (i + 1));
            sb.append(name).append(" = \"127.0.0.1:").append(port).append("\"\n");
        }
        return sb.toString();
    }


    private static List<String> jvmArgsForMainServer(Config cfg, String instanceName, Path instanceDir, String runId)
        throws IOException {
        if (cfg == null) return List.of();

        List<String> base = cfg.jvmArgs != null ? cfg.jvmArgs : List.of();
        if (!cfg.jfrEnabled) return base;

        List<String> out = new ArrayList<>(base);

        boolean hasStartFlightRecording = out.stream().anyMatch(
            arg -> arg != null && arg.startsWith("-XX:StartFlightRecording")
        );
        boolean hasFlightRecorderOptions = out.stream().anyMatch(
            arg -> arg != null && arg.startsWith("-XX:FlightRecorderOptions")
        );

        Files.createDirectories(instanceDir.resolve("jfr"));

        String safeName = sanitizeForFileName(instanceName);
        String safeRunId = (runId == null || runId.isBlank()) ? Long.toString(System.currentTimeMillis()) : runId;
        String fileName = "jfr/" + safeName + "-" + safeRunId + ".jfr";

        String settings = (cfg.jfrSettings != null && !cfg.jfrSettings.isBlank())
            ? cfg.jfrSettings.trim()
            : "profile";

        if (!hasStartFlightRecording) {
            out.add(
                "-XX:StartFlightRecording=name=" + safeName
                    + ",settings=" + settings
                    + ",filename=" + fileName
                    + ",dumponexit=true"
            );
        }
        if (!hasFlightRecorderOptions) {
            out.add("-XX:FlightRecorderOptions=stackdepth=128");
        }

        return out;
    }

    private static String sanitizeForFileName(String raw) {
        if (raw == null) return "server";
        String trimmed = raw.trim();
        if (trimmed.isBlank()) return "server";

        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }

        String sanitized = sb.toString();
        return sanitized.isBlank() ? "server" : sanitized;
    }

    private static Path createInstanceDir(Path instancesDir, String name) throws IOException {
        Path dir = instancesDir.resolve(name );
        Files.createDirectories(dir);
        Files.createDirectories(dir.resolve("plugins"));
        return dir;
    }

    private static void installPlugin(Path serverDir, Path pluginJar) throws IOException {
        Path pluginsDir = serverDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Files.copy(pluginJar, pluginsDir.resolve(pluginJar.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static List<String> dockerMysqlCommand(Config cfg, String containerName) {
        // We run detached and clean up explicitly in the shutdown hook so a
        // crashed run doesn't leave the next run broken due to "container already exists".
        return Arrays.asList(
            "docker", "run", "-d",
            "--name", containerName,
            "-p", cfg.mysqlPort + ":3306",
            "-e", "MYSQL_ROOT_PASSWORD=" + cfg.mysqlRootPassword,
            "-e", "MYSQL_DATABASE=" + cfg.mysqlDatabase,
            cfg.mysqlImage
        );
    }

    private static void ensureMysqlContainerRunning(Config cfg, Path workingDir, String containerName)
        throws IOException, InterruptedException {
        DockerContainerState state = dockerContainerState(workingDir, containerName);
        if (state == DockerContainerState.RUNNING) return;

        if (state == DockerContainerState.EXISTS_STOPPED) {
            DockerCommandResult start = runCommand(workingDir, List.of("docker", "start", containerName));
            if (start.exitCode != 0) {
                throw new IOException("Failed to start existing MySQL container '" + containerName + "': " + start.output);
            }
            return;
        }

        DockerCommandResult run = runCommand(workingDir, dockerMysqlCommand(cfg, containerName));
        if (run.exitCode != 0) {
            throw new IOException("Failed to start MySQL container '" + containerName + "': " + run.output);
        }
    }

    private static void cleanupMysqlContainer(Path workingDir, String containerName) {
        try {
            // Best-effort: ignore errors if it doesn't exist anymore.
            runCommand(workingDir, List.of("docker", "rm", "-f", containerName));
        } catch (Exception e) {
            System.err.println("[mysql] Failed to cleanup container '" + containerName + "': " + e.getMessage());
        }
    }

    private enum DockerContainerState {
        NOT_FOUND,
        EXISTS_STOPPED,
        RUNNING
    }

    private static DockerContainerState dockerContainerState(Path workingDir, String containerName)
        throws IOException, InterruptedException {
        DockerCommandResult res = runCommand(workingDir, List.of(
            "docker", "inspect", "-f", "{{.State.Running}}", containerName
        ));
        if (res.exitCode != 0) {
            // docker prints "No such object" on stderr/stdout depending on platform.
            if (res.output.toLowerCase().contains("no such object")) return DockerContainerState.NOT_FOUND;
            return DockerContainerState.NOT_FOUND;
        }
        String v = res.output.trim();
        if (v.equalsIgnoreCase("true")) return DockerContainerState.RUNNING;
        return DockerContainerState.EXISTS_STOPPED;
    }

    private record DockerCommandResult(int exitCode, String output) {}

    private static DockerCommandResult runCommand(Path workingDir, List<String> command)
        throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toAbsolutePath().normalize().toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out;
        try (var in = p.getInputStream()) {
            out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int code = p.waitFor();
        return new DockerCommandResult(code, out);
    }

    @SuppressWarnings("SameParameterValue")
    private static void waitForPortOpen(String host, int port, long timeoutMs) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        IOException last = null;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 750);
                return;
            } catch (IOException e) {
                last = e;
                //noinspection BusyWait
                Thread.sleep(500);
            }
        }
        throw new IOException("Timed out waiting for " + host + ":" + port + " to accept connections", last);
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean waitForMysqlAdminPing(Config cfg, Path workingDir, String containerName, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                DockerCommandResult ping = runCommand(workingDir, List.of(
                    "docker", "exec",
                    containerName,
                    "mysqladmin", "ping",
                    "-h", "127.0.0.1",
                    "-uroot",
                    "-p" + cfg.mysqlRootPassword,
                    "--silent"
                ));
                if (ping.exitCode == 0) {
                    System.out.println("[mysql] mysqladmin ping OK");
                    return true;
                }
            } catch (Exception ignored) {
            }
            try {
                //noinspection BusyWait
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private record Config(
        String javaBin,
        List<String> jvmArgs,
        String paperVersion,
        int paperCount,
        int paperBasePort,
        Path paperPlugin,
        List<Path> paperExtraPlugins,
        String velocityVersion,
        int velocityPort,
        Path velocityPlugin,
        List<Path> velocityExtraPlugins,
        boolean jfrEnabled,
        String jfrSettings,
        Path baseDir,
        Path cacheDir,
        Path instancesDir,
        boolean mysqlEnabled,
        int mysqlPort,
        String mysqlDatabase,
        String mysqlRootPassword,
        String mysqlImage,
        String mysqlContainerName,
        List<ServerRunnerPatches.RegexReplace> regexReplaces
    ) {
            private Config(
                    String javaBin,
                    List<String> jvmArgs,
                    String paperVersion,
                    int paperCount,
                    int paperBasePort,
                    Path paperPlugin,
                    List<Path> paperExtraPlugins,
                    String velocityVersion,
                    int velocityPort,
                    Path velocityPlugin,
                    List<Path> velocityExtraPlugins,
                    boolean jfrEnabled,
                    String jfrSettings,
                    Path baseDir,
                    Path cacheDir,
                    Path instancesDir,
                    boolean mysqlEnabled,
                    int mysqlPort,
                    String mysqlDatabase,
                    String mysqlRootPassword,
                    String mysqlImage,
                    String mysqlContainerName,
                    List<ServerRunnerPatches.RegexReplace> regexReplaces
            ) {
                this.javaBin = javaBin;
                this.jvmArgs = jvmArgs;
                this.paperVersion = paperVersion;
                this.paperCount = paperCount;
                this.paperBasePort = paperBasePort;
                this.paperPlugin = paperPlugin;
                this.paperExtraPlugins = paperExtraPlugins != null ? List.copyOf(paperExtraPlugins) : List.of();
                this.velocityVersion = velocityVersion;
                this.velocityPort = velocityPort;
                this.velocityPlugin = velocityPlugin;
                this.velocityExtraPlugins = velocityExtraPlugins != null ? List.copyOf(velocityExtraPlugins) : List.of();
                this.jfrEnabled = jfrEnabled;
                this.jfrSettings = jfrSettings != null ? jfrSettings : "profile";
                this.baseDir = baseDir;
                this.cacheDir = cacheDir;
                this.instancesDir = instancesDir;
                this.mysqlEnabled = mysqlEnabled;
                this.mysqlPort = mysqlPort;
                this.mysqlDatabase = mysqlDatabase;
                this.mysqlRootPassword = mysqlRootPassword;
                this.mysqlImage = mysqlImage;
                this.mysqlContainerName = mysqlContainerName;
                this.regexReplaces = regexReplaces != null ? regexReplaces : List.of();
            }

            static Config parse(String[] args) {
                Map<String, String> flags = new HashMap<>();
                List<String> jvmArgs = new ArrayList<>();
                List<Path> paperExtraPlugins = new ArrayList<>();
                List<Path> velocityExtraPlugins = new ArrayList<>();
                List<ServerRunnerPatches.RegexReplace> regexReplaces = new ArrayList<>();

                for (int i = 0; i < args.length; i++) {
                    String a = args[i];
                    if (a.equals("--help") || a.equals("-h")) usageAndExit(0);
                    if (a.startsWith("--jvm-arg=")) {
                        jvmArgs.add(a.substring("--jvm-arg=".length()));
                        continue;
                    }
                    if (a.equals("--jvm-arg")) {
                        if (i + 1 >= args.length) usageAndExit(2);
                        jvmArgs.add(args[++i]);
                        continue;
                    }

                    if (a.equals("--replace")) {
                        if (i + 3 >= args.length) usageAndExit(2);
                        String relativePath = args[++i];
                        String regex = args[++i];
                        String replacement = args[++i];
                        regexReplaces.add(new ServerRunnerPatches.RegexReplace(relativePath, regex, replacement));
                        continue;
                    }

                    if (a.startsWith("--paper-extra-plugin=")) {
                        paperExtraPlugins.add(Path.of(a.substring("--paper-extra-plugin=".length())));
                        continue;
                    }
                    if (a.equals("--paper-extra-plugin")) {
                        if (i + 1 >= args.length) usageAndExit(2);
                        paperExtraPlugins.add(Path.of(args[++i]));
                        continue;
                    }

                    if (a.startsWith("--velocity-extra-plugin=")) {
                        velocityExtraPlugins.add(Path.of(a.substring("--velocity-extra-plugin=".length())));
                        continue;
                    }
                    if (a.equals("--velocity-extra-plugin")) {
                        if (i + 1 >= args.length) usageAndExit(2);
                        velocityExtraPlugins.add(Path.of(args[++i]));
                        continue;
                    }

                    if (!a.startsWith("--")) usageAndExit(2);
                    String key = a.substring(2);
                    if (key.equals("mysql")) {
                        flags.put("mysql", "true");
                        continue;
                    }
                    if (key.equals("jfr")) {
                        flags.put("jfr", "true");
                        continue;
                    }
                    String value;
                    int eq = key.indexOf('=');
                    if (eq >= 0) {
                        value = key.substring(eq + 1);
                        key = key.substring(0, eq);
                    } else {
                        if (i + 1 >= args.length) usageAndExit(2);
                        value = args[++i];
                    }
                    flags.put(key, value);
                }

                String javaBin = flags.getOrDefault("java", DEFAULT_JAVA);
                String paperVersion = flags.getOrDefault("paper-version", "latest");
                int paperCount = parseInt(flags.getOrDefault("paper-count", "0"), "paper-count");
                int paperBasePort = parseInt(flags.getOrDefault("paper-base-port", "25566"), "paper-base-port");
                Path paperPlugin = flags.containsKey("paper-plugin") ? Path.of(flags.get("paper-plugin")) : null;

                String velocityVersion = flags.get("velocity-version");
                if (velocityVersion != null && velocityVersion.isBlank()) velocityVersion = null;
                if (velocityVersion != null && velocityVersion.equalsIgnoreCase("latest")) velocityVersion = "latest";
                int velocityPort = parseInt(flags.getOrDefault("velocity-port", "25565"), "velocity-port");
                Path velocityPlugin = flags.containsKey("velocity-plugin") ? Path.of(flags.get("velocity-plugin")) : null;

                boolean jfrEnabled = Boolean.parseBoolean(flags.getOrDefault("jfr", "false"));
                String jfrSettings = flags.getOrDefault("jfr-settings", "profile");

                Path baseDir = flags.containsKey("base-dir") ? Path.of(flags.get("base-dir")) : null;
                Path cacheDir = flags.containsKey("cache-dir") ? Path.of(flags.get("cache-dir")) : null;
                Path instancesDir = flags.containsKey("instances-dir") ? Path.of(flags.get("instances-dir")) : null;

                boolean mysqlEnabled = Boolean.parseBoolean(flags.getOrDefault("mysql", "false"));
                int mysqlPort = parseInt(flags.getOrDefault("mysql-port", "3307"), "mysql-port");
                String mysqlDatabase = flags.getOrDefault("mysql-database", "rapunzellib");
                String mysqlRootPassword = flags.getOrDefault("mysql-root-password", "root");
                String mysqlImage = flags.getOrDefault("mysql-image", "mysql:latest");
                String mysqlContainerName = flags.get("mysql-container-name");

                return new Config(
                        javaBin,
                        jvmArgs,
                        paperVersion,
                        paperCount,
                        paperBasePort,
                        paperPlugin,
                        paperExtraPlugins,
                        velocityVersion,
                        velocityPort,
                        velocityPlugin,
                        velocityExtraPlugins,
                        jfrEnabled,
                        jfrSettings,
                        baseDir,
                        cacheDir,
                        instancesDir,
                        mysqlEnabled,
                        mysqlPort,
                        mysqlDatabase,
                        mysqlRootPassword,
                        mysqlImage,
                        mysqlContainerName,
                        regexReplaces
                );
            }

            String mysqlJdbc() {
                // Use query params so we don't rely on external config, and keep it simple for local dev.
                String user = "root";
                String password = urlEncode(mysqlRootPassword);
                String db = urlEncode(mysqlDatabase);
                return "jdbc:mysql://127.0.0.1:" + mysqlPort + "/" + db
                        + "?user=" + user
                        + "&password=" + password
                        + "&useSSL=false"
                        + "&allowPublicKeyRetrieval=true";
            }

            List<Integer> velocityBackendPorts(int basePort, int count) {
                List<Integer> ports = new ArrayList<>();
                for (int i = 0; i < count; i++) ports.add(basePort + i);
                return ports;
            }

            private static String urlEncode(String s) {
                try {
                    return URLEncoder.encode(s, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid value for URL encoding");
                }
            }

            private static int parseInt(String value, String name) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid --" + name + ": " + value);
                }
            }

            private static void usageAndExit(int code) {
                throw new UsageException(code, """
                        RapunzelLib server-runner (Fill v3)

                        Downloads (via fill.papermc.io v3) and starts temporary Paper + Velocity instances in parallel.
                        
                        Required:
                          --paper-count <n>              e.g. 2 (use 0 to skip Paper)
                        Optional:
                          --paper-version <mcVersion>    e.g. 1.21.10 or 'latest' (default: latest)
                          --velocity-version <version>   e.g. 3.4.0-SNAPSHOT or 'latest' (omit to skip Velocity, default: latest if provided)
                          --paper-base-port <port>       default 25566
                          --velocity-port <port>         default 25565
                          --paper-plugin <pathToJar>     copied into each Paper plugins/
                          --paper-extra-plugin <pathToJar> repeatable (additional plugins copied into each Paper plugins/)
                          --velocity-plugin <pathToJar>  copied into Velocity plugins/
                          --velocity-extra-plugin <pathToJar> repeatable (additional plugins copied into Velocity plugins/)
                          --java <javaBin>               default 'java'
                          --jvm-arg <arg>                repeatable (e.g. --jvm-arg -Xmx2G)
                          --jfr                         enable JFR recordings for long-running servers (written to instances/<name>/jfr/)
                          --jfr-settings <name>          default 'profile' (e.g. 'default', 'profile')
                          --base-dir <dir>               default run/server-runner
                          --cache-dir <dir>              default <base-dir>/cache
                          --instances-dir <dir>          default <base-dir>/instances
                        
                        Regex file replacements (repeatable, best-effort):
                          --replace <relativePath> <regex> <replacement>
                              - Path is relative to the server root (instance directory)
                              - Java regex; replacement uses Java regex replacement syntax
                              - Variables are substituted as {{var}}, e.g. {{velocity_secret}}
                        
                        MySQL (Docker, optional):
                          --mysql                        start a local MySQL container (requires docker)
                          --mysql-port <port>            host port to bind (default 3307)
                          --mysql-database <name>        default rapunzellib
                          --mysql-root-password <pw>     default root
                          --mysql-image <image:tag>      default mysql:8.4
                          --mysql-container-name <name>  default rapunzellib-mysql-<timestamp>
                        """);
            }
        }

    private static final class UsageException extends RuntimeException {
        private final int code;

        private UsageException(int code, String message) {
            super(message);
            this.code = code;
        }
    }
}
