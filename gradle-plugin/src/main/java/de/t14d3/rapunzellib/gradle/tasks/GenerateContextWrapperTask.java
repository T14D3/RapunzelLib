package de.t14d3.rapunzellib.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates a per-project static wrapper around {@code RapunzelContext}
 * <strong>and</strong> rewrites {@code Rapunzel.method()} calls in source files
 * to use the generated wrapper instead.
 *
 * <p>This lets developers write portable {@code Rapunzel.context()},
 * {@code Rapunzel.messages()}, etc. in their source code. At build time the
 * task rewrites those calls to e.g. {@code MyPluginRapunzel.context()},
 * {@code MyPluginRapunzel.messages()}, giving each project a direct reference
 * to its own context without the ambiguity of the global {@code Rapunzel.*()} API.</p>
 */
@DisableCachingByDefault
public abstract class GenerateContextWrapperTask extends DefaultTask {

    /** Methods that exist on both {@code Rapunzel} and the generated wrapper. */
    private static final Set<String> REDIRECTABLE_METHODS = Set.of(
        "context", "runtime", "platformId", "logger", "dataDirectory",
        "resources", "scheduler", "services", "registries", "configs",
        "messages", "players", "entities", "entityTypes", "itemTypes",
        "blockTypes", "worlds", "blocks", "attachments", "nativeInterop",
        "owner", "supports", "supportsAttachments", "requireAttachmentSupport",
        "dispatchCommand", "close"
    );

    /** Matches {@code Rapunzel.<redirectable_method>(}. */
    private static final Pattern RAPUNZEL_CALL = Pattern.compile(
        "\\bRapunzel\\.(?<method>" + String.join("|", REDIRECTABLE_METHODS) + ")\\s*\\("
    );

    /** Matches an explicit {@code import de.t14d3.rapunzellib.Rapunzel;} line. */
    private static final Pattern RAPUNZEL_IMPORT =
        Pattern.compile("^import\\s+de\\.t14d3\\.rapunzellib\\.Rapunzel\\s*;$", Pattern.MULTILINE);

    /** Matches any {@code Rapunzel.} reference (outside the import or a string literal). */
    private static final Pattern RAPUNZEL_REFERENCE =
        Pattern.compile("\\bRapunzel\\.", Pattern.MULTILINE);

    // -- Task inputs / outputs -------------------------------------------------------

    @Input
    public abstract Property<String> getPackageName();

    @Input
    public abstract Property<String> getClassName();

    /**
     * The directory containing the Java sources to scan and transform.
     * Typically {@code src/main/java}. When not set, only the wrapper
     * class is generated (no source transformation).
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    public abstract DirectoryProperty getSourceDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    // -- Task action -----------------------------------------------------------------

    @TaskAction
    public void generate() throws IOException {
        String pkg = getPackageName().get();
        String cls = getClassName().get();
        File outputRoot = getOutputDir().get().getAsFile();

        // 1. Generate the wrapper class
        writeWrapper(pkg, cls, outputRoot);

        // 2. Transform source files (copy with Rapunzel.* rewrites)
        if (getSourceDir().isPresent()) {
            File sourceDir = getSourceDir().get().getAsFile();
            if (sourceDir.exists()) {
                transformSourceFiles(pkg, cls, sourceDir, outputRoot);
            } else {
                getLogger().info("Source directory does not exist, skipping source transformation: {}", sourceDir);
            }
        } else {
            getLogger().debug("No source directory configured, skipping source transformation.");
        }
    }

    // -- Wrapper generation ----------------------------------------------------------

    private void writeWrapper(String pkg, String cls, File outputRoot) {
        String source = renderSource(pkg, cls);
        File targetFile = new File(outputRoot, pkg.replace('.', '/') + "/" + cls + ".java");
        File targetParent = targetFile.getParentFile();
        if (targetFile.exists()) {
            getProject().delete(targetFile);
        }
        if (!targetParent.exists() && !targetParent.mkdirs()) {
            throw new GradleException("Failed to create context wrapper output directory " + targetParent);
        }
        try {
            Files.writeString(targetFile.toPath(), source, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new GradleException("Failed to write generated context wrapper source.", ex);
        }
        getLogger().lifecycle("Generated context wrapper {} at {}", cls, getProject().relativePath(targetFile));
    }

    // -- Source transformation -------------------------------------------------------

    private void transformSourceFiles(String pkg, String cls, File sourceDir, File outputRoot) throws IOException {
        FileTree sourceFiles = getProject()
                .fileTree(sourceDir)
                .matching(spec -> spec.include("**/*.java"));

