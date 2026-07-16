/* =====================================================================
 * Codebase Visualizer - binary format decoder
 *
 * Decodes the compact binary payload produced by BinaryGraphWriter.java
 * (base64-embedded in graph-data.js as window.__GRAPH_BIN__) back into the
 * { nodes: [...], edges: [...] } shape consumed by app.indices.js.
 *
 * The decoder reconstructs the exact same JS objects the old JSON path
 * produced, so the rest of the application is unchanged:
 *   node = { id, type, simpleName, qualifiedName, containingModule,
 *            containingPackage, sourceFile, properties }
 *   edge = { source, target, type }
 *
 * Format (all integers little-endian, unsigned):
 *   magic "RVBF" + version u8
 *   stringTable: u32 count + (u16 len + utf8)*
 *   nodeTypes:   u8 count + (u8 len + utf8)*
 *   edgeTypes:   u8 count + (u8 len + utf8)*
 *   edgeGroups:  u8 count + (u8 len + utf8)*
 *   nodes: u32 count + columnar arrays + properties
 *   edges: u32 count + u32[] source + u32[] target + u8[] type
 * ===================================================================== */
(function (RV) {
    'use strict';

    function decodeBase64(b64) {
        // Use atob for the decode, then pack into a Uint8Array.
        var bin = atob(b64);
        var len = bin.length;
        var bytes = new Uint8Array(len);
        for (var i = 0; i < len; i++) bytes[i] = bin.charCodeAt(i);
        return bytes;
    }

    function Decoder(bytes) {
        this.bytes = bytes;
        this.pos = 0;
    }

    Decoder.prototype.u8 = function () {
        return this.bytes[this.pos++];
    };

    Decoder.prototype.u16 = function () {
        var b = this.bytes, p = this.pos;
        this.pos = p + 2;
        return b[p] | (b[p + 1] << 8);
    };

    Decoder.prototype.u32 = function () {
        var b = this.bytes, p = this.pos;
        this.pos = p + 4;
        return (b[p] | (b[p + 1] << 8) | (b[p + 2] << 16) | (b[p + 3] << 24)) >>> 0;
    };

    Decoder.prototype.f64 = function () {
        var b = this.bytes, p = this.pos;
        this.pos = p + 8;
        // Reconstruct via DataView for correct endianness.
        if (!this._dv) this._dv = new DataView(this.bytes.buffer);
        return this._dv.getFloat64(p, true);
    };

    Decoder.prototype.utf8 = function (len) {
        var start = this.pos;
        this.pos += len;
        // TextDecoder is universally available in modern browsers.
        if (!Decoder._td) Decoder._td = new TextDecoder('utf-8');
        return Decoder._td.decode(this.bytes.subarray(start, start + len));
    };

    Decoder.prototype.skip = function (n) { this.pos += n; };

    function readStringTable(dec) {
        var count = dec.u32();
        // Index 0 is reserved for null/absent; the writer starts real strings at 1.
        var table = new Array(count + 1);
        table[0] = null;
        for (var i = 1; i <= count; i++) {
            var len = dec.u16();
            table[i] = dec.utf8(len);
        }
        return table;
    }

    function readEnumNames(dec) {
        var count = dec.u8();
        var arr = new Array(count);
        for (var i = 0; i < count; i++) {
            var len = dec.u8();
            arr[i] = dec.utf8(len);
        }
        return arr;
    }

    function decode(bytes) {
        var dec = new Decoder(bytes);

        // Magic + version.
        if (dec.u8() !== 0x52 || dec.u8() !== 0x56 || dec.u8() !== 0x42 || dec.u8() !== 0x46) {
            throw new Error('Bad magic in graph binary');
        }
        var version = dec.u8();
        if (version !== 1) throw new Error('Unsupported graph binary version: ' + version);

        var strings = readStringTable(dec);
        var nodeTypes = readEnumNames(dec);
        var edgeTypes = readEnumNames(dec);
        var edgeGroups = readEnumNames(dec);

        // ---- Nodes ----
        var nodeCount = dec.u32();
        var nodes = new Array(nodeCount);

        var idIdx = readU32Array(dec, nodeCount);
        var typeIdx = readU8Array(dec, nodeCount);
        var simpleIdx = readU32Array(dec, nodeCount);
        var qnameIdx = readU32Array(dec, nodeCount);
        var modIdx = readU32Array(dec, nodeCount);
        var pkgIdx = readU32Array(dec, nodeCount);
        var srcIdx = readU32Array(dec, nodeCount);

        // Property counts per node.
        var propCounts = new Array(nodeCount);
        for (var i = 0; i < nodeCount; i++) propCounts[i] = dec.u16();

        // Build node objects.
        for (var n = 0; n < nodeCount; n++) {
            nodes[n] = {
                id: strings[idIdx[n]],
                type: nodeTypes[typeIdx[n]],
                simpleName: strings[simpleIdx[n]] || undefined,
                qualifiedName: strings[qnameIdx[n]] || undefined,
                containingModule: strings[modIdx[n]] || undefined,
                containingPackage: strings[pkgIdx[n]] || undefined,
                sourceFile: strings[srcIdx[n]] || undefined,
                properties: {}
            };
        }

        // Read properties.
        for (var p = 0; p < nodeCount; p++) {
            var pc = propCounts[p];
            if (pc === 0) continue;
            var props = nodes[p].properties;
            for (var k = 0; k < pc; k++) {
                var keyIdx = dec.u16();
                var vt = dec.u8();
                var key = strings[keyIdx];
                if (vt === 0) { // null
                    props[key] = null;
                } else if (vt === 1) { // string
                    props[key] = strings[dec.u32()] || '';
                } else if (vt === 2) { // number
                    props[key] = dec.f64();
                } else if (vt === 3) { // bool
                    props[key] = dec.u8() !== 0;
                } else if (vt === 4) { // string array
                    var alen = dec.u16();
                    var arr = new Array(alen);
                    for (var a = 0; a < alen; a++) arr[a] = strings[dec.u32()] || '';
                    props[key] = arr;
                } else {
                    throw new Error('Unknown property value type: ' + vt);
                }
            }
        }

        // ---- Edges ----
        var edgeCount = dec.u32();
        var edges = new Array(edgeCount);
        var eSrc = readU32Array(dec, edgeCount);
        var eTgt = readU32Array(dec, edgeCount);
        var eTypeRaw = readU8Array(dec, edgeCount);
        for (var e = 0; e < edgeCount; e++) {
            edges[e] = {
                source: nodes[eSrc[e]].id,
                target: nodes[eTgt[e]].id,
                type: edgeTypes[eTypeRaw[e]]
            };
        }

        return {
            nodes: nodes,
            edges: edges,
            _meta: {
                nodeTypes: nodeTypes,
                edgeTypes: edgeTypes,
                edgeGroups: edgeGroups
            }
        };
    }

    function readU32Array(dec, count) {
        // We cannot use a Uint32Array view directly because dec.pos may not be
        // 4-byte aligned (the string table and enums leave it at arbitrary
        // offsets). Copy into a fresh, aligned buffer instead.
        var sub = new Uint8Array(dec.bytes.buffer, dec.pos, count * 4);
        var out = new Uint32Array(count);
        var dv = new DataView(sub.buffer, sub.byteOffset, sub.byteLength);
        for (var i = 0; i < count; i++) {
            out[i] = dv.getUint32(i * 4, true);
        }
        dec.pos += count * 4;
        return out;
    }

    function readU8Array(dec, count) {
        var view = new Uint8Array(dec.bytes.buffer, dec.pos, count);
        dec.pos += count;
        return view;
    }

    /**
     * Load the graph from the embedded binary blob, falling back to the
     * legacy JSON form (window.__GRAPH_DATA__) or graph.json fetch.
     * Returns a Promise resolving to the decoded graph object.
     */
    function loadGraph() {
        console.log('[RV] loadGraph: __GRAPH_BIN__ exists?', !!window.__GRAPH_BIN__,
            window.__GRAPH_BIN__ ? ('len=' + window.__GRAPH_BIN__.length) : '');
        if (window.__GRAPH_BIN__) {
            try {
                console.log('[RV] Decoding binary graph...');
                var bytes = decodeBase64(window.__GRAPH_BIN__);
                console.log('[RV] Base64 decoded, bytes=' + bytes.length);
                var graph = decode(bytes);
                console.log('[RV] Decode OK: nodes=' + graph.nodes.length + ' edges=' + graph.edges.length);
                window.__GRAPH_BIN__ = null;
                return Promise.resolve(graph);
            } catch (e) {
                console.error('[RV] Binary graph decode FAILED:', e.message, e.stack);
            }
        }
        console.log('[RV] __GRAPH_DATA__ exists?', !!window.__GRAPH_DATA__);
        if (window.__GRAPH_DATA__) {
            console.log('[RV] Using inline JSON data');
            return Promise.resolve(window.__GRAPH_DATA__);
        }
        console.log('[RV] Fetching graph.json...');
        return fetch('graph.json').then(function (r) {
            console.log('[RV] graph.json response status:', r.status, r.ok);
            if (!r.ok) throw new Error('HTTP ' + r.status);
            return r.json();
        }).then(function (data) {
            console.log('[RV] graph.json parsed: nodes=' + data.nodes.length);
            return data;
        }).catch(function (e) {
            console.error('[RV] graph.json fetch/parse FAILED:', e.message);
            throw e;
        });
    }

    RV.decodeGraph = decode;
    RV.loadGraph = loadGraph;
})(window.RV = window.RV || {});
