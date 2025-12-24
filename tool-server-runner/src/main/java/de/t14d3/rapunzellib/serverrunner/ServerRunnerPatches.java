package de.t14d3.rapunzellib.serverrunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ServerRunnerPatches {
    record RegexReplace(String relativePath, String regex, String replacement) {}

    private ServerRunnerPatches() {
    }

    static void applyRegexReplaces(Path serverDir, List<RegexReplace> patches, Map<String, String> variables)
        throws IOException {
        if (patches == null || patches.isEmpty()) return;

        Path serverRoot = serverDir.toAbsolutePath().normalize();

        Map<String, List<RegexReplace>> byFile = new LinkedHashMap<>();
        for (RegexReplace patch : patches) {
            if (patch == null) continue;
            String relPath = substitute(patch.relativePath(), variables);
            String regex = substitute(patch.regex(), variables);
            String replacement = substitute(patch.replacement(), variables);
            byFile.computeIfAbsent(relPath, ignored -> new ArrayList<>())
                .add(new RegexReplace(relPath, regex, replacement));
        }

        for (var entry : byFile.entrySet()) {
            String relativePath = entry.getKey();
            if (relativePath == null || relativePath.isBlank()) continue;

            Path relPath = Path.of(relativePath);
            if (relPath.isAbsolute()) {
                throw new IOException("Patch path must be relative to the server root: " + relativePath);
            }

            Path targetFile = serverRoot.resolve(relPath).normalize();
            if (!targetFile.startsWith(serverRoot)) {
                throw new IOException("Patch path escapes server root: " + relativePath);
            }

            if (!Files.isRegularFile(targetFile)) {
                System.out.println("[patch] Skip missing " + relativePath);
                continue;
            }

            String original = Files.readString(targetFile, StandardCharsets.UTF_8);
            String updated = original;

            for (RegexReplace patch : entry.getValue()) {
                Pattern pattern = Pattern.compile(patch.regex(), Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(updated);
                if (!matcher.find()) {
                    System.out.println("[patch] No match in " + relativePath + " for /" + shorten(patch.regex()) + "/");
                    continue;
                }
                updated = matcher.replaceAll(patch.replacement());
            }

            if (!updated.equals(original)) {
                Path backup = targetFile.resolveSibling(targetFile.getFileName() + ".backup");
                Files.copy(targetFile, backup, StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(targetFile, updated, StandardCharsets.UTF_8);
                System.out.println("[patch] Patched " + relativePath);
            }
        }
    }

    private static String substitute(String s, Map<String, String> variables) {
        if (s == null || s.isEmpty()) return s;
        if (variables == null || variables.isEmpty()) return s;

        String out = s;
        for (var entry : variables.entrySet()) {
            out = out.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return out;
    }

    private static String shorten(String s) {
        if (s == null) return "null";
        int max = 120;
        String collapsed = s.replace('\n', ' ').replace('\r', ' ');
        if (collapsed.length() <= max) return collapsed;
        return collapsed.substring(0, max - 3) + "...";
    }
}
