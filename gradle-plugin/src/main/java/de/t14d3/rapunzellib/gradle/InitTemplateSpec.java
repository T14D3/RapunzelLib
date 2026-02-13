package de.t14d3.rapunzellib.gradle;

public record InitTemplateSpec(
    String basePackageName,
    String projectName,
    String pluginId
) {
    public String packagePath() {
        return basePackageName.replace('.', '/');
    }

    public String version() {
        return "1.21.11-R0.1-SNAPSHOT";
    }
}
