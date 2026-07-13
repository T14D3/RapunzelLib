package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class ContextWrapperGenerationExtension {
    @Inject
    public ContextWrapperGenerationExtension(ObjectFactory objects) {
    }

    public abstract Property<String> getPackageName();

    public abstract Property<String> getClassName();

    public abstract DirectoryProperty getOutputDir();

    /**
     * Whether to transform source files to redirect {@code Rapunzel.*()} calls
     * to the generated wrapper.
     * <p>
     * Defaults to {@code true} for consumer projects and {@code false} for
     * RLib's own library modules (where the original {@code Rapunzel.*()} calls
     * should be preserved).
     */
    public abstract Property<Boolean> getTransformSources();

    public void applyDefaultConventions(Project project) {
        getPackageName().convention("generated.rapunzellib.context");
        getClassName().convention(sanitizeIdentifier(project.getName()) + "Rapunzel");
        getOutputDir().convention(project.getLayout().getProjectDirectory().dir("build/generated/sources/rapunzellib-context-wrapper"));
        // Disable source transformation for RLib's own library modules
        getTransformSources().convention(!"de.t14d3.rapunzellib".equals(project.getGroup()));
    }

    /**
     * Sanitizes a Gradle project name into a valid Java identifier.
     * Replaces hyphens and other non-Java-identifier characters with underscores.
     */
    private static String sanitizeIdentifier(String name) {
        if (name == null || name.isEmpty()) return "Project";
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        // Ensure the first character is valid for a Java identifier
        if (sb.length() > 0 && !Character.isJavaIdentifierStart(sb.charAt(0))) {
            sb.insert(0, '_');
        }
        return sb.toString();
    }
}