package de.t14d3.rapunzellib.buildlogic.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public abstract class CheckReposiliteConfigTask extends DefaultTask {
    @Input
    @Optional
    public abstract Property<String> getReposiliteBaseUrl();

    @Input
    @Optional
    public abstract Property<String> getReposiliteUsername();

    @Input
    @Optional
    public abstract Property<String> getReposilitePassword();

    @TaskAction
    public void validate() {
        String baseUrl = trimToNull(getReposiliteBaseUrl().getOrNull());
        String username = trimToNull(getReposiliteUsername().getOrNull());
        String password = trimToNull(getReposilitePassword().getOrNull());

        StringBuilder message = new StringBuilder();
        appendMissing(message, username, "reposiliteUsername/REPOSILITE_USERNAME");
        appendMissing(message, password, "reposilitePassword/REPOSILITE_PASSWORD");

        if (baseUrl != null && !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            appendLine(message, "reposiliteBaseUrl/REPOSILITE_BASE_URL must start with http:// or https://");
        }

        if (message.length() > 0) {
            throw new GradleException(
                "Missing Reposilite publishing configuration:\n"
                    + message
                    + "\nConfigure these as Gradle properties or environment variables."
            );
        }
    }

    private static void appendMissing(StringBuilder message, String value, String key) {
        if (value == null) {
            appendLine(message, key);
        }
    }

    private static void appendLine(StringBuilder message, String line) {
        if (message.length() > 0) {
            message.append('\n');
        }
        message.append("- ").append(line);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
