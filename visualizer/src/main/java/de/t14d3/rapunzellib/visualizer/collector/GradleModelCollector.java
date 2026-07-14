package de.t14d3.rapunzellib.visualizer.collector;

import de.t14d3.rapunzellib.visualizer.model.Edge;
import de.t14d3.rapunzellib.visualizer.model.EdgeType;
import de.t14d3.rapunzellib.visualizer.model.Graph;
import de.t14d3.rapunzellib.visualizer.model.Node;
import de.t14d3.rapunzellib.visualizer.model.NodeType;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks the Gradle project model and emits the structural portion of the
 * graph: the root project, each subproject (Module), each source set, and
 * project-to-project dependencies.
 *
 * <p>This collector deliberately does not look at Java source - that is the
 * job of {@link SourceCollector}. Keeping the two concerns separate means the
 * Gradle structure can be rendered even for projects whose source fails to
 * parse, and the Java collector can be swapped without touching this class.
 */
public final class GradleModelCollector {
    private final Project rootProject;
    private final boolean includeTestSources;

    public GradleModelCollector(Project rootProject) {
        this(rootProject, true);
    }

    public GradleModelCollector(Project rootProject, boolean includeTestSources) {
        this.rootProject = rootProject;
        this.includeTestSources = includeTestSources;
    }

    public void collect(Graph graph) {
        Node projectNode = new Node(projectId(rootProject), NodeType.PROJECT);
        projectNode.setSimpleName(rootProject.getName());
        projectNode.setQualifiedName(rootProject.getName());
        projectNode.setSourceFile(rootProject.getRootDir().getAbsolutePath());
        graph.addNode(projectNode);

        collectModule(graph, projectNode, rootProject);
        for (Project sub : sortedSubprojects(rootProject)) {
            collectModule(graph, projectNode, sub);
        }
    }

    private void collectModule(Graph graph, Node projectNode, Project module) {
        SourceSetContainer sourceSets = module.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets == null || sourceSets.isEmpty()) {
            // Try by name (handles classloader edge cases with included builds)
            Object ext = module.getExtensions().findByName("sourceSets");
            if (ext instanceof SourceSetContainer) {
                sourceSets = (SourceSetContainer) ext;
            } else {
                return;
            }
        }

        Node moduleNode = new Node(moduleId(module), NodeType.MODULE);
        moduleNode.setSimpleName(module.getName());
        moduleNode.setQualifiedName(module.getPath());
        moduleNode.setSourceFile(module.getProjectDir().getAbsolutePath());
        graph.addNode(moduleNode);
        graph.addEdge(new Edge(projectNode.getId(), moduleNode.getId(), EdgeType.CONTAINS));

        for (SourceSet sourceSet : sourceSets) {
            collectSourceSet(graph, moduleNode, module, sourceSet);
        }

        collectProjectDependencies(graph, module, moduleNode);
    }

    private void collectSourceSet(Graph graph, Node moduleNode, Project module, SourceSet sourceSet) {
        if (!includeTestSources && SourceSet.TEST_SOURCE_SET_NAME.equals(sourceSet.getName())) {
            return;
        }

        String ssId = sourceSetId(module, sourceSet);
        Node ssNode = new Node(ssId, NodeType.SOURCE_SET);
        ssNode.setSimpleName(sourceSet.getName());
        ssNode.setQualifiedName(module.getPath() + ":" + sourceSet.getName());
        ssNode.setContainingModule(module.getName());
        graph.addNode(ssNode);
        graph.addEdge(new Edge(moduleNode.getId(), ssNode.getId(), EdgeType.CONTAINS));

        // Use the JavaCompile task's configured source rather than sourceSet.getAllJava().
        // This ensures we pick up preprocessed/generated sources that other plugins
        // (e.g. the multi-version preprocessor) have wired into the compile task.
        List<File> files = new ArrayList<>();
        org.gradle.api.tasks.compile.JavaCompile compileTask =
            (org.gradle.api.tasks.compile.JavaCompile) module.getTasks().findByName(sourceSet.getCompileJavaTaskName());
        if (compileTask != null) {
            for (File f : compileTask.getSource().getFiles()) {
                if (f.isFile() && f.getName().endsWith(".java")) {
                    files.add(f);
                }
            }
        }
        // Fallback: if the compile task source is empty (e.g. task not yet configured),
        // use sourceSet.getAllJava() as a best-effort.
        if (files.isEmpty()) {
            FileTree javaFiles = sourceSet.getAllJava();
            if (javaFiles != null) {
                for (File f : javaFiles) {
                    if (f.isFile() && f.getName().endsWith(".java")) {
                        files.add(f);
                    }
                }
            }
        }

        graph.putSourceFiles(ssId, files);
        graph.putCompileClasspath(ssId, sourceSet.getCompileClasspath().getFiles());
    }

    private void collectProjectDependencies(Graph graph, Project module, Node moduleNode) {
        Set<String> addedDeps = new HashSet<>();
        for (var config : module.getConfigurations()) {
            for (var dep : config.getDependencies()) {
                if (dep instanceof ProjectDependency pd) {
                    String depPath = pd.getPath();
                    if (depPath == null) continue;
                    Project target = rootProject.findProject(depPath);
                    if (target != null && !target.equals(module)) {
                        String targetId = moduleId(target);
                        if (addedDeps.add(targetId)) {
                            graph.addEdge(new Edge(moduleNode.getId(), targetId, EdgeType.DEPENDS_ON));
                        }
                    }
                }
            }
        }
    }

    private static List<Project> sortedSubprojects(Project root) {
        List<Project> list = new ArrayList<>(root.getSubprojects());
        list.sort((a, b) -> a.getPath().compareTo(b.getPath()));
        return list;
    }

    public static String projectId(Project p) {
        return "project:" + p.getRootProject().getName();
    }

    public static String moduleId(Project p) {
        return "module:" + p.getPath();
    }

    public static String sourceSetId(Project p, SourceSet ss) {
        return "sourceSet:" + p.getPath() + ":" + ss.getName();
    }
}
