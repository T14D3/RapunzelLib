package de.t14d3.rapunzellib.platform.paper;

import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PaperPlatformLoaderTest {
    @Test
    void pluginDescriptionReadsEmbeddedCompanionMetadata() throws IOException {
        byte[] jar = companionJar("1.2.3");

        PluginDescriptionFile description = PaperPlatformLoader.pluginDescription(jar);

        assertEquals(PaperPlatformBootstrapHost.PLUGIN_NAME, description.getName());
        assertEquals("de.t14d3.rapunzellib.platform.paper.PaperPlatformPlugin", description.getMain());
        assertEquals("1.2.3", description.getVersion());
    }

    @Test
    void extractedJarFileNameIsVersionedAndStable() throws IOException {
        byte[] jar = companionJar("1.2.3");
        PluginDescriptionFile description = PaperPlatformLoader.pluginDescription(jar);

        String fileName = PaperPlatformLoader.extractedJarFileName(description, jar);

        assertTrue(fileName.startsWith(PaperPlatformBootstrapHost.PLUGIN_NAME + "-1.2.3-"));
        assertTrue(fileName.endsWith(".jar"));
        assertEquals(fileName, PaperPlatformLoader.extractedJarFileName(description, jar));
    }

    @Test
    void writeCompanionJarIsIdempotentForSameBytes(@TempDir Path dir) throws Exception {
        byte[] jar = companionJar("1.2.3");
        Path output = dir.resolve("companion.jar");

        PaperPlatformLoader.writeCompanionJarIfNecessary(jar, output);
        long firstModified = Files.getLastModifiedTime(output).toMillis();
        Thread.sleep(5L);
        PaperPlatformLoader.writeCompanionJarIfNecessary(jar, output);
        long secondModified = Files.getLastModifiedTime(output).toMillis();

        assertEquals(firstModified, secondModified);
        assertEquals(jar.length, Files.size(output));
    }

    @Test
    void writeCompanionJarReplacesChangedBytes(@TempDir Path dir) throws Exception {
        byte[] original = companionJar("1.2.3");
        byte[] updated = companionJar("1.2.4");
        Path output = dir.resolve("companion.jar");

        PaperPlatformLoader.writeCompanionJarIfNecessary(original, output);
        Thread.sleep(5L);
        PaperPlatformLoader.writeCompanionJarIfNecessary(updated, output);

        PluginDescriptionFile description = PaperPlatformLoader.pluginDescription(Files.readAllBytes(output));
        assertEquals("1.2.4", description.getVersion());
    }

    private static byte[] companionJar(String version) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(output)) {
            jar.putNextEntry(new JarEntry("plugin.yml"));
            jar.write(("""
                name: RapunzelLibPlatformPaper
                main: de.t14d3.rapunzellib.platform.paper.PaperPlatformPlugin
                version: %s
                api-version: '1.21'
                """.formatted(version)).stripIndent().getBytes());
            jar.closeEntry();
        }
        return output.toByteArray();
    }
}
