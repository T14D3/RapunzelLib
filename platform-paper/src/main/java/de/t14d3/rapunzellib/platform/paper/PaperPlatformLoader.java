package de.t14d3.rapunzellib.platform.paper;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

final class PaperPlatformLoader {
    private static final String EMBEDDED_PLUGIN_JAR = "META-INF/rapunzellib/platform-paper-plugin.jar";
    private static final String COMPANION_MAIN_CLASS = "de.t14d3.rapunzellib.platform.paper.PaperPlatformPlugin";
    private static final Object LOAD_LOCK = new Object();

    private PaperPlatformLoader() {
    }

    static @NotNull JavaPlugin ensureLoaded(@NotNull JavaPlugin consumerPlugin) {
        Objects.requireNonNull(consumerPlugin, "consumerPlugin");

        synchronized (LOAD_LOCK) {
            PluginManager pluginManager = Bukkit.getPluginManager();
            EmbeddedCompanion companion = loadEmbeddedCompanion(consumerPlugin.getClass().getClassLoader());
            Plugin existing = pluginManager.getPlugin(PaperPlatformBootstrapHost.PLUGIN_NAME);
            if (existing instanceof JavaPlugin javaPlugin) {
                verifyLoadedCompanion(javaPlugin, companion.description());
                if (!javaPlugin.isEnabled()) {
                    pluginManager.enablePlugin(javaPlugin);
                }
                return javaPlugin;
            }

            Path dataDirectory = consumerPlugin.getDataFolder().toPath().toAbsolutePath();
            Path pluginsDir = dataDirectory.getParent();
            if (pluginsDir == null) {
                throw new IllegalStateException("Failed to resolve plugins directory for " + consumerPlugin.getName());
            }

            Path extractedJar = extractEmbeddedCompanion(companion, pluginsDir);

            try {
                Plugin loaded = pluginManager.loadPlugin(extractedJar.toFile());
                if (!(loaded instanceof JavaPlugin javaPlugin)) {
                    throw new IllegalStateException(
                        "Loaded Paper platform companion is not a JavaPlugin: " + extractedJar
                    );
                }
                verifyLoadedCompanion(javaPlugin, companion.description());
                if (!javaPlugin.isEnabled()) {
                    pluginManager.enablePlugin(javaPlugin);
                }
                return javaPlugin;
            } catch (InvalidPluginException | InvalidDescriptionException e) {
                throw new IllegalStateException("Failed to load RapunzelLib Paper platform companion from " + extractedJar, e);
            }
        }
    }

    static @NotNull String extractedJarFileName(@NotNull PluginDescriptionFile description, byte @NotNull [] companionBytes) {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(companionBytes, "companionBytes");

        String version = sanitizeFileComponent(description.getVersion());
        String digest = shortDigest(companionBytes);
        return PaperPlatformBootstrapHost.PLUGIN_NAME + '-' + version + '-' + digest + ".jar";
    }

    static @NotNull Path writeCompanionJarIfNecessary(byte @NotNull [] companionBytes, @NotNull Path extractedJar) {
        Objects.requireNonNull(companionBytes, "companionBytes");
        Objects.requireNonNull(extractedJar, "extractedJar");

        try {
            Files.createDirectories(Objects.requireNonNull(extractedJar.getParent(), "extractedJar.parent"));
            if (Files.isRegularFile(extractedJar) && MessageDigest.isEqual(Files.readAllBytes(extractedJar), companionBytes)) {
                return extractedJar;
            }

            Path tempFile = extractedJar.resolveSibling(extractedJar.getFileName() + ".tmp");
            Files.write(tempFile, companionBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(tempFile, extractedJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tempFile, extractedJar, StandardCopyOption.REPLACE_EXISTING);
            }
            return extractedJar;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to extract RapunzelLib Paper platform companion", exception);
        }
    }

    static @NotNull PluginDescriptionFile pluginDescription(byte @NotNull [] companionBytes) {
        Objects.requireNonNull(companionBytes, "companionBytes");

        try (JarInputStream jarInputStream = new JarInputStream(new ByteArrayInputStream(companionBytes))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if ("plugin.yml".equals(entry.getName())) {
                    return new PluginDescriptionFile(jarInputStream);
                }
            }
        } catch (IOException | InvalidDescriptionException exception) {
            throw new IllegalStateException("Failed to read embedded Paper platform companion metadata", exception);
        }
        throw new IllegalStateException("Embedded Paper platform companion is missing plugin.yml");
    }

    private static @NotNull EmbeddedCompanion loadEmbeddedCompanion(@NotNull ClassLoader classLoader) {
        try (InputStream inputStream = classLoader.getResourceAsStream(EMBEDDED_PLUGIN_JAR)) {
            if (inputStream == null) {
                throw new IllegalStateException(
                    "Missing embedded RapunzelLib Paper platform companion at " + EMBEDDED_PLUGIN_JAR
                );
            }
            byte[] companionBytes = inputStream.readAllBytes();
            return new EmbeddedCompanion(companionBytes, pluginDescription(companionBytes));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read RapunzelLib Paper platform companion", exception);
        }
    }

    private static @NotNull Path extractEmbeddedCompanion(@NotNull EmbeddedCompanion companion, @NotNull Path pluginsDir) {
        Path extractedJar = pluginsDir.resolve(extractedJarFileName(companion.description(), companion.bytes()));
        return writeCompanionJarIfNecessary(companion.bytes(), extractedJar);
    }

    private static void verifyLoadedCompanion(@NotNull JavaPlugin plugin, @NotNull PluginDescriptionFile expectedDescription) {
        String actualMain = plugin.getDescription().getMain();
        String actualVersion = plugin.getDescription().getVersion();
        String expectedMain = expectedDescription.getMain();
        String expectedVersion = expectedDescription.getVersion();

        if (!COMPANION_MAIN_CLASS.equals(actualMain) || !Objects.equals(expectedMain, actualMain)) {
            throw new IllegalStateException(
                "Loaded Paper platform companion has unexpected main class: " + actualMain + " (expected " + COMPANION_MAIN_CLASS + ')'
            );
        }
        if (!Objects.equals(expectedVersion, actualVersion)) {
            throw new IllegalStateException(
                "Loaded Paper platform companion version mismatch: " + actualVersion + " (expected " + expectedVersion + ')'
            );
        }
    }

    private static @NotNull String sanitizeFileComponent(@NotNull String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static @NotNull String shortDigest(byte @NotNull [] companionBytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(companionBytes);
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Missing SHA-256 support", exception);
        }
    }

    private record EmbeddedCompanion(byte[] bytes, PluginDescriptionFile description) {
    }

}
