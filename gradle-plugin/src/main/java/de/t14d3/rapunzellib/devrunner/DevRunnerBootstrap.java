package de.t14d3.rapunzellib.devrunner;

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

public final class DevRunnerBootstrap {
    private static final SecureRandom RNG = new SecureRandom();
    private static final String SECRET_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private DevRunnerBootstrap() {
    }

    public static void touchFile(Path file) throws IOException {
        if (Files.exists(file)) return;
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(file, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    public static String readOrGenerateSecret(Path secretFile) throws IOException {
        if (Files.isRegularFile(secretFile)) {
            String existing = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
            if (!existing.isBlank()) return existing;
        }
        return randomSecret(16);
    }

    public static void applyRegexReplaces(
        Path serverDir,
        List<DevRunnerConfig.RegexReplace> patches,
        Map<String, String> variables
    ) throws IOException {
        if (patches == null || patches.isEmpty()) return;

        Path serverRoot = serverDir.toAbsolutePath().normalize();

        for (DevRunnerConfig.RegexReplace patch : patches) {
            if (patch.serverPattern() != null && !patch.serverPattern().isBlank()) {
                // Server pattern filtering is handled by the caller
            }

            String relPath = substitute(patch.relativePath(), variables);
            String regex = substitute(patch.regex(), variables);
            String replacement = substitute(patch.replacement(), variables);

            Path targetFile = serverRoot.resolve(relPath).normalize();
            if (!targetFile.startsWith(serverRoot)) {
                throw new IOException("Patch path escapes server root: " + relPath);
            }

            if (!Files.isRegularFile(targetFile)) {
                System.out.println("[patch] Skip missing " + relPath);
                continue;
            }

            String original = Files.readString(targetFile, StandardCharsets.UTF_8);
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.MULTILINE);
            java.util.regex.Matcher matcher = pattern.matcher(original);
            if (!matcher.find()) {
                System.out.println("[patch] No match in " + relPath);
                continue;
            }

            String updated = matcher.replaceAll(replacement);
            if (!updated.equals(original)) {
                Path backup = targetFile.resolveSibling(targetFile.getFileName() + ".backup");
                Files.copy(targetFile, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(targetFile, updated, StandardCharsets.UTF_8);
                System.out.println("[patch] Patched " + relPath);
            }
        }
    }

    public static Map<String, String> buildServerVariables(
        DevRunnerConfig.ServerSpec spec,
        String serverName,
        int serverIndex,
        DevRunnerConfig cfg,
        String forwardingSecret,
        String mysqlJdbc
    ) {
        Map<String, String> vars = new HashMap<>();
        vars.put("server_name", serverName);
        vars.put("server_type", spec.platform());
        vars.put("server_port", Integer.toString(spec.port()));
        vars.put("paper_index", Integer.toString(serverIndex));
        vars.put("paper_port", Integer.toString(spec.port()));
        vars.put("velocity_port", Integer.toString(spec.port()));

        boolean hasVelocity = cfg.servers().values().stream().anyMatch(s -> "velocity".equals(s.platform()));
        vars.put("velocity_enabled", hasVelocity ? "true" : "false");
        vars.put("velocity_secret", forwardingSecret != null ? forwardingSecret : "");

        if (mysqlJdbc != null) {
            vars.put("mysql_jdbc", mysqlJdbc);
        }

        // Build velocity servers block
        StringBuilder velocityServers = new StringBuilder();
        velocityServers.append("[servers]\n");
        for (var entry : cfg.servers().entrySet()) {
            if ("velocity".equals(entry.getValue().platform())) continue;
            velocityServers.append(entry.getKey()).append(" = \"127.0.0.1:").append(entry.getValue().port()).append("\"\n");
        }
        vars.put("velocity_servers_block", velocityServers.toString());

        return vars;
    }

    public static String buildVelocityServersBlock(DevRunnerConfig cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append("[servers]\n");
        for (var entry : cfg.servers().entrySet()) {
            if ("velocity".equals(entry.getValue().platform())) continue;
            sb.append(entry.getKey()).append(" = \"127.0.0.1:").append(entry.getValue().port()).append("\"\n");
        }
        return sb.toString();
    }

    public static String substitute(String s, Map<String, String> variables) {
        if (s == null || s.isEmpty() || variables == null || variables.isEmpty()) return s;
        String out = s;
        for (var entry : variables.entrySet()) {
            out = out.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return out;
    }

    private static String randomSecret(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECRET_ALPHABET.charAt(RNG.nextInt(SECRET_ALPHABET.length())));
        }
        return sb.toString();
    }
}
