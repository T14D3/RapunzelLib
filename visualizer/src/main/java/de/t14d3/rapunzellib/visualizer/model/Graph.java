package de.t14d3.rapunzellib.visualizer.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The complete codebase graph: nodes keyed by id, plus all edges.
 *
 * <p>The graph is built incrementally by collectors and is the single source
 * of truth handed to the renderer. It is intentionally not optimised for
 * traversal - the renderer builds its own indices from the JSON form.
 *
 * <p>The {@code sourceFiles} and {@code compileClasspath} maps are a transient
 * side-channel used to hand source roots from {@code GradleModelCollector} to
 * the Java source collector. They are not serialised by the renderer.
 */
public final class Graph {
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Set<String> edgeKeys = new HashSet<>();
    private final Map<String, List<File>> sourceFiles = new LinkedHashMap<>();
    private final Map<String, Collection<File>> compileClasspath = new LinkedHashMap<>();

    public Node addNode(Node node) {
        Node existing = nodes.get(node.getId());
        if (existing != null) {
            return existing;
        }
        nodes.put(node.getId(), node);
        return node;
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public boolean hasNode(String id) {
        return nodes.containsKey(id);
    }

    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public int nodeCount() {
        return nodes.size();
    }

    public void addEdge(Edge edge) {
        String key = edge.getSource() + "|" + edge.getTarget() + "|" + edge.getType().jsonName();
        if (edgeKeys.add(key)) {
            edges.add(edge);
        }
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public int edgeCount() {
        return edges.size();
    }

    public void putSourceFiles(String sourceSetId, List<File> files) {
        sourceFiles.put(sourceSetId, files);
    }

    public List<File> getSourceFiles(String sourceSetId) {
        return sourceFiles.get(sourceSetId);
    }

    public Map<String, List<File>> allSourceFiles() {
        return Collections.unmodifiableMap(sourceFiles);
    }

    public void putCompileClasspath(String sourceSetId, Collection<File> files) {
        compileClasspath.put(sourceSetId, files);
    }

    public Collection<File> getCompileClasspath(String sourceSetId) {
        return compileClasspath.get(sourceSetId);
    }
}
