package de.t14d3.rapunzellib.visualizer.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.t14d3.rapunzellib.visualizer.model.Edge;
import de.t14d3.rapunzellib.visualizer.model.Graph;
import de.t14d3.rapunzellib.visualizer.model.Node;

import java.util.Collection;
import java.util.Map;

/**
 * Serialises a {@link Graph} to a neutral JSON representation consumed by the
 * rendering layer.
 *
 * <p>The JSON schema is:
 * <pre>{@code
 * {
 *   "nodes": [
 *     {"id":"...","type":"...","simpleName":"...","qualifiedName":"...",
 *      "containingModule":"...","containingPackage":"...","sourceFile":"...",
 *      "properties":{...}}
 *   ],
 *   "edges": [
 *     {"source":"...","target":"...","type":"..."}
 *   ]
 * }
 * }</pre>
 *
 * <p>The schema is independent of the rendering library - the same JSON could
 * feed a Cytoscape.js, Sigma.js, or custom canvas renderer.
 */
public final class JsonWriter {
    private final Gson gson;

    public JsonWriter() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public String writeJson(Graph graph) {
        JsonObject root = new JsonObject();

        JsonArray nodes = new JsonArray();
        for (Node node : graph.getNodes()) {
            JsonObject n = new JsonObject();
            n.addProperty("id", node.getId());
            n.addProperty("type", node.getType().jsonName());
            addNullable(n, "simpleName", node.getSimpleName());
            addNullable(n, "qualifiedName", node.getQualifiedName());
            addNullable(n, "containingModule", node.getContainingModule());
            addNullable(n, "containingPackage", node.getContainingPackage());
            addNullable(n, "sourceFile", node.getSourceFile());
            n.add("properties", propertiesToJson(node.getProperties()));
            nodes.add(n);
        }
        root.add("nodes", nodes);

        JsonArray edges = new JsonArray();
        for (Edge edge : graph.getEdges()) {
            JsonObject e = new JsonObject();
            e.addProperty("source", edge.getSource());
            e.addProperty("target", edge.getTarget());
            e.addProperty("type", edge.getType().jsonName());
            edges.add(e);
        }
        root.add("edges", edges);

        return gson.toJson(root);
    }

    private static void addNullable(JsonObject obj, String key, String value) {
        if (value != null) {
            obj.addProperty(key, value);
        } else {
            obj.add(key, null);
        }
    }

    private JsonObject propertiesToJson(Map<String, Object> props) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Object> e : props.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                obj.add(e.getKey(), null);
            } else if (v instanceof String s) {
                obj.addProperty(e.getKey(), s);
            } else if (v instanceof Number n) {
                obj.addProperty(e.getKey(), n);
            } else if (v instanceof Boolean b) {
                obj.addProperty(e.getKey(), b);
            } else if (v instanceof Character c) {
                obj.addProperty(e.getKey(), c);
            } else if (v instanceof Collection<?> c) {
                JsonArray arr = new JsonArray();
                for (Object item : c) {
                    if (item instanceof String s) arr.add(s);
                    else if (item instanceof Number n) arr.add(n);
                    else if (item instanceof Boolean b) arr.add(b);
                    else arr.add(String.valueOf(item));
                }
                obj.add(e.getKey(), arr);
            } else {
                obj.addProperty(e.getKey(), String.valueOf(v));
            }
        }
        return obj;
    }
}
