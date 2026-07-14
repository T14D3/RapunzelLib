/* =====================================================================
 * Codebase Visualizer - WebGL2 renderer
 *
 * Replaces the canvas-2D renderer (app.render.js) for large graphs.
 * Nodes are drawn as instanced rounded-rect quads in a single draw call;
 * edges as instanced thick line segments in another. A small canvas-2D
 * overlay draws labels only for the (culled) visible nodes - typically a
 * few hundred - so text cost stays bounded regardless of graph size.
 *
 * Exposes the same RV.render / RV.computeBounds / RV.renderMinimap /
 * RV.minimapToWorld API as the old renderer, so app.js and app.interact.js
 * work unchanged.
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    // ---- Bounds (same semantics as the 2D renderer) -----------------------

    function computeBounds() {
        S = RV.state;
        var first = true;
        var minX = 0, maxX = 0, minY = 0, maxY = 0;
        for (var id in S.positions) {
            if (!S.positions.hasOwnProperty(id)) continue;
            var node = S.nodesById[id];
            if (!RV.isNodeVisible(node)) continue;
            var p = S.positions[id];
            if (first) { minX = maxX = p.x; minY = maxY = p.y; first = false; }
            else {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
        }
        S.bounds = {
            minX: minX - RV.NODE_W, maxX: maxX + RV.NODE_W,
            minY: minY - RV.NODE_H, maxY: maxY + RV.NODE_H
        };
    }

    // ---- WebGL state -------------------------------------------------------

    var gl = null;
    var nodeProg = null;
    var edgeProg = null;
    var nodeVao = null;
    var edgeVao = null;
    var nodeBuffers = null;
    var edgeBuffers = null;
    var overlayCanvas = null;
    var overlayCtx = null;

    // Per-instance node attributes (SoA layout for cache efficiency).
    var nodeInstanceData = null;   // Float32Array
    var edgeInstanceData = null;    // Float32Array
    var nodeInstanceCount = 0;
    var edgeInstanceCount = 0;

    // Cached node/edge index maps so we can rebuild instance buffers only
    // when the visible set changes (expand/collapse/filter), not every frame.
    var visibleNodeIds = null;
    var visibleEdgeIdxs = null;
    var dirty = true; // rebuild instance buffers on next render

    // ---- Shaders ----------------------------------------------------------

    var NODE_VS = [
        '#version 300 es',
        'precision highp float;',
        'uniform vec2 uHalfView;   // (cssW/2, cssH/2)',
        'uniform vec2 uCamera;',
        'uniform float uZoom;',
        'uniform float uDpr;',
        // Per-vertex quad corner in [-1,1] x [-1,1] (a quad built in the VS).
        'const vec2 corners[4] = vec2[4](vec2(-1.0,-1.0), vec2(1.0,-1.0), vec2(-1.0,1.0), vec2(1.0,1.0));',
        'in vec2 aCorner;          // 0..3',
        // Per-instance attributes.
        'in vec2 aCenter;          // world coords',
        'in vec2 aHalfSize;        // half width/height in world units',
        'in vec4 aColor;           // fill rgba 0..1',
        'in vec4 aBorderColor;     // border rgba 0..1',
        'in float aBorderWidth;    // in px',
        'in float aBright;         // focus brightness 0..1',
        'in float aRadius;         // corner radius in world units',
        'out vec2 vLocal;          // local coords in [-halfSize, +halfSize]',
        'out vec4 vFillColor;',
        'out vec4 vBorderColor;',
        'out float vBorderWidth;',
        'out float vBright;',
        'out float vRadius;',
        'out vec2 vHalfSize;',
        'void main() {',
        '  vec2 corner = corners[int(aCorner.x)];',
        '  vec2 local = corner * aHalfSize;',
        '  vec2 world = aCenter + local;',
        '  // world -> screen px: (world - camera) * zoom, then to NDC.',
        '  vec2 px = (world - uCamera) * uZoom;',
        '  vec2 ndc = px / uHalfView;',
        '  gl_Position = vec4(ndc, 0.0, 1.0);',
        '  vLocal = local;',
        '  vFillColor = aColor;',
        '  vBorderColor = aBorderColor;',
        '  vBorderWidth = aBorderWidth;',
        '  vBright = aBright;',
        '  vRadius = aRadius;',
        '  vHalfSize = aHalfSize;',
        '}'
    ].join('\n');

    var NODE_FS = [
        '#version 300 es',
        'precision highp float;',
        'uniform float uZoom;',
        'in vec2 vLocal;',
        'in vec4 vFillColor;',
        'in vec4 vBorderColor;',
        'in float vBorderWidth;',
        'in float vBright;',
        'in float vRadius;',
        'in vec2 vHalfSize;',
        'out vec4 fragColor;',
        '',
        // Signed-distance field for a rounded rectangle centered at origin
        // with half-extents `h` and corner radius `r`.
        'float sdRoundedBox(vec2 p, vec2 h, float r) {',
        '  vec2 q = abs(p) - h + vec2(r);',
        '  return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;',
        '}',
        'void main() {',
        '  float dist = sdRoundedBox(vLocal, vHalfSize, vRadius);',
        '  // Anti-alias over ~1px. Convert 1px to world units via 1/uZoom.',
        '  float aaWorld = 1.0 / max(0.05, uZoom);',
        '  // Fill: inside (dist < 0) with a 1px anti-aliased edge.',
        '  float fillAlpha = 1.0 - smoothstep(-aaWorld, aaWorld, dist);',
        '  // Border band of vBorderWidth px, centered on the edge (dist==0).',
        '  float bwWorld = vBorderWidth / max(0.05, uZoom);',
        '  float borderAlpha = (1.0 - smoothstep(-aaWorld, aaWorld, dist + bwWorld * 0.5))',
        '                     - (1.0 - smoothstep(-aaWorld, aaWorld, dist - bwWorld * 0.5));',
        '  borderAlpha = clamp(borderAlpha, 0.0, 1.0);',
        '  vec4 col = mix(vFillColor, vBorderColor, borderAlpha);',
        '  col.a *= fillAlpha * vBright;',
        '  if (col.a < 0.01) discard;',
        '  fragColor = col;',
        '}'
    ].join('\n');

    // Edge shader: instanced thick line segments with per-instance color.
    var EDGE_VS = [
        '#version 300 es',
        'precision highp float;',
        'uniform vec2 uHalfView;',
        'uniform vec2 uCamera;',
        'uniform float uZoom;',
        'in vec2 aCorner;          // quad corner 0..3',
        'in vec2 aSrc;             // world coords',
        'in vec2 aTgt;',
        'in vec4 aColor;',
        'in float aWidth;          // px',
        'in float aBright;',
        'out vec4 vColor;',
        'void main() {',
        '  vec2 d = aTgt - aSrc;',
        '  float len = length(d);',
        '  vec2 dir = len > 0.0001 ? d / len : vec2(1.0, 0.0);',
        '  vec2 nrm = vec2(-dir.y, dir.x);',
        '  // Build a quad along the segment: corners in [-len/2, +len/2] x [-w/2, +w/2].',
        '  vec2 corners[4] = vec2[4](vec2(-1.0,-1.0), vec2(1.0,-1.0), vec2(-1.0,1.0), vec2(1.0,1.0));',
        '  vec2 c = corners[int(aCorner.x)];',
        '  // Width in world units: px / zoom.',
        '  float wWorld = aWidth / max(0.05, uZoom);',
        '  vec2 local = vec2(c.x * len * 0.5, c.y * wWorld * 0.5);',
        '  // Rotate local by dir/nrm and translate to segment midpoint.',
        '  vec2 mid = (aSrc + aTgt) * 0.5;',
        '  vec2 world = mid + dir * local.x + nrm * local.y;',
        '  vec2 px = (world - uCamera) * uZoom;',
        '  vec2 ndc = px / uHalfView;',
        '  gl_Position = vec4(ndc, 0.0, 1.0);',
        '  vColor = vec4(aColor.rgb, aColor.a * aBright);',
        '}'
    ].join('\n');

    var EDGE_FS = [
        '#version 300 es',
        'precision highp float;',
        'in vec4 vColor;',
        'out vec4 fragColor;',
        'void main() {',
        '  if (vColor.a < 0.01) discard;',
        '  fragColor = vColor;',
        '}'
    ].join('\n');

    // ---- GLSL helpers -----------------------------------------------------

    function compileShader(type, src) {
        var sh = gl.createShader(type);
        gl.shaderSource(sh, src);
        gl.compileShader(sh);
        if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) {
            var log = gl.getShaderInfoLog(sh);
            gl.deleteShader(sh);
            throw new Error('Shader compile failed: ' + log);
        }
        return sh;
    }

    function linkProgram(vsSrc, fsSrc) {
        var vs = compileShader(gl.VERTEX_SHADER, vsSrc);
        var fs = compileShader(gl.FRAGMENT_SHADER, fsSrc);
        var prog = gl.createProgram();
        gl.attachShader(prog, vs);
        gl.attachShader(prog, fs);
        gl.linkProgram(prog);
        if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
            var log = gl.getProgramInfoLog(prog);
            gl.deleteProgram(prog);
            throw new Error('Program link failed: ' + log);
        }
        return prog;
    }

    function loc(prog, name) { return gl.getAttribLocation(prog, name); }
    function uloc(prog, name) { return gl.getUniformLocation(prog, name); }

    // ---- Color helpers ----------------------------------------------------

    function hexToRgb01(hex) {
        // "#rrggbb" -> [r,g,b] in 0..1
        var h = hex.replace('#', '');
        return [
            parseInt(h.substring(0, 2), 16) / 255,
            parseInt(h.substring(2, 4), 16) / 255,
            parseInt(h.substring(4, 6), 16) / 255
        ];
    }

    // ---- Setup ------------------------------------------------------------

    function setupGl() {
        S = RV.state;
        var canvas = S.canvas;
        // Try WebGL2; fall back to the 2D renderer if unavailable.
        gl = canvas.getContext('webgl2', { antialias: true, premultipliedAlpha: false });
        if (!gl) {
            RV._useFallbackRenderer = true;
            return false;
        }
        try {
            nodeProg = linkProgram(NODE_VS, NODE_FS);
            edgeProg = linkProgram(EDGE_VS, EDGE_FS);
        } catch (e) {
            console.error('WebGL2 program setup failed, falling back to 2D:', e);
            RV._useFallbackRenderer = true;
            gl = null;
            return false;
        }

        // Overlay canvas for labels (sits on top of the GL canvas).
        if (!overlayCanvas) {
            overlayCanvas = document.createElement('canvas');
            overlayCanvas.id = 'label-overlay';
            overlayCanvas.style.position = 'absolute';
            overlayCanvas.style.left = '0';
            overlayCanvas.style.top = '0';
            overlayCanvas.style.pointerEvents = 'none';
            canvas.parentElement.appendChild(overlayCanvas);
            overlayCtx = overlayCanvas.getContext('2d');
        }

        buildNodeGeometry();
        buildEdgeGeometry();
        return true;
    }

    // Quad corner index buffer (shared by nodes and edges).
    var cornerBuffer = null;

    function buildCornerBuffer() {
        if (cornerBuffer) return;
        cornerBuffer = gl.createBuffer();
        // Four corner indices (0,1,2,3) as floats, one per vertex of the quad.
        // We draw as TRIANGLE_STRIP with 4 vertices; the VS maps each index
        // to a 2D corner via the corners[] array.
        gl.bindBuffer(gl.ARRAY_BUFFER, cornerBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0, 1, 2, 3]), gl.STATIC_DRAW);
    }

    function buildNodeGeometry() {
        buildCornerBuffer();
        nodeVao = gl.createVertexArray();
        gl.bindVertexArray(nodeVao);

        // aCorner
        gl.bindBuffer(gl.ARRAY_BUFFER, cornerBuffer);
        var aCorner = loc(nodeProg, 'aCorner');
        gl.enableVertexAttribArray(aCorner);
        gl.vertexAttribPointer(aCorner, 1, gl.FLOAT, false, 0, 0);
        // Instanced: one per instance.
        gl.vertexAttribDivisor(aCorner, 0);

        // Per-instance buffers (filled on rebuildInstanceBuffers).
        nodeBuffers = {
            center: gl.createBuffer(),
            halfSize: gl.createBuffer(),
            color: gl.createBuffer(),
            borderColor: gl.createBuffer(),
            borderWidth: gl.createBuffer(),
            bright: gl.createBuffer(),
            radius: gl.createBuffer()
        };
        bindInstanceAttr(nodeProg, 'aCenter', nodeBuffers.center, 2);
        bindInstanceAttr(nodeProg, 'aHalfSize', nodeBuffers.halfSize, 2);
        bindInstanceAttr(nodeProg, 'aColor', nodeBuffers.color, 4);
        bindInstanceAttr(nodeProg, 'aBorderColor', nodeBuffers.borderColor, 4);
        bindInstanceAttr(nodeProg, 'aBorderWidth', nodeBuffers.borderWidth, 1);
        bindInstanceAttr(nodeProg, 'aBright', nodeBuffers.bright, 1);
        bindInstanceAttr(nodeProg, 'aRadius', nodeBuffers.radius, 1);

        gl.bindVertexArray(null);
    }

    function buildEdgeGeometry() {
        edgeVao = gl.createVertexArray();
        gl.bindVertexArray(edgeVao);

        gl.bindBuffer(gl.ARRAY_BUFFER, cornerBuffer);
        var aCorner = loc(edgeProg, 'aCorner');
        gl.enableVertexAttribArray(aCorner);
        gl.vertexAttribPointer(aCorner, 1, gl.FLOAT, false, 0, 0);
        gl.vertexAttribDivisor(aCorner, 0);

        edgeBuffers = {
            src: gl.createBuffer(),
            tgt: gl.createBuffer(),
            color: gl.createBuffer(),
            width: gl.createBuffer(),
            bright: gl.createBuffer()
        };
        bindInstanceAttr(edgeProg, 'aSrc', edgeBuffers.src, 2);
        bindInstanceAttr(edgeProg, 'aTgt', edgeBuffers.tgt, 2);
        bindInstanceAttr(edgeProg, 'aColor', edgeBuffers.color, 4);
        bindInstanceAttr(edgeProg, 'aWidth', edgeBuffers.width, 1);
        bindInstanceAttr(edgeProg, 'aBright', edgeBuffers.bright, 1);

        gl.bindVertexArray(null);
    }

    function bindInstanceAttr(prog, name, buf, size) {
        var l = loc(prog, name);
        if (l < 0) return;
        gl.bindBuffer(gl.ARRAY_BUFFER, buf);
        gl.enableVertexAttribArray(l);
        gl.vertexAttribPointer(l, size, gl.FLOAT, false, 0, 0);
        gl.vertexAttribDivisor(l, 1);
    }

    // ---- Instance buffer rebuild -----------------------------------------

    function rebuildInstanceBuffers() {
        S = RV.state;
        // Collect visible nodes (culled to viewport).
        var viewW = S.cssW / S.zoom;
        var viewH = S.cssH / S.zoom;
        var vl = S.camera.x - viewW / 2 - RV.NODE_W;
        var vr = S.camera.x + viewW / 2 + RV.NODE_W;
        var vt = S.camera.y - viewH / 2 - RV.NODE_H;
        var vb = S.camera.y + viewH / 2 + RV.NODE_H;

        // First pass: count visible nodes & edges.
        var nCount = 0;
        var ids = [];
        for (var id in S.positions) {
            if (!S.positions.hasOwnProperty(id)) continue;
            var node = S.nodesById[id];
            if (!RV.isNodeVisible(node)) continue;
            var p = S.positions[id];
            if (p.x < vl || p.x > vr || p.y < vt || p.y > vb) continue;
            ids.push(id);
            nCount++;
        }
        visibleNodeIds = ids;

        // Edges: contains + cross, culled.
        var eCount = 0;
        var edgeList = [];
        // Contains edges.
        for (var i = 0; i < S.graph.edges.length; i++) {
            var edge = S.graph.edges[i];
            if (edge.type !== 'contains') continue;
            if (!RV.isEdgeVisible(edge)) continue;
            var sp = S.positions[edge.source];
            var tp = S.positions[edge.target];
            if (!sp || !tp) continue;
            if (!edgeInView(sp, tp, vl, vr, vt, vb)) continue;
            edgeList.push(edge);
            eCount++;
        }
        // Cross edges.
        for (var j = 0; j < S.crossEdges.length; j++) {
            var ce = S.crossEdges[j];
            if (!RV.isEdgeVisible(ce)) continue;
            var csp = S.positions[ce.source];
            var ctp = S.positions[ce.target];
            if (!csp || !ctp) continue;
            if (!edgeInView(csp, ctp, vl, vr, vt, vb)) continue;
            edgeList.push(ce);
            eCount++;
        }
        visibleEdgeIdxs = edgeList;

        // Allocate SoA arrays.
        // Node: center(2) halfSize(2) color(4) borderColor(4) borderWidth(1) bright(1) radius(1) = 15 floats
        var nStride = 15;
        nodeInstanceData = new Float32Array(nCount * nStride);
        nodeInstanceCount = nCount;

        for (var k = 0; k < nCount; k++) {
            var nid = ids[k];
            var n = S.nodesById[nid];
            var pos = S.positions[nid];
            var selected = S.selected === nid;
            var hovered = S.hoveredNode === nid;
            var bright = RV.nodeBrightness(nid);
            var bgHex = RV.NODE_COLORS[n.type] || '#555555';
            var bg = hexToRgb01(bgHex);
            var borderHex = selected ? '#ffffff' : (hovered ? '#ffffff' : '#ffffff26');
            var bd = hexToRgb01(borderHex);
            var bw = selected ? 3 : (hovered ? 2 : 1);
            var off = k * nStride;
            nodeInstanceData[off + 0] = pos.x;
            nodeInstanceData[off + 1] = pos.y;
            nodeInstanceData[off + 2] = RV.NODE_W / 2;
            nodeInstanceData[off + 3] = RV.NODE_H / 2;
            nodeInstanceData[off + 4] = bg[0];
            nodeInstanceData[off + 5] = bg[1];
            nodeInstanceData[off + 6] = bg[2];
            nodeInstanceData[off + 7] = 1.0;
            nodeInstanceData[off + 8] = bd[0];
            nodeInstanceData[off + 9] = bd[1];
            nodeInstanceData[off + 10] = bd[2];
            nodeInstanceData[off + 11] = selected || hovered ? 1.0 : 0.15;
            nodeInstanceData[off + 12] = bw;
            nodeInstanceData[off + 13] = bright;
            nodeInstanceData[off + 14] = 6; // corner radius in world units
        }

        // Upload node buffers (interleaved SoA -> separate buffers).
        uploadInstanceBuffer(nodeBuffers.center, nodeInstanceData, nCount, 15, 0, 2);
        uploadInstanceBuffer(nodeBuffers.halfSize, nodeInstanceData, nCount, 15, 2, 2);
        uploadInstanceBuffer(nodeBuffers.color, nodeInstanceData, nCount, 15, 4, 4);
        uploadInstanceBuffer(nodeBuffers.borderColor, nodeInstanceData, nCount, 15, 8, 4);
        uploadInstanceBuffer(nodeBuffers.borderWidth, nodeInstanceData, nCount, 15, 12, 1);
        uploadInstanceBuffer(nodeBuffers.bright, nodeInstanceData, nCount, 15, 13, 1);
        uploadInstanceBuffer(nodeBuffers.radius, nodeInstanceData, nCount, 15, 14, 1);

        // Edge: src(2) tgt(2) color(4) width(1) bright(1) = 10 floats
        var eStride = 10;
        edgeInstanceData = new Float32Array(eCount * eStride);
        edgeInstanceCount = eCount;
        for (var m = 0; m < eCount; m++) {
            var ed = edgeList[m];
            var esp = S.positions[ed.source];
            var etp = S.positions[ed.target];
            var grp = RV.EDGE_GROUPS[ed.type];
            var baseAlpha = (S.layerAlpha[grp] || 0.5) * RV.edgeBrightness(ed);
            var isHovered = S.hoveredEdge && S.hoveredEdge.source === ed.source &&
                S.hoveredEdge.target === ed.target && S.hoveredEdge.type === ed.type;
            var eHex = RV.EDGE_COLORS[ed.type] || '#888888';
            var ec = hexToRgb01(eHex);
            var width = ed.type === 'contains' ? 1 : (isHovered ? 2.5 : 1.4);
            var alpha = isHovered ? Math.min(1, baseAlpha + 0.4) : baseAlpha;
            var off2 = m * eStride;
            edgeInstanceData[off2 + 0] = esp.x;
            edgeInstanceData[off2 + 1] = esp.y;
            edgeInstanceData[off2 + 2] = etp.x;
            edgeInstanceData[off2 + 3] = etp.y;
            edgeInstanceData[off2 + 4] = ec[0];
            edgeInstanceData[off2 + 5] = ec[1];
            edgeInstanceData[off2 + 6] = ec[2];
            edgeInstanceData[off2 + 7] = alpha;
            edgeInstanceData[off2 + 8] = width;
            edgeInstanceData[off2 + 9] = 1.0;
        }
        uploadInstanceBuffer(edgeBuffers.src, edgeInstanceData, eCount, 10, 0, 2);
        uploadInstanceBuffer(edgeBuffers.tgt, edgeInstanceData, eCount, 10, 2, 2);
        uploadInstanceBuffer(edgeBuffers.color, edgeInstanceData, eCount, 10, 4, 4);
        uploadInstanceBuffer(edgeBuffers.width, edgeInstanceData, eCount, 10, 8, 1);
        uploadInstanceBuffer(edgeBuffers.bright, edgeInstanceData, eCount, 10, 9, 1);

        dirty = false;
    }

    function uploadInstanceBuffer(buf, data, count, stride, offset, size) {
        // Extract a sub-view for this attribute.
        var sub = new Float32Array(count * size);
        for (var i = 0; i < count; i++) {
            for (var j = 0; j < size; j++) {
                sub[i * size + j] = data[i * stride + offset + j];
            }
        }
        gl.bindBuffer(gl.ARRAY_BUFFER, buf);
        gl.bufferData(gl.ARRAY_BUFFER, sub, gl.DYNAMIC_DRAW);
    }

    function edgeInView(sp, tp, vl, vr, vt, vb) {
        var minX = Math.min(sp.x, tp.x) - RV.NODE_W;
        var maxX = Math.max(sp.x, tp.x) + RV.NODE_W;
        var minY = Math.min(sp.y, tp.y) - RV.NODE_H;
        var maxY = Math.max(sp.y, tp.y) + RV.NODE_H;
        return !(maxX < vl || minX > vr || maxY < vt || minY > vb);
    }

    // ---- Main render ------------------------------------------------------

    function render() {
        S = RV.state;
        if (RV._useFallbackRenderer) {
            // Delegate to the legacy canvas-2D renderer.
            RV.computeBounds = RV.computeBounds2d;
            RV.render = RV._render2d;
            RV.renderMinimap = RV.renderMinimap2d;
            RV.minimapToWorld = RV.minimapToWorld2d;
            return RV._render2d();
        }
        if (!gl) {
            if (!setupGl()) {
                // setupGl already set _useFallbackRenderer; recurse to use 2D.
                return render();
            }
        }

        var canvas = S.canvas;
        var dpr = S.dpr;
        var w = canvas.width;
        var h = canvas.height;

        // Resize overlay to match.
        if (overlayCanvas.width !== w || overlayCanvas.height !== h) {
            overlayCanvas.width = w;
            overlayCanvas.height = h;
            overlayCanvas.style.width = S.cssW + 'px';
            overlayCanvas.style.height = S.cssH + 'px';
        }

        gl.viewport(0, 0, w, h);
        gl.clearColor(0, 0, 0, 0);
        gl.clear(gl.COLOR_BUFFER_BIT);
        gl.enable(gl.BLEND);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

        // Rebuild instance buffers when the visible set changes.
        // We mark dirty on expand/collapse/filter/pan/zoom via markDirty().
        // For pan/zoom we rebuild every frame (cheap relative to 2D's per-shape cost).
        if (dirty || cameraChanged()) {
            rebuildInstanceBuffers();
        }

        // Draw edges first.
        if (edgeInstanceCount > 0) {
            gl.useProgram(edgeProg);
            setEdgeUniforms(edgeProg);
            gl.bindVertexArray(edgeVao);
            gl.drawArraysInstanced(gl.TRIANGLE_STRIP, 0, 4, edgeInstanceCount);
            gl.bindVertexArray(null);
        }

        // Draw nodes on top.
        if (nodeInstanceCount > 0) {
            gl.useProgram(nodeProg);
            setNodeUniforms(nodeProg);
            gl.bindVertexArray(nodeVao);
            gl.drawArraysInstanced(gl.TRIANGLE_STRIP, 0, 4, nodeInstanceCount);
            gl.bindVertexArray(null);
        }

        // Labels on the 2D overlay (only visible nodes).
        drawLabels();

        // Minimap (2D, cheap).
        renderMinimap();
    }

    var _lastCam = { x: NaN, y: NaN, zoom: NaN, cssW: NaN, cssH: NaN };
    function cameraChanged() {
        var c = _lastCam;
        var changed = c.x !== S.camera.x || c.y !== S.camera.y ||
            c.zoom !== S.zoom || c.cssW !== S.cssW || c.cssH !== S.cssH;
        if (changed) {
            c.x = S.camera.x; c.y = S.camera.y;
            c.zoom = S.zoom; c.cssW = S.cssW; c.cssH = S.cssH;
        }
        return changed;
    }

    function setNodeUniforms(prog) {
        gl.uniform2f(uloc(prog, 'uHalfView'), S.cssW / 2, S.cssH / 2);
        gl.uniform2f(uloc(prog, 'uCamera'), S.camera.x, S.camera.y);
        gl.uniform1f(uloc(prog, 'uZoom'), S.zoom);
        gl.uniform1f(uloc(prog, 'uDpr'), S.dpr);
    }
    function setEdgeUniforms(prog) {
        gl.uniform2f(uloc(prog, 'uHalfView'), S.cssW / 2, S.cssH / 2);
        gl.uniform2f(uloc(prog, 'uCamera'), S.camera.x, S.camera.y);
        gl.uniform1f(uloc(prog, 'uZoom'), S.zoom);
    }

    // ---- Labels (2D overlay) ----------------------------------------------

    function drawLabels() {
        var ctx = overlayCtx;
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.clearRect(0, 0, overlayCanvas.width, overlayCanvas.height);
        ctx.scale(S.dpr, S.dpr);
        ctx.translate(S.cssW / 2, S.cssH / 2);
        ctx.scale(S.zoom, S.zoom);
        ctx.translate(-S.camera.x, -S.camera.y);

        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.font = '12px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';

        var ids = visibleNodeIds || [];
        for (var i = 0; i < ids.length; i++) {
            var id = ids[i];
            var node = S.nodesById[id];
            var pos = S.positions[id];
            var bright = RV.nodeBrightness(id);
            var textColor = RV.NODE_TEXT_COLORS[node.type] || '#fff';
            // Glyph.
            ctx.globalAlpha = bright;
            ctx.fillStyle = textColor;
            ctx.textAlign = 'left';
            var glyph = RV.NODE_GLYPH[node.type] || '';
            if (glyph) ctx.fillText(glyph, pos.x - RV.NODE_W / 2 + 6, pos.y);
            // Label.
            ctx.textAlign = 'center';
            var label = node.simpleName || node.id;
            drawWrappedLabel(ctx, label, pos.x + 6, pos.y, RV.NODE_W - 24, bright, textColor);
            // Child badge.
            var hasChildren = (S.childrenByParent[id] || []).length > 0;
            if (hasChildren) {
                var expanded = S.expanded[id];
                ctx.textAlign = 'right';
                ctx.font = 'bold 10px -apple-system, sans-serif';
                ctx.fillStyle = expanded ? '#ffffff' : 'rgba(255,255,255,0.5)';
                if (expanded) {
                    ctx.fillText('\u2212', pos.x + RV.NODE_W / 2 - 4, pos.y);
                } else {
                    var count = (S.childrenByParent[id] || []).length;
                    ctx.fillText('+' + count, pos.x + RV.NODE_W / 2 - 4, pos.y);
                }
                ctx.font = '12px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
            }
        }
        ctx.globalAlpha = 1;
    }

    function drawWrappedLabel(ctx, text, cx, cy, maxW, alpha, color) {
        ctx.fillStyle = color;
        ctx.globalAlpha = alpha;
        if (ctx.measureText(text).width <= maxW) {
            ctx.fillText(text, cx, cy);
            return;
        }
        var t = text;
        while (t.length > 1 && ctx.measureText(t + '\u2026').width > maxW) {
            t = t.substring(0, t.length - 1);
        }
        ctx.fillText(t + '\u2026', cx, cy);
        ctx.globalAlpha = 1;
    }

    // ---- Minimap (2D, same as before) -------------------------------------

    function renderMinimap() {
        S = RV.state;
        if (!S.minimap) return;
        var mm = S.minimap;
        var mctx = S.minimapCtx;
        var dpr = S.dpr;
        var w = mm.clientWidth;
        var h = mm.clientHeight;
        if (mm.width !== w * dpr || mm.height !== h * dpr) {
            mm.width = w * dpr;
            mm.height = h * dpr;
        }
        mctx.setTransform(1, 0, 0, 1, 0, 0);
        mctx.clearRect(0, 0, mm.width, mm.height);
        mctx.scale(dpr, dpr);

        var b = S.bounds;
        var bw = Math.max(1, b.maxX - b.minX);
        var bh = Math.max(1, b.maxY - b.minY);
        var scale = Math.min(w / bw, h / bh) * 0.9;
        var offX = (w - bw * scale) / 2;
        var offY = (h - bh * scale) / 2;

        mctx.fillStyle = 'rgba(88,166,255,0.08)';
        for (var mId in S.moduleMembers) {
            if (!S.moduleMembers.hasOwnProperty(mId)) continue;
            var members = S.moduleMembers[mId];
            var mMinX = Infinity, mMaxX = -Infinity, mMinY = Infinity, mMaxY = -Infinity;
            for (var i = 0; i < members.length; i++) {
                var p = S.positions[members[i]];
                if (!p) continue;
                if (p.x < mMinX) mMinX = p.x;
                if (p.x > mMaxX) mMaxX = p.x;
                if (p.y < mMinY) mMinY = p.y;
                if (p.y > mMaxY) mMaxY = p.y;
            }
            if (mMinX === Infinity) continue;
            mctx.fillRect(
                offX + (mMinX - b.minX) * scale,
                offY + (mMinY - b.minY) * scale,
                Math.max(2, (mMaxX - mMinX) * scale),
                Math.max(2, (mMaxY - mMinY) * scale)
            );
        }

        mctx.fillStyle = 'rgba(201,209,217,0.6)';
        for (var id in S.positions) {
            if (!S.positions.hasOwnProperty(id)) continue;
            var node = S.nodesById[id];
            if (!RV.isNodeVisible(node)) continue;
            var pos = S.positions[id];
            mctx.fillStyle = RV.NODE_COLORS[node.type] || '#888';
            mctx.fillRect(
                offX + (pos.x - b.minX) * scale - 1,
                offY + (pos.y - b.minY) * scale - 1,
                2, 2
            );
        }

        var viewW = S.cssW / S.zoom;
        var viewH = S.cssH / S.zoom;
        mctx.strokeStyle = '#58a6ff';
        mctx.lineWidth = 1;
        mctx.strokeRect(
            offX + (S.camera.x - viewW / 2 - b.minX) * scale,
            offY + (S.camera.y - viewH / 2 - b.minY) * scale,
            viewW * scale,
            viewH * scale
        );
    }

    function minimapToWorld(mx, my) {
        S = RV.state;
        var mm = S.minimap;
        var w = mm.clientWidth;
        var h = mm.clientHeight;
        var b = S.bounds;
        var bw = Math.max(1, b.maxX - b.minX);
        var bh = Math.max(1, b.maxY - b.minY);
        var scale = Math.min(w / bw, h / bh) * 0.9;
        var offX = (w - bw * scale) / 2;
        var offY = (h - bh * scale) / 2;
        return {
            x: b.minX + (mx - offX) / scale,
            y: b.minY + (my - offY) / scale
        };
    }

    // ---- Public API -------------------------------------------------------

    RV.computeBounds = computeBounds;
    RV.render = render;
    RV.renderMinimap = renderMinimap;
    RV.minimapToWorld = minimapToWorld;
    RV.markRenderDirty = function () { dirty = true; };
})(window.RV = window.RV || {});
