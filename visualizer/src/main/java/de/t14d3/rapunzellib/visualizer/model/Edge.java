package de.t14d3.rapunzellib.visualizer.model;

/**
 * A directed relationship from one node to another.
 *
 * <p>Edges are identified by the {@code (source, target, type)} triple; the
 * collector must deduplicate so the same relationship is not emitted twice.
 */
public final class Edge {
    private final String source;
    private final String target;
    private final EdgeType type;

    public Edge(String source, String target, EdgeType type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public EdgeType getType() {
        return type;
    }
}
