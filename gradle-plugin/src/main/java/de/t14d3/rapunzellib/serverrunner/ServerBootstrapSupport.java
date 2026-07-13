package de.t14d3.rapunzellib.serverrunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @deprecated Use {@link de.t14d3.rapunzellib.devrunner.DevRunnerBootstrap} instead.
 */
@Deprecated
final class ServerBootstrapSupport {
    private static final SecureRandom RNG = new SecureRandom();
    private static final String SECRET_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private ServerBootstrapSupport() {
    }

    static void shutdownServersGracefully(List<ServerProcess> processes, long gracefulTimeoutMs) {
        if (processes == null || processes.isEmpty()) return;

        for (ServerProcess process : processes) {
            try {
                if (process == null || !process.isAlive()) continue;
                String name = process.name();
                if (name != null && name.startsWith("paper")) {
                    process.sendLine("stop");
                } else if ("velocity".equals(name)) {
                    process.sendLine("shutdown");
                } else {
                    process.sendLine("stop");
                }
            } catch (Exception e) {
                String name;
                try {
                    name = process != null ? String.valueOf(process.name()) : "null";
                } catch (Exception nameError) {
                    name = "unknown";
                }
                System.err.println("[shutdown] Failed to request stop (" + name + "): " + e.getMessage());
            }
        }

        waitForShutdown(processes, Math.max(1_000L, gracefulTimeoutMs));
        if (allStopped(processes)) return;

        for (ServerProcess process : processes) {
            if (process != null && process.isAlive()) process.destroy();
        }

        waitForShutdown(processes, 5_000L);
        if (allStopped(processes)) return;

        for (ServerProcess process : processes) {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }

        for (ServerProcess process : processes) {
            try {
                if (process != null) process.waitFor(5_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static List<String> jvmArgsForMainServer(
        ServerRunnerMain.Config cfg,
        String instanceName,
        Path instanceDir,
        String runId
    ) throws IOException {
        if (cfg == null) return List.of();

        List<String> args = new ArrayList<>(cfg.jvmArgs() != null ? cfg.jvmArgs() : List.of());

        if (!cfg.jfrEnabled()) return args;

        boolean hasStartFlightRecording = args.stream().anyMatch(
            arg -> arg != null && arg.startsWith("-XX:StartFlightRecording")
        );
        boolean hasFlightRecorderOptions = args.stream().anyMatch(
            arg -> arg != null && arg.startsWith("-XX:FlightRecorderOptions")
        );

        Files.createDirectories(instanceDir.resolve("jfr"));

        String safeName = sanitizeForFileName(instanceName);
        String safeRunId = (runId == null || runId.isBlank()) ? Long.toString(System.currentTimeMillis()) : runId;
        String fileName = "jfr/" + safeName + "-" + safeRunId + ".jfr";
        String settings = (cfg.jfrSettings() != null && !cfg.jfrSettings().isBlank()) ? cfg.jfrSettings().trim() : "profile";

        if (!hasStartFlightRecording) {
            args.add(
                "-XX:StartFlightRecording=name=" + safeName
                    + ",settings=" + settings
                    + ",filename=" + fileName
                    + ",dumponexit=true"
            );
        }
        if (!hasFlightRecorderOptions) {
            args.add("-XX:FlightRecorderOptions=stackdepth=128");
        }

        return args;
    }

    static void applyRegexReplacesForServer(
        Path serverDir,
        String serverType,
        int paperIndex1Based,
        ServerRunnerMain.Config cfg,
        String velocityForwardingSecret,
        String mysqlJdbc
    ) throws IOException {
        if (cfg.regexReplaces().isEmpty()) return;

        Map<String, String> variables = new HashMap<>();
        variables.put("server_type", serverType);
        variables.put("server_name", serverDir.getFileName().toString());
        variables.put("paper_count", Integer.toString(cfg.paperCount()));
        variables.put("paper_base_port", Integer.toString(cfg.paperBasePort()));
        variables.put("paper_index", Integer.toString(paperIndex1Based));
        variables.put(
            "paper_port",
            Integer.toString(serverType.equals("paper") ? cfg.paperBasePort() + (paperIndex1Based - 1) : cfg.paperBasePort())
        );
        variables.put("velocity_port", Integer.toString(cfg.velocityPort()));
        variables.put("velocity_enabled", velocityForwardingSecret != null ? "true" : "false");
        variables.put("velocity_secret", velocityForwardingSecret != null ? velocityForwardingSecret : "");
        variables.put("velocity_servers_block", buildVelocityServersBlock(cfg.paperBasePort(), cfg.paperCount()));
        variables.put("velocity_forced_hosts_block", "[forced-hosts]\n\"localhost\" = [\"lobby\"]\n");
        if (mysqlJdbc != null) {
            variables.put("mysql_jdbc", mysqlJdbc);
        }

        ServerRunnerPatches.applyRegexReplaces(serverDir, cfg.regexReplaces(), variables);
    }

    static void touchFile(Path file) throws IOException {
        if (Files.exists(file)) return;
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(file, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    static void bootstrapVelocityTomlIfNeeded(
        Path serverDir,
        String javaBin,
        List<String> jvmArgs,
        Path velocityToml,
        List<Path> waitForPaths
    ) throws IOException, InterruptedException {
        if (Files.isRegularFile(velocityToml)) return;

        List<String> command = ServerProcess.javaCommand(javaBin, jvmArgs, "velocity.jar", List.of());
        ServerProcess process = ServerProcess.start("velocity-bootstrap", serverDir, command, null);

        long deadline = System.currentTimeMillis() + 30_000L;
        while (System.currentTimeMillis() < deadline) {
            if (Files.isRegularFile(velocityToml) || !process.isAlive()) break;
            Thread.sleep(250);
        }

        if (waitForPaths != null && !waitForPaths.isEmpty()) {
            long pluginDeadline = System.currentTimeMillis() + 20_000L;
            while (System.currentTimeMillis() < pluginDeadline) {
                if (!process.isAlive() || allExist(waitForPaths)) break;
                Thread.sleep(250);
            }
        }

        process.destroy();
        process.waitFor();

        if (!Files.isRegularFile(velocityToml)) {
            throw new IOException("Velocity did not generate velocity.toml within timeout");
        }
    }

    static void bootstrapServerOnce(
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
            List<String> command = ServerProcess.javaCommand(javaBin, jvmArgs, jarName, programArgs);
            ServerProcess process = ServerProcess.start(name + "-bootstrap", serverDir, command, null);

            long deadline = System.currentTimeMillis() + Math.max(1_000L, timeoutMs);
            while (System.currentTimeMillis() < deadline) {
                if (!process.isAlive()) break;
                if (waitForPaths != null && !waitForPaths.isEmpty()) {
                    if (allExist(waitForPaths)) break;
                } else if (hasPluginDataDirectory(serverDir)) {
                    break;
                }
                Thread.sleep(250);
            }

            process.destroy();
            process.waitFor();
        } catch (Exception e) {
            System.out.println("[bootstrap] " + name + " bootstrap failed (best-effort): " + e.getMessage());
        }
    }

    static String tryReadVelocityPluginId(Path pluginJar) {
        if (pluginJar == null || !Files.isRegularFile(pluginJar)) return null;
        try (JarFile jar = new JarFile(pluginJar.toFile())) {
            var entry = jar.getEntry("velocity-plugin.json");
            if (entry == null) entry = jar.getEntry("META-INF/velocity-plugin.json");
            if (entry == null) return null;
            try (var in = jar.getInputStream(entry)) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                var element = com.google.gson.JsonParser.parseString(json);
                if (!element.isJsonObject()) return null;
                var id = element.getAsJsonObject().get("id");
                return id != null && id.isJsonPrimitive() ? id.getAsString() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    static String tryReadPaperPluginName(Path pluginJar) {
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

    static String readOrGenerateSecret(Path secretFile) throws IOException {
        if (Files.isRegularFile(secretFile)) {
            String existing = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
            if (!existing.isBlank()) return existing;
        }
        return randomSecret(16);
    }

    static Path createInstanceDir(Path instancesDir, String name) throws IOException {
        Path dir = instancesDir.resolve(name);
        Files.createDirectories(dir);
        Files.createDirectories(dir.resolve("plugins"));
        return dir;
    }

    static void installPlugin(Path serverDir, Path pluginJar) throws IOException {
        Path pluginsDir = serverDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Files.copy(pluginJar, pluginsDir.resolve(pluginJar.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean allExist(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return true;
        for (Path path : paths) {
            if (path != null && !Files.exists(path)) return false;
        }
        return true;
    }

    private static boolean hasPluginDataDirectory(Path serverDir) throws IOException {
        Path pluginsDir = serverDir.resolve("plugins");
        if (!Files.isDirectory(pluginsDir)) return false;
        try (var stream = Files.list(pluginsDir)) {
            return stream.anyMatch(Files::isDirectory);
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
            String name = i == 0 ? "lobby" : ("backend" + (i + 1));
            sb.append(name).append(" = \"127.0.0.1:").append(port).append("\"\n");
        }
        return sb.toString();
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

    private static void waitForShutdown(List<ServerProcess> processes, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (allStopped(processes)) return;
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static boolean allStopped(List<ServerProcess> processes) {
        for (ServerProcess process : processes) {
            if (process != null && process.isAlive()) return false;
        }
        return true;
    }
}
