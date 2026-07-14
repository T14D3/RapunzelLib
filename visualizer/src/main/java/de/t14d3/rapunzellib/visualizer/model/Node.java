package de.t14d3.rapunzellib.visualizer.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single declaration or structural element in the codebase graph.
 *
 * <p>Identity is by {@link #id} - a stable synthetic key produced by the
 * collector. All other fields are descriptive and may be empty when not
 * applicable (e.g. a {@code Module} node has no source file).
 *
 * <p>Free-form attributes go into {@link #properties} so the model can carry
 * future metadata (modifiers, source location, statistics) without changing
 * the renderer contract.
 */
public final class Node {
    private final String id;
    private final NodeType type;
    private String simpleName;
    private String qualifiedName;
    private String containingModule;
    private String containingPackage;
    private String sourceFile;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    public Node(String id, NodeType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getContainingModule() {
        return containingModule;
    }

    public void setContainingModule(String containingModule) {
        this.containingModule = containingModule;
    }

    public String getContainingPackage() {
        return containingPackage;
    }

    public void setContainingPackage(String containingPackage) {
        this.containingPackage = containingPackage;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void putProperty(String key, Object value) {
        properties.put(key, value);
    }
}
