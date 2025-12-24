package de.t14d3.rapunzellib.serverrunner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

final class FillV3Client {
    private static final URI BASE = URI.create("https://fill.papermc.io/v3/");
    private static final String USER_AGENT = "RapunzelLib/FillV3Client (https://github.com/T14D3/RapunzelLib)";

    private final HttpClient http;

    FillV3Client() {
        this.http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    }

    record ResolvedBuild(int build, String downloadName, String sha256, String url) {}

    String resolveLatestVersion(String project) throws IOException, InterruptedException {
        Objects.requireNonNull(project, "project");

        // Fill v3 exposes a dedicated versions listing endpoint where the newest version is index 0.
        URI versionsUri = BASE.resolve("projects/" + project + "/versions");
        HttpRequest req = HttpRequest.newBuilder(versionsUri)
            .timeout(Duration.ofSeconds(30))
            .header("accept", "application/json")
            .header("user-agent", USER_AGENT)
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Fill v3 versions request failed: " + resp.statusCode() + " " + versionsUri);
        }

        JsonElement parsed = JsonParser.parseString(resp.body());
        if (parsed.isJsonObject()) {
            JsonObject root = parsed.getAsJsonObject();
            JsonElement versionsEl = root.get("versions");
            if (versionsEl != null && versionsEl.isJsonArray() && !versionsEl.getAsJsonArray().isEmpty()) {
                return extractVersionId(versionsEl.getAsJsonArray().get(0));
            }
        } else if (parsed.isJsonArray()) {
            var arr = parsed.getAsJsonArray();
            if (!arr.isEmpty()) return extractVersionId(arr.get(0));
        }

