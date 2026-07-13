package de.t14d3.rapunzellib.livetest.shared;

import de.t14d3.rapunzellib.livetest.LiveTestResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;

/**
 * Writes live test results to report files in JUnit XML and JSON formats.
 *
 * <p>Platform-agnostic - uses only Java stdlib. Reports are written to the
 * specified output directory. If the directory does not exist it will be created.</p>
 */
public final class LiveTestReportWriter {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").withZone(ZoneOffset.UTC);

    private final Path outputDir;
    private final String suiteName;

    /**
     * @param outputDir directory where report files will be written
     * @param suiteName name for the test suite (used in JUnit XML)
     */
    public LiveTestReportWriter(@NotNull Path outputDir, @NotNull String suiteName) {
        this.outputDir = Objects.requireNonNull(outputDir, "outputDir");
        this.suiteName = Objects.requireNonNull(suiteName, "suiteName");
    }

    /**
     * Writes both JUnit XML and JSON report files for the given results.
     *
     * @param results   all test results collected during a run
     * @param timestamp the instant at which the test run started
     * @throws IOException if writing fails
     */
    public void writeReports(@NotNull Collection<LiveTestResult> results, @NotNull Instant timestamp) throws IOException {
        Files.createDirectories(outputDir);
        writeJunitXml(results, timestamp);
        writeJson(results, timestamp);
    }

    // ── JUnit XML ────────────────────────────────────────────────────────────

    private void writeJunitXml(Collection<LiveTestResult> results, Instant timestamp) throws IOException {
        long totalTimeMs = results.stream().mapToLong(LiveTestResult::durationMs).sum();
        long passed = results.stream().filter(LiveTestResult::passed).count();
        long failed = results.stream().filter(LiveTestResult::failed).count();
        long skipped = results.stream().filter(LiveTestResult::skipped).count();
        long errors = results.stream().filter(LiveTestResult::isError).count();

        StringBuilder xml = new StringBuilder(1024);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuite name=\"")
                .append(escapeXml(suiteName))
                .append("\" tests=\"").append(results.size())
                .append("\" failures=\"").append(failed)
                .append("\" errors=\"").append(errors)
                .append("\" skipped=\"").append(skipped)
                .append("\" time=\"").append(formatTimeSeconds(totalTimeMs))
                .append("\" timestamp=\"").append(ISO_FORMATTER.format(timestamp))
                .append("\">\n");

        for (LiveTestResult r : results) {
            xml.append("  <testcase name=\"").append(escapeXml(r.name()))
                    .append("\" classname=\"").append(escapeXml(r.name()))
                    .append("\" time=\"").append(formatTimeSeconds(r.durationMs()))
                    .append("\">\n");

            if (r.failed()) {
                xml.append("    <failure message=\"").append(escapeXml(nullToEmpty(r.message()))).append("\"/>\n");
            } else if (r.isError()) {
                xml.append("    <error message=\"").append(escapeXml(nullToEmpty(r.message()))).append("\"/>\n");
            } else if (r.skipped()) {
                xml.append("    <skipped message=\"").append(escapeXml(nullToEmpty(r.message()))).append("\"/>\n");
            }

            xml.append("  </testcase>\n");
        }

        xml.append("</testsuite>\n");

        Path file = outputDir.resolve("TEST-" + suiteName + ".xml");
        Files.writeString(file, xml.toString(), StandardCharsets.UTF_8);
    }

    // ── JSON ─────────────────────────────────────────────────────────────────

    private void writeJson(Collection<LiveTestResult> results, Instant timestamp) throws IOException {
        long totalTimeMs = results.stream().mapToLong(LiveTestResult::durationMs).sum();
        long passed = results.stream().filter(LiveTestResult::passed).count();
        long failed = results.stream().filter(LiveTestResult::failed).count();
        long skipped = results.stream().filter(LiveTestResult::skipped).count();
        long errors = results.stream().filter(LiveTestResult::isError).count();

        StringBuilder json = new StringBuilder(1024);
        json.append("{\n");
        json.append("  \"suite\": \"").append(escapeJson(suiteName)).append("\",\n");
        json.append("  \"timestamp\": \"").append(ISO_FORMATTER.format(timestamp)).append("\",\n");
        json.append("  \"durationMs\": ").append(totalTimeMs).append(",\n");
        json.append("  \"summary\": {\n");
        json.append("    \"total\": ").append(results.size()).append(",\n");
        json.append("    \"passed\": ").append(passed).append(",\n");
        json.append("    \"failed\": ").append(failed).append(",\n");
        json.append("    \"skipped\": ").append(skipped).append(",\n");
        json.append("    \"errors\": ").append(errors).append("\n");
        json.append("  },\n");
        json.append("  \"tests\": [\n");

        boolean first = true;
        for (LiveTestResult r : results) {
            if (!first) json.append(",\n");
            first = false;
            json.append("    {");
            json.append("\"name\": \"").append(escapeJson(r.name())).append("\"");
            json.append(", \"status\": \"").append(r.status().name()).append("\"");
            json.append(", \"durationMs\": ").append(r.durationMs());
            if (r.message() != null && !r.message().isEmpty()) {
                json.append(", \"message\": \"").append(escapeJson(r.message())).append("\"");
            }
            json.append("}");
        }

        json.append("\n  ]\n");
        json.append("}\n");

        Path file = outputDir.resolve("livetest-results.json");
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String formatTimeSeconds(long millis) {
        return String.format("%.3f", millis / 1000.0);
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
