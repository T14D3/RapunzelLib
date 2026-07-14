package de.t14d3.rapunzellib.visualizer.model;

/**
 * Kinds of directed relationships between two nodes.
 *
 * <p>Each edge has a {@link #group} which the UI uses to toggle whole
 * relationship layers on and off without regenerating the report. Adding a
 * new relationship type only requires a new constant here plus emitting it
 * from the collector - the renderer picks it up automatically.
 */
public enum EdgeType {
    CONTAINS("contains", "structure"),
    EXTENDS("extends", "inheritance"),
    IMPLEMENTS("implements", "inheritance"),
    CALLS("calls", "calls"),
    REFERENCES("references", "references"),
    USES("uses", "references"),
    RETURNS("returns", "references"),
    THROWS("throws", "references"),
    CREATES("creates", "references"),
    ANNOTATED_BY("annotatedBy", "annotations"),
    DEPENDS_ON("dependsOn", "gradle");

    private final String jsonName;
    private final String group;

    EdgeType(String jsonName, String group) {
        this.jsonName = jsonName;
        this.group = group;
    }

    public String jsonName() {
        return jsonName;
    }

    public String group() {
        return group;
    }

    public static EdgeType fromJsonName(String name) {
        for (EdgeType t : values()) {
            if (t.jsonName.equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown edge type: " + name);
    }
}
