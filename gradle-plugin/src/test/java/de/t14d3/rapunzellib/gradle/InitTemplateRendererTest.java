package de.t14d3.rapunzellib.gradle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InitTemplateRendererTest {
    private final InitTemplateSpec spec = new InitTemplateSpec("de.t14d3.example", "ExampleProject", "example-project");

    @Test
    void commonFeatureTemplateUsesCurrentFeatureEntrypoints() {
        List<GeneratedTextFile> generated = InitTemplateRenderer.render(spec);
        GeneratedTextFile commonFeature = generated.stream()
            .filter(file -> file.relativePath().endsWith("CommonFeature.java"))
            .findFirst()
            .orElseThrow();

        assertTrue(commonFeature.content().contains("EventFeatures.install();"));
        assertTrue(commonFeature.content().contains("CommandFeatures.install();"));
        assertTrue(commonFeature.content().contains("GuiFeatures.install();"));
        assertTrue(commonFeature.content().contains("NbtFeatures.install();"));
        assertTrue(commonFeature.content().contains("public static void install()"));
    }

    @Test
    void starterConfigTemplateDocumentsNetworkDefaults() {
        List<GeneratedTextFile> generated = InitTemplateRenderer.render(spec);
        GeneratedTextFile config = generated.stream()
            .filter(file -> file.relativePath().equals("common/src/main/resources/config.yml"))
            .findFirst()
            .orElseThrow();

        assertTrue(config.content().contains("transportPriority: \"plugin_first\""));
        assertTrue(config.content().contains("proxyServerName"));
        assertTrue(config.content().contains("rpcServer:"));
        assertTrue(config.content().contains("enabled: false"));
    }
}