        throw new IOException("No versions found for project " + project);
    }

    ResolvedBuild resolveLatestBuild(String project, String version) throws IOException, InterruptedException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(version, "version");

        if (version.equalsIgnoreCase("latest")) {
            version = resolveLatestVersion(project);
        }

        URI buildsUri = BASE.resolve("projects/" + project + "/versions/" + version + "/builds");
        HttpRequest req = HttpRequest.newBuilder(buildsUri)
            .timeout(Duration.ofSeconds(30))
            .header("accept", "application/json")
            .header("user-agent", USER_AGENT)
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Fill v3 builds request failed: " + resp.statusCode() + " " + buildsUri);
        }

        JsonElement parsed = JsonParser.parseString(resp.body());
        com.google.gson.JsonArray builds;
        if (parsed.isJsonArray()) {
            builds = parsed.getAsJsonArray();
        } else if (parsed.isJsonObject()) {
            JsonObject root = parsed.getAsJsonObject();
            builds = root.getAsJsonArray("builds");
        } else {
            throw new IOException("Unexpected builds response type for " + buildsUri);
        }
        if (builds == null || builds.isEmpty()) throw new IOException("No builds found for " + project + " " + version);

        int buildNumber = -1;
        for (JsonElement el : builds) {
            JsonObject obj = el.getAsJsonObject();
            int candidate = getBuildNumber(obj);
            if (candidate > buildNumber) buildNumber = candidate;
        }
        if (buildNumber < 0) throw new IOException("No build id found for " + project + " " + version);

        JsonObject details = fetchBuildDetails(project, version, buildNumber);
        JsonObject downloads = details.getAsJsonObject("downloads");
        if (downloads == null || downloads.isEmpty()) {
            throw new IOException("No downloads listed for " + project + " " + version + " build " + buildNumber);
        }

        JsonObject chosen = chooseDownload(downloads);
        String name = chosen.get("name").getAsString();
        String sha256 = extractSha256(chosen);
        String url = extractUrl(chosen);
        return new ResolvedBuild(buildNumber, name, sha256, url);
    }

    Path downloadJar(String project, String version, ResolvedBuild build, Path cacheDir) throws IOException, InterruptedException {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(cacheDir, "cacheDir");

        if (version.equalsIgnoreCase("latest")) {
            version = resolveLatestVersion(project);
        }

        Path projectDir = cacheDir.resolve(project).resolve(version).resolve(Integer.toString(build.build()));
        Files.createDirectories(projectDir);
        Path target = projectDir.resolve(build.downloadName());
        if (Files.exists(target)) {
            if (build.sha256() == null || verifySha256(target, build.sha256())) return target;
            Files.delete(target);
        }

        URI downloadUri = build.url() != null
            ? URI.create(build.url())
            : BASE.resolve(
                "projects/" + project + "/versions/" + version + "/builds/" + build.build() + "/downloads/" + build.downloadName()
            );

        HttpRequest req = HttpRequest.newBuilder(downloadUri)
            .timeout(Duration.ofMinutes(2))
            .header("accept", "application/octet-stream")
            .header("user-agent", USER_AGENT)
            .build();

        HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("Fill v3 download request failed: " + resp.statusCode() + " " + downloadUri);
        }

        Path tmp = Files.createTempFile(projectDir, build.downloadName() + ".", ".part");
        try (InputStream in = resp.body()) {
            Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        if (build.sha256() != null && !verifySha256(tmp, build.sha256())) {
            Files.deleteIfExists(tmp);
            throw new IOException("SHA-256 mismatch for " + downloadUri);
        }

        Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static boolean verifySha256(Path file, String expectedHexLowercase) throws IOException {
        String actual = sha256Hex(file);
        return actual.equalsIgnoreCase(expectedHexLowercase);
    }

    private static String sha256Hex(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                digest.update(buf, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static int getBuildNumber(JsonObject obj) throws IOException {
        JsonElement buildEl = obj.get("build");
        if (buildEl == null) buildEl = obj.get("id"); // Fill v3 uses `id` for build number in some responses
        if (buildEl == null || !buildEl.isJsonPrimitive()) {
            throw new IOException("Could not determine build number from: " + obj);
        }
        try {
            return buildEl.getAsInt();
        } catch (NumberFormatException e) {
            throw new IOException("Invalid build number in: " + obj, e);
        }
    }

    private static String extractVersionId(JsonElement el) throws IOException {
        if (el == null || el.isJsonNull()) throw new IOException("Version element was null");
        if (el.isJsonPrimitive()) return el.getAsString();
        if (!el.isJsonObject()) throw new IOException("Unexpected version element type: " + el);

        JsonObject obj = el.getAsJsonObject();
        if (obj.has("id") && obj.get("id").isJsonPrimitive()) return obj.get("id").getAsString();
        if (obj.has("version") && obj.get("version").isJsonObject()) {
            JsonObject version = obj.getAsJsonObject("version");
            if (version.has("id") && version.get("id").isJsonPrimitive()) return version.get("id").getAsString();
        }
        throw new IOException("Could not extract version id from: " + obj);
    }

    private JsonObject fetchBuildDetails(String project, String version, int build) throws IOException, InterruptedException {
        URI uri = BASE.resolve("projects/" + project + "/versions/" + version + "/builds/" + build);
        HttpRequest req = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(30))
            .header("accept", "application/json")
            .header("user-agent", USER_AGENT)
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Fill v3 build details request failed: " + resp.statusCode() + " " + uri);
        }
        JsonElement parsed = JsonParser.parseString(resp.body());
        if (!parsed.isJsonObject()) throw new IOException("Unexpected build details response type for " + uri);
        return parsed.getAsJsonObject();
    }

    private static JsonObject chooseDownload(JsonObject downloads) throws IOException {
        // Velocity uses "server:default"; Paper currently uses "application" (but handle both).
        String[] preferredKeys = new String[] { "application", "server:default", "server", "default" };
        for (String key : preferredKeys) {
            if (downloads.has(key) && downloads.get(key).isJsonObject()) return downloads.getAsJsonObject(key);
        }
        // Otherwise pick first entry (stable ordering by key for determinism).
        return downloads.entrySet().stream().min(Map.Entry.comparingByKey())
            .orElseThrow(() -> new IOException("No downloads entries found"))
            .getValue()
            .getAsJsonObject();
    }

    private static String extractSha256(JsonObject download) {
        if (download.has("sha256") && download.get("sha256").isJsonPrimitive()) return download.get("sha256").getAsString();
        if (download.has("checksums") && download.get("checksums").isJsonObject()) {
            JsonObject checksums = download.getAsJsonObject("checksums");
            if (checksums.has("sha256") && checksums.get("sha256").isJsonPrimitive()) return checksums.get("sha256").getAsString();
        }
        return null;
    }

    private static String extractUrl(JsonObject download) {
        if (download.has("url") && download.get("url").isJsonPrimitive()) return download.get("url").getAsString();
        return null;
    }
}
