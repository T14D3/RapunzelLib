package de.t14d3.rapunzellib.visualizer.model;

/**
 * Kinds of nodes that can appear in the codebase graph.
 *
 * <p>The set is intentionally closed over the spec's hierarchy but the
 * {@link #jsonName} of each entry is the wire format used by the renderer,
 * so future additions only need a new constant here - no renderer change
 * required as long as the UI treats unknown types gracefully.
 */
public enum NodeType {
    PROJECT("project"),
    MODULE("module"),
    SOURCE_SET("sourceSet"),
    PACKAGE("package"),
    CLASS("class"),
    INTERFACE("interface"),
    ENUM("enum"),
    RECORD("record"),
    ANNOTATION("annotation"),
    METHOD("method"),
    CONSTRUCTOR("constructor"),
    FIELD("field");

    private final String jsonName;

    NodeType(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }

    public static NodeType fromJsonName(String name) {
        for (NodeType t : values()) {
            if (t.jsonName.equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + name);
    }
}