        String redirectImport = "import " + pkg + "." + cls + ";";
        int transformed = 0;
        int unchanged = 0;

        for (File sourceFile : sourceFiles) {
            String relativePath = sourceDir.toPath().relativize(sourceFile.toPath()).toString();
            File outputFile = new File(outputRoot, relativePath);
            File parent = outputFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new GradleException("Failed to create output directory " + parent);
            }

            String content = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);

            // Quick path: no Rapunzel reference at all -> copy unchanged
            if (!content.contains("Rapunzel")) {
                Files.copy(sourceFile.toPath(), outputFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                unchanged++;
                continue;
            }

            // Replace Rapunzel.method( -> cls.method( for redirectable methods
            String rewritten = RAPUNZEL_CALL.matcher(content).replaceAll(cls + ".${method}(");

            // Check whether any Rapunzel. references remain (after the rewrite)
            boolean hasRemainingRapunzel = RAPUNZEL_REFERENCE.matcher(rewritten).find();

            if (!hasRemainingRapunzel) {
                // All Rapunzel calls were redirected -> replace the import entirely
                rewritten = RAPUNZEL_IMPORT.matcher(rewritten)
                    .replaceFirst(Matcher.quoteReplacement(redirectImport));
            } else {
                // Some Rapunzel calls remain (e.g. shutdown) -> add the new import alongside
                if (!rewritten.contains(redirectImport)) {
                    rewritten = RAPUNZEL_IMPORT.matcher(rewritten)
                        .replaceFirst(m -> m.group() + "\n" + redirectImport);
                }
            }

            Files.writeString(outputFile.toPath(), rewritten, StandardCharsets.UTF_8);
            transformed++;
        }

        if (transformed > 0) {
            getLogger().lifecycle(
                "Transformed {} source files: rewrote Rapunzel.*() calls to {}.*()",
                transformed, cls);
        }
        if (unchanged > 0) {
            getLogger().debug("Copied {} source files unchanged (no Rapunzel.* calls)", unchanged);
        }
    }

    // -- Template --------------------------------------------------------------------

    private String renderSource(String pkg, String cls) {
        String template = """
            package $PACKAGE$;
            
            import de.t14d3.rapunzellib.PlatformId;
            import de.t14d3.rapunzellib.attachments.AttachmentSupport;
            import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
            import de.t14d3.rapunzellib.config.ConfigService;
            import de.t14d3.rapunzellib.context.RapunzelContext;
            import de.t14d3.rapunzellib.context.ResourceProvider;
            import de.t14d3.rapunzellib.context.ServiceRegistry;
            import de.t14d3.rapunzellib.message.MessageFormatService;
            import de.t14d3.rapunzellib.objects.Entities;
            import de.t14d3.rapunzellib.objects.Players;
            import de.t14d3.rapunzellib.objects.RNative;
            import de.t14d3.rapunzellib.objects.Worlds;
            import de.t14d3.rapunzellib.objects.block.Blocks;
            import de.t14d3.rapunzellib.objects.interop.RNativeInterop;
            import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
            import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
            import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
            import de.t14d3.rapunzellib.registry.RRegistryAccess;
            import de.t14d3.rapunzellib.runtime.LifecycleOwner;
            import de.t14d3.rapunzellib.runtime.PlatformRuntime;
            import de.t14d3.rapunzellib.runtime.RuntimeCapability;
            import de.t14d3.rapunzellib.scheduler.Scheduler;
            import org.jetbrains.annotations.NotNull;
            import org.slf4j.Logger;
            
            import java.nio.file.Path;
            import java.util.Optional;
            
            /**
             * Per-project static wrapper around a {@link RapunzelContext}.
             *
             * <p>Generated by RapunzelLib's Gradle plugin. Holds a direct reference to the
             * project's own context so that static convenience methods resolve without
             * going through the ambiguous {@code Rapunzel.context()} resolution.</p>
             *
             * <p>Usage after bootstrap:</p>
             * <pre>{@code
             * BootstrapHandle handle = PaperRapunzelBootstrap.acquire(this);
             * $CLASS$.init(handle.context());
             * }</pre>
             */
            public final class $CLASS$ {
            
                private static RapunzelContext context;
            
                private $CLASS$() {
                }
            
                /**
                 * Initialises the wrapper with the project's context.
                 * Must be called once after bootstrap, before any other static method.
                 */
                public static void init(@NotNull RapunzelContext ctx) {
                    context = ctx;
                }
            
                /** Returns the raw context, for access to methods not exposed here. */
                public static @NotNull RapunzelContext context() {
                    return context;
                }
            
                // -- Accessors ------------------------------------------------------------------
            
                public static @NotNull PlatformRuntime runtime() {
                    return context.runtime();
                }
            
                public static @NotNull PlatformId platformId() {
                    return context.platformId();
                }
            
                public static @NotNull Logger logger() {
                    return context.logger();
                }
            
                public static @NotNull Path dataDirectory() {
                    return context.dataDirectory();
                }
            
                public static @NotNull ResourceProvider resources() {
                    return context.resources();
                }
            
                public static @NotNull Scheduler scheduler() {
                    return context.scheduler();
                }
            
                public static @NotNull ServiceRegistry services() {
                    return context.services();
                }
            
                public static @NotNull RRegistryAccess registries() {
                    return context.registries();
                }
            
                public static @NotNull ConfigService configs() {
                    return context.configs();
                }
            
                public static @NotNull MessageFormatService messages() {
                    return context.messages();
                }
            
                public static @NotNull Players players() {
                    return context.players();
                }
            
                public static @NotNull Entities entities() {
                    return context.entities();
                }
            
                public static @NotNull REntityTypeRegistry entityTypes() {
                    return context.entityTypes();
                }
            
                public static @NotNull RItemTypeRegistry itemTypes() {
                    return context.itemTypes();
                }
            
                public static @NotNull RBlockTypeRegistry blockTypes() {
                    return context.blockTypes();
                }
            
                public static @NotNull Worlds worlds() {
                    return context.worlds();
                }
            
                public static @NotNull Blocks blocks() {
                    return context.blocks();
                }
            
                public static @NotNull AttachmentSupport attachments() {
                    return context.attachments();
                }
            
                public static @NotNull Optional<RNativeInterop> nativeInterop() {
                    return context.nativeInterop();
                }
            
                public static @NotNull LifecycleOwner owner() {
                    return context.owner();
                }
            
                // -- Parameterised accessors ---------------------------------------------------
            
                public static boolean supports(@NotNull RuntimeCapability capability) {
                    return context.supports(capability);
                }
            
                public static boolean supportsAttachments(@NotNull RNative target) {
                    return context.supportsAttachments(target);
                }
            
                @SuppressWarnings("unchecked")
                public static <T extends RNative> @NotNull T requireAttachmentSupport(@NotNull T target) {
                    return context.requireAttachmentSupport(target);
                }
            
                public static @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
                    return context.attachments(target);
                }
            
                // -- Lifecycle -----------------------------------------------------------------
            
                public static void dispatchCommand(@NotNull String command) {
                    context.dispatchCommand(command);
                }
            
                /**
                 * Closes the underlying context. After calling this the wrapper is
                 * no longer usable and must be re-initialised.
                 */
                public static void close() throws Exception {
                    context.close();
                }
            }
            """;

        return template.replace("$PACKAGE$", pkg).replace("$CLASS$", cls);
    }
}
