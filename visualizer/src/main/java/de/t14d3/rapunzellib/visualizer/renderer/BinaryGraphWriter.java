package de.t14d3.rapunzellib.visualizer.renderer;

import de.t14d3.rapunzellib.visualizer.model.Edge;
import de.t14d3.rapunzellib.visualizer.model.EdgeType;
import de.t14d3.rapunzellib.visualizer.model.Graph;
import de.t14d3.rapunzellib.visualizer.model.Node;
import de.t14d3.rapunzellib.visualizer.model.NodeType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialises a {@link Graph} to a compact binary representation consumed by
 * the WebGL2 renderer.
 *
 * <p>The binary format eliminates the massive string duplication that bloats
 * the JSON form: node ids (which average ~90 chars and can exceed 600) are
 * interned into a string table and referenced by 32-bit indices; edges store
 * node indices and a one-byte edge-type enum instead of repeating full ids
 * and type names; repeated descriptive fields (containingModule,
 * containingPackage, sourceFile) are likewise interned. Pretty-printing
 * whitespace is eliminated entirely.
 *
 * <p>For a ~19k-node / ~66k-edge graph this typically reduces the payload
 * from ~27 MB (pretty JSON) to a few hundred KB - a 30-50x improvement -
 * while preserving every field the renderer needs.
 *
 * <p>Layout (all integers little-endian, unsigned):
 * <pre>
 *   magic            : 4 bytes  "RVBF"
 *   version          : 1 byte   (currently 1)
 *   stringTableCount : u32
 *   stringTable      : for each: u16 length + UTF-8 bytes
 *   nodeTypeCount    : u8
 *   nodeTypes        : for each: u8 length + UTF-8 bytes (jsonName)
 *   edgeTypeCount    : u8
 *   edgeTypes        : for each: u8 length + UTF-8 bytes (jsonName)
 *   edgeGroupCount   : u8
 *   edgeGroups       : for each: u8 length + UTF-8 bytes (group name)
 *   nodeCount        : u32
 *   nodeId           : u32[nodeCount]   (string-table index)
 *   nodeType         : u8[nodeCount]     (nodeTypes enum index)
 *   nodeSimpleName   : u32[nodeCount]   (0 = absent)
 *   nodeQualifiedName: u32[nodeCount]
 *   nodeModule       : u32[nodeCount]
 *   nodePackage      : u32[nodeCount]
 *   nodeSourceFile   : u32[nodeCount]
 *   nodePropCount    : u16[nodeCount]
 *   nodeProps        : for each node with props: u16 keyIdx + u8 valueType + value
 *                      valueType: 0=null,1=string(u32 idx),2=number(f64),3=bool(u8),
 *                      4=string-array(u16 count + u32[] idx)
 *   edgeCount        : u32
 *   edgeSource       : u32[edgeCount]   (node index)
 *   edgeTarget       : u32[edgeCount]   (node index)
 *   edgeType         : u8[edgeCount]     (edgeTypes enum index)
 * </pre>
 *
 * <p>The binary blob is base64-encoded and wrapped as
 * {@code window.__GRAPH_BIN__ = "<base64>";} so it loads under the
 * {@code file://} protocol (no fetch/CORS) and decodes via a small JS
 * routine in {@code app.decode.js}.
 */
public final class BinaryGraphWriter {

    private static final byte[] MAGIC = {'R', 'V', 'B', 'F'};
    private static final int VERSION = 1;

    /** Sentinel string-table index meaning "no value" (0 is reserved). */
    private static final int NULL_STR = 0;

    private final StringTable strings = new StringTable();
    private final Map<String, Integer> nodeTypeEnum = new HashMap<>();
    private final Map<String, Integer> edgeTypeEnum = new HashMap<>();
    private final Map<String, Integer> edgeGroupEnum = new HashMap<>();

    public BinaryGraphWriter() {
        // Pre-register enums in declaration order so indices are stable.
        for (NodeType t : NodeType.values()) {
            nodeTypeEnum.put(t.jsonName(), nodeTypeEnum.size());
        }
        for (EdgeType t : EdgeType.values()) {
            edgeTypeEnum.put(t.jsonName(), edgeTypeEnum.size());
            edgeGroupEnum.put(t.group(), edgeGroupEnum.getOrDefault(t.group(), edgeGroupEnum.size()));
        }
    }

    /**
     * @return the compact binary payload as raw bytes.
     */
    public byte[] writeBytes(Graph graph) {
        // First pass: intern all strings and assign node indices.
        int nodeCount = graph.nodeCount();
        int[] idIdx = new int[nodeCount];
        int[] typeIdx = new int[nodeCount];
        int[] simpleIdx = new int[nodeCount];
        int[] qnameIdx = new int[nodeCount];
        int[] modIdx = new int[nodeCount];
        int[] pkgIdx = new int[nodeCount];
        int[] srcIdx = new int[nodeCount];

        // Property encoding is deferred; collect per-node prop lists.
        List<List<PropEntry>> nodeProps = new ArrayList<>(nodeCount);

        int i = 0;
        for (Node node : graph.getNodes()) {
            idIdx[i] = strings.intern(node.getId());
            typeIdx[i] = nodeTypeEnum.get(node.getType().jsonName());
            simpleIdx[i] = internNullable(node.getSimpleName());
            qnameIdx[i] = internNullable(node.getQualifiedName());
            modIdx[i] = internNullable(node.getContainingModule());
            pkgIdx[i] = internNullable(node.getContainingPackage());
            srcIdx[i] = internNullable(node.getSourceFile());

            List<PropEntry> props = new ArrayList<>(node.getProperties().size());
            for (Map.Entry<String, Object> e : node.getProperties().entrySet()) {
                PropEntry pe = encodeProp(e.getKey(), e.getValue());
                if (pe != null) props.add(pe);
            }
            nodeProps.add(props);
            i++;
        }

        // Build a node-id -> node-index map for edge encoding.
        Map<String, Integer> nodeIndex = new LinkedHashMap<>(nodeCount * 2);
        i = 0;
        for (Node node : graph.getNodes()) {
            nodeIndex.put(node.getId(), i++);
        }

        int edgeCount = graph.edgeCount();
        int[] eSrc = new int[edgeCount];
        int[] eTgt = new int[edgeCount];
        byte[] eType = new byte[edgeCount];
        i = 0;
        for (Edge edge : graph.getEdges()) {
            Integer si = nodeIndex.get(edge.getSource());
            Integer ti = nodeIndex.get(edge.getTarget());
            // Skip edges whose endpoints vanished (shouldn't happen, but be safe).
            if (si == null || ti == null) continue;
            eSrc[i] = si;
            eTgt[i] = ti;
            eType[i] = (byte) (int) edgeTypeEnum.get(edge.getType().jsonName());
            i++;
        }
        int actualEdgeCount = i;

        // Now serialise.
        ByteArrayOutputStream out = new ByteArrayOutputStream(estimateSize(nodeCount, edgeCount));
        try {
            writeHeader(out);
            writeStringTable(out);
            writeEnums(out);
            writeNodes(out, nodeCount, idIdx, typeIdx, simpleIdx, qnameIdx, modIdx, pkgIdx, srcIdx, nodeProps);
            writeEdges(out, actualEdgeCount, eSrc, eTgt, eType);
        } catch (IOException e) {
            throw new RuntimeException(e); // ByteArrayOutputStream doesn't throw
        }
        return out.toByteArray();
    }

    /**
     * @return the binary payload base64-encoded, suitable for embedding in JS.
     */
    public String writeBase64(Graph graph) {
        return Base64.encode(writeBytes(graph));
    }

    /**
     * @return a JS snippet assigning the base64 payload to {@code window.__GRAPH_BIN__}.
     */
    public String writeJsEmbed(Graph graph) {
        return "window.__GRAPH_BIN__ = \"" + writeBase64(graph) + "\";\n";
    }

    // ---- encoding helpers -------------------------------------------------

    private int internNullable(String s) {
        return s == null ? NULL_STR : strings.intern(s);
    }

    private PropEntry encodeProp(String key, Object value) {
        int keyIdx = strings.intern(key);
        if (value == null) {
            return new PropEntry(keyIdx, VT_NULL, null);
        } else if (value instanceof String s) {
            return new PropEntry(keyIdx, VT_STRING, strings.intern(s));
        } else if (value instanceof Boolean b) {
            return new PropEntry(keyIdx, VT_BOOL, b);
        } else if (value instanceof Number n) {
            return new PropEntry(keyIdx, VT_NUMBER, n.doubleValue());
        } else if (value instanceof Collection<?> c) {
            List<Integer> idxs = new ArrayList<>(c.size());
            for (Object item : c) {
                idxs.add(strings.intern(item == null ? "" : String.valueOf(item)));
            }
            return new PropEntry(keyIdx, VT_STR_ARRAY, idxs);
        } else {
            return new PropEntry(keyIdx, VT_STRING, strings.intern(String.valueOf(value)));
        }
    }

    // ---- low-level writers ------------------------------------------------

    private void writeHeader(OutputStream out) throws IOException {
        out.write(MAGIC);
        out.write(VERSION);
    }

    private void writeStringTable(OutputStream out) throws IOException {
        List<String> table = strings.table;
        writeU32(out, table.size());
        for (String s : table) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            if (b.length > 0xFFFF) {
                // Should not happen for our ids, but guard anyway.
                throw new IOException("String too long for u16 length: " + b.length);
            }
            writeU16(out, b.length);
            out.write(b);
        }
    }

    private void writeEnums(OutputStream out) throws IOException {
        writeEnumNames(out, nodeTypeEnum);
        writeEnumNames(out, edgeTypeEnum);
        writeEnumNamesU8(out, edgeGroupEnum);
    }

    private void writeEnumNames(OutputStream out, Map<String, Integer> enumMap) throws IOException {
        // nodeTypes and edgeTypes: count is u8, names with u8 length prefix.
        String[] byIdx = new String[enumMap.size()];
        for (Map.Entry<String, Integer> e : enumMap.entrySet()) {
            byIdx[e.getValue()] = e.getKey();
        }
        out.write(byIdx.length);
        for (String name : byIdx) {
            byte[] b = name.getBytes(StandardCharsets.UTF_8);
            out.write(b.length);
            out.write(b);
        }
    }

    private void writeEnumNamesU8(OutputStream out, Map<String, Integer> enumMap) throws IOException {
        String[] byIdx = new String[enumMap.size()];
        for (Map.Entry<String, Integer> e : enumMap.entrySet()) {
            byIdx[e.getValue()] = e.getKey();
        }
        out.write(byIdx.length);
        for (String name : byIdx) {
            byte[] b = name.getBytes(StandardCharsets.UTF_8);
            out.write(b.length);
            out.write(b);
        }
    }

    private void writeNodes(OutputStream out, int count,
                            int[] idIdx, int[] typeIdx,
                            int[] simpleIdx, int[] qnameIdx,
                            int[] modIdx, int[] pkgIdx, int[] srcIdx,
                            List<List<PropEntry>> nodeProps) throws IOException {
        writeU32(out, count);
        writeU32Array(out, idIdx);
        writeU8Array(out, typeIdx);
        writeU32Array(out, simpleIdx);
        writeU32Array(out, qnameIdx);
        writeU32Array(out, modIdx);
        writeU32Array(out, pkgIdx);
        writeU32Array(out, srcIdx);

        // Property counts.
        for (List<PropEntry> props : nodeProps) {
            writeU16(out, props.size());
        }
        // Property entries.
        for (List<PropEntry> props : nodeProps) {
            for (PropEntry pe : props) {
                writeU16(out, pe.keyIdx);
                out.write(pe.valueType);
                switch (pe.valueType) {
                    case VT_NULL -> { /* no payload */ }
                    case VT_STRING -> writeU32(out, (int) pe.value);
                    case VT_BOOL -> out.write(((Boolean) pe.value) ? 1 : 0);
                    case VT_NUMBER -> writeF64(out, (Double) pe.value);
                    case VT_STR_ARRAY -> {
                        @SuppressWarnings("unchecked")
                        List<Integer> arr = (List<Integer>) pe.value;
                        writeU16(out, arr.size());
                        for (int idx : arr) writeU32(out, idx);
                    }
                    default -> throw new IOException("Unknown value type: " + pe.valueType);
                }
            }
        }
    }

    private void writeEdges(OutputStream out, int count, int[] src, int[] tgt, byte[] type) throws IOException {
        writeU32(out, count);
        writeU32Array(out, src, count);
        writeU32Array(out, tgt, count);
        out.write(type, 0, count);
    }

    // ---- primitive writers ------------------------------------------------

    private static void writeU16(OutputStream out, int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    private static void writeU32(OutputStream out, int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }

    private static void writeF64(OutputStream out, double v) throws IOException {
        long bits = Double.doubleToLongBits(v);
        for (int i = 0; i < 8; i++) {
            out.write((int) (bits >>> (i * 8)) & 0xFF);
        }
    }

    private static void writeU32Array(OutputStream out, int[] arr) throws IOException {
        writeU32Array(out, arr, arr.length);
    }

    private static void writeU32Array(OutputStream out, int[] arr, int len) throws IOException {
        byte[] buf = new byte[len * 4];
        for (int i = 0; i < len; i++) {
            int v = arr[i];
            int off = i * 4;
            buf[off] = (byte) (v & 0xFF);
            buf[off + 1] = (byte) ((v >>> 8) & 0xFF);
            buf[off + 2] = (byte) ((v >>> 16) & 0xFF);
            buf[off + 3] = (byte) ((v >>> 24) & 0xFF);
        }
        out.write(buf);
    }

    private static void writeU8Array(OutputStream out, int[] arr) throws IOException {
        byte[] buf = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            buf[i] = (byte) arr[i];
        }
        out.write(buf);
    }

    private static int estimateSize(int nodes, int edges) {
        // Rough upper bound: nodes * (7*4 + 2 + 40 props) + edges * 9 + strings.
        return nodes * 90 + edges * 9 + 65536;
    }

    // ---- property value type constants ------------------------------------

    private static final int VT_NULL = 0;
    private static final int VT_STRING = 1;
    private static final int VT_NUMBER = 2;
    private static final int VT_BOOL = 3;
    private static final int VT_STR_ARRAY = 4;

    private record PropEntry(int keyIdx, int valueType, Object value) {}

    // ---- string interning --------------------------------------------------

    /**
     * Compact string table. Index 0 is reserved for "null/absent" so the
     * first real string gets index 1.
     */
    private static final class StringTable {
        final List<String> table = new ArrayList<>();
        final Map<String, Integer> index = new HashMap<>();
        private int next = 1; // 0 reserved

        int intern(String s) {
            if (s == null) return NULL_STR;
            Integer existing = index.get(s);
            if (existing != null) return existing;
            int idx = next++;
            index.put(s, idx);
            table.add(s);
            return idx;
        }
    }

    // ---- minimal base64 encoder (avoids android/java.util.Base64 dependency) ----

    private static final class Base64 {
        private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

        static String encode(byte[] data) {
            int len = data.length;
            if (len == 0) return "";
            int groups = (len + 2) / 3;
            char[] out = new char[groups * 4];
            int o = 0;
            int i = 0;
            while (i + 3 <= len) {
                int b0 = data[i] & 0xFF;
                int b1 = data[i + 1] & 0xFF;
                int b2 = data[i + 2] & 0xFF;
                out[o] = ALPHABET[b0 >>> 2];
                out[o + 1] = ALPHABET[((b0 & 0x03) << 4) | (b1 >>> 4)];
                out[o + 2] = ALPHABET[((b1 & 0x0F) << 2) | (b2 >>> 6)];
                out[o + 3] = ALPHABET[b2 & 0x3F];
                i += 3;
                o += 4;
            }
            int rem = len - i;
            if (rem == 1) {
                int b0 = data[i] & 0xFF;
                out[o] = ALPHABET[b0 >>> 2];
                out[o + 1] = ALPHABET[(b0 & 0x03) << 4];
                out[o + 2] = '=';
                out[o + 3] = '=';
            } else if (rem == 2) {
                int b0 = data[i] & 0xFF;
                int b1 = data[i + 1] & 0xFF;
                out[o] = ALPHABET[b0 >>> 2];
                out[o + 1] = ALPHABET[((b0 & 0x03) << 4) | (b1 >>> 4)];
                out[o + 2] = ALPHABET[(b1 & 0x0F) << 2];
                out[o + 3] = '=';
            }
            return new String(out);
        }
    }
}
