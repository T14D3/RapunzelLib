/* =====================================================================
 * Codebase Visualizer - WebGL2 renderer
 *
 * Replaces the canvas-2D renderer (app.render.js) for large graphs.
 * Nodes are drawn as instanced rounded-rect quads in a single draw call;
 * edges as instanced thick line segments in another. A small canvas-2D
 * overlay draws labels only for the (culled) visible nodes - typically a
 * few hundred - so text cost stays bounded regardless of graph size.
 *
 * KEY PERFORMANCE PRINCIPLE: GPU buffers are NEVER rebuilt during camera
 * interaction (pan/zoom). The full visible set is uploaded once when the
 * graph loads or when expand/collapse/filter/layout changes. The GPU
 * clips off-screen geometry for free via its built-in NDC clipping.
 * This eliminates the ~1.7MB/-frame buffer upload that was the primary
 * rendering bottleneck.
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

    // Persistent GPU buffers are uploaded once and left untouched during
    // camera interaction. They contain ALL filter-visible nodes/edges
    // regardless of viewport position. The GPU clips.
    var nodeInstanceCount = 0;
    var edgeInstanceCount = 0;
    var dirty = true; // rebuild full buffers on next render
    var visibleNodeIds = [];
    var visibleEdgeIdxs = [];

    // ---- Shaders ----------------------------------------------------------

    var NODE_VS = [
        '#version 300 es',
        'precision highp float;',
        'uniform vec2 uHalfView;   // (cssW/2, cssH/2)',
        'uniform vec2 uCamera;',
        'uniform float uZoom;',
        'uniform float uDpr;',
        'const vec2 corners[4] = vec2[4](vec2(-1.0,-1.0), vec2(1.0,-1.0), vec2(-1.0,1.0), vec2(1.0,1.0));',
        'in float aCorner;',
        'in vec2 aCenter;',
        'in vec2 aHalfSize;',
        'in vec4 aColor;',
        'in vec4 aBorderColor;',
        'in float aBorderWidth;',
        'in float aBright;',
        'in float aRadius;',
        'out vec2 vLocal;',
        'out vec4 vFillColor;',
        'out vec4 vBorderColor;',
        'out float vBorderWidth;',
        'out float vBright;',
        'out float vRadius;',
        'out vec2 vHalfSize;',
        'void main() {',
        '  vec2 corner = corners[int(aCorner)];',
        '  vec2 local = corner * aHalfSize;',
        '  vec2 world = aCenter + local;',
        '  vec2 px = (world - uCamera) * uZoom;',
        // Flip Y: NDC has +Y up, but our world/canvas coords have +Y down.
        '  vec2 ndc = vec2(px.x / uHalfView.x, -px.y / uHalfView.y);',
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
        'float sdRoundedBox(vec2 p, vec2 h, float r) {',
        '  vec2 q = abs(p) - h + vec2(r);',
        '  return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;',
        '}',
        'void main() {',
        '  float dist = sdRoundedBox(vLocal, vHalfSize, vRadius);',
        '  float aaWorld = 1.0 / max(0.05, uZoom);',
        '  float fillAlpha = 1.0 - smoothstep(-aaWorld, aaWorld, dist);',
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

    var EDGE_VS = [
        '#version 300 es',
        'precision highp float;',
        'uniform vec2 uHalfView;',
        'uniform vec2 uCamera;',
        'uniform float uZoom;',
        'in float aCorner;',
        'in vec2 aSrc;',
        'in vec2 aTgt;',
        'in vec4 aColor;',
        'in float aWidth;',
        'in float aBright;',
        'out vec4 vColor;',
        'void main() {',
        '  vec2 d = aTgt - aSrc;',
        '  float len = length(d);',
        '  float wWorld = aWidth / max(0.05, uZoom);',
        // If edge is very short or zoomed way out, skip it by moving off-screen.
        '  if (len < 1.0 || wWorld < 0.5) { gl_Position = vec4(2.0, 2.0, 0.0, 1.0); return; }',
        '  vec2 dir = d / len;',
        '  vec2 nrm = vec2(-dir.y, dir.x);',
        '  vec2 corners[4] = vec2[4](vec2(-1.0,-1.0), vec2(1.0,-1.0), vec2(-1.0,1.0), vec2(1.0,1.0));',
        '  vec2 c = corners[int(aCorner)];',
        '  vec2 local = vec2(c.x * len * 0.5, c.y * wWorld * 0.5);',
        '  vec2 mid = (aSrc + aTgt) * 0.5;',
        '  vec2 world = mid + dir * local.x + nrm * local.y;',
        '  vec2 px = (world - uCamera) * uZoom;',
        // Flip Y: NDC has +Y up, but our world/canvas coords have +Y down.
        '  vec2 ndc = vec2(px.x / uHalfView.x, -px.y / uHalfView.y);',
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

    function hexToRgb01(hex) {
        var h = hex.replace('#', '');
        return [
            parseInt(h.substring(0, 2), 16) / 255,
            parseInt(h.substring(2, 4), 16) / 255,
            parseInt(h.substring(4, 6), 16) / 255
        ];
    }

    // ---- Setup ------------------------------------------------------------

    function setBadge(useWebGL2) {
        var badge = document.getElementById('renderer-badge');
        if (!badge) return;
        if (useWebGL2) {
            badge.className = 'webgl2';
            badge.textContent = 'WebGL2';
        } else {
            badge.className = 'canvas2d';
            badge.textContent = 'Canvas2D';
        }
    }

    function setupGl() {
        S = RV.state;
        var canvas = S.canvas;
        gl = canvas.getContext('webgl2', { antialias: true, premultipliedAlpha: false });
        if (!gl) { RV._useFallbackRenderer = true; setBadge(false); return false; }
        try {
            nodeProg = linkProgram(NODE_VS, NODE_FS);
            edgeProg = linkProgram(EDGE_VS, EDGE_FS);
        } catch (e) {
            console.error('WebGL2 setup failed, falling back to 2D:', e);
            RV._useFallbackRenderer = true;
            setBadge(false);
            gl = null;
            return false;
        }

        setBadge(true);

        // Overlay canvas for labels (sits on top of GL canvas).
        if (!overlayCanvas) {
            overlayCanvas = document.createElement('canvas');
            overlayCanvas.id = 'label-overlay';
            overlayCanvas.style.position = 'absolute';
            overlayCanvas.style.pointerEvents = 'none';
            canvas.parentElement.appendChild(overlayCanvas);
            overlayCtx = overlayCanvas.getContext('2d');
        }

        buildGeometry();
        return true;
    }

    var cornerBuffer = null;

    function buildCornerBuffer() {
        if (cornerBuffer) return;
        cornerBuffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, cornerBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0, 1, 2, 3]), gl.STATIC_DRAW);
    }

    function buildGeometry() {
        buildCornerBuffer();

        // Node VAO
        nodeVao = gl.createVertexArray();
        gl.bindVertexArray(nodeVao);
        gl.bindBuffer(gl.ARRAY_BUFFER, cornerBuffer);
        var a = loc(nodeProg, 'aCorner');
        gl.enableVertexAttribArray(a);
        gl.vertexAttribPointer(a, 1, gl.FLOAT, false, 0, 0);
        gl.vertexAttribDivisor(a, 0);

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

        // Edge VAO
        edgeVao = gl.createVertexArray();
        gl.bindVertexArray(edgeVao);
        gl.bindBuffer(gl.ARRAY_BUFFER, cornerBuffer);
        var ea = loc(edgeProg, 'aCorner');
        gl.enableVertexAttribArray(ea);
        gl.vertexAttribPointer(ea, 1, gl.FLOAT, false, 0, 0);
        gl.vertexAttribDivisor(ea, 0);

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

    // ---- Full buffer rebuild (only on dirty) -----------------------------

    function rebuildFullBuffers() {
        S = RV.state;
        dirty = false;

        // Collect ALL filter-visible nodes (no viewport culling - GPU handles that).
        var ids = [];
        for (var id in S.positions) {
            if (!S.positions.hasOwnProperty(id)) continue;
            var node = S.nodesById[id];
            if (!RV.isNodeVisible(node)) continue;
            ids.push(id);
        }
        var nCount = ids.length;
        visibleNodeIds = ids;

        // Collect ALL and filter-visible edges.
        var edgeList = [];
        for (var i = 0; i < S.graph.edges.length; i++) {
            var edge = S.graph.edges[i];
            if (edge.type !== 'contains') continue;
            if (!RV.isEdgeVisible(edge)) continue;
            var sp = S.positions[edge.source];
            var tp = S.positions[edge.target];
            if (!sp || !tp) continue;
            edgeList.push(edge);
        }
        for (var j = 0; j < S.crossEdges.length; j++) {
            var ce = S.crossEdges[j];
            if (!RV.isEdgeVisible(ce)) continue;
            var csp = S.positions[ce.source];
            var ctp = S.positions[ce.target];
            if (!csp || !ctp) continue;
            edgeList.push(ce);
        }
        var eCount = edgeList.length;
        visibleEdgeIdxs = edgeList;

        // Build node instance data.
        var nStride = 15;
        var nodeData = new Float32Array(nCount * nStride);
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
            nodeData[off + 0] = pos.x;
            nodeData[off + 1] = pos.y;
            nodeData[off + 2] = RV.NODE_W / 2;
            nodeData[off + 3] = RV.NODE_H / 2;
            nodeData[off + 4] = bg[0];
            nodeData[off + 5] = bg[1];
            nodeData[off + 6] = bg[2];
            nodeData[off + 7] = 1.0;
            nodeData[off + 8] = bd[0];
            nodeData[off + 9] = bd[1];
            nodeData[off + 10] = bd[2];
            nodeData[off + 11] = selected || hovered ? 1.0 : 0.15;
            nodeData[off + 12] = bw;
            nodeData[off + 13] = bright;
            nodeData[off + 14] = 6;
        }
        uploadInstanceBuffer(nodeBuffers.center, nodeData, nCount, 15, 0, 2);
        uploadInstanceBuffer(nodeBuffers.halfSize, nodeData, nCount, 15, 2, 2);
        uploadInstanceBuffer(nodeBuffers.color, nodeData, nCount, 15, 4, 4);
        uploadInstanceBuffer(nodeBuffers.borderColor, nodeData, nCount, 15, 8, 4);
        uploadInstanceBuffer(nodeBuffers.borderWidth, nodeData, nCount, 15, 12, 1);
        uploadInstanceBuffer(nodeBuffers.bright, nodeData, nCount, 15, 13, 1);
        uploadInstanceBuffer(nodeBuffers.radius, nodeData, nCount, 15, 14, 1);

        // Build edge instance data.
        var eStride = 10;
        var edgeData = new Float32Array(eCount * eStride);
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
            edgeData[off2 + 0] = esp.x;
            edgeData[off2 + 1] = esp.y;
            edgeData[off2 + 2] = etp.x;
            edgeData[off2 + 3] = etp.y;
            edgeData[off2 + 4] = ec[0];
            edgeData[off2 + 5] = ec[1];
            edgeData[off2 + 6] = ec[2];
            edgeData[off2 + 7] = alpha;
            edgeData[off2 + 8] = width;
            edgeData[off2 + 9] = 1.0;
        }
        uploadInstanceBuffer(edgeBuffers.src, edgeData, eCount, 10, 0, 2);
        uploadInstanceBuffer(edgeBuffers.tgt, edgeData, eCount, 10, 2, 2);
        uploadInstanceBuffer(edgeBuffers.color, edgeData, eCount, 10, 4, 4);
        uploadInstanceBuffer(edgeBuffers.width, edgeData, eCount, 10, 8, 1);
        uploadInstanceBuffer(edgeBuffers.bright, edgeData, eCount, 10, 9, 1);
    }

    function uploadInstanceBuffer(buf, data, count, stride, offset, size) {
        var sub = new Float32Array(count * size);
        for (var i = 0; i < count; i++) {
            for (var j = 0; j < size; j++) {
                sub[i * size + j] = data[i * stride + offset + j];
            }
        }
        gl.bindBuffer(gl.ARRAY_BUFFER, buf);
        gl.bufferData(gl.ARRAY_BUFFER, sub, gl.DYNAMIC_DRAW);
    }

    // ---- Render (runs every frame during interaction) ---------------------

    function render() {
        S = RV.state;
        if (RV._useFallbackRenderer) {
            RV.computeBounds = RV.computeBounds2d;
            RV.render = RV._render2d;
            RV.renderMinimap = RV.renderMinimap2d;
            RV.minimapToWorld = RV.minimapToWorld2d;
            return RV._render2d();
        }
        if (!gl) {
            if (!setupGl()) return render();
        }

        var canvas = S.canvas;
        var dpr = S.dpr;
        var w = canvas.width;
        var h = canvas.height;

        // Position and resize overlay to exactly match the graph canvas.
        // The canvas is in a grid cell, so we sync the overlay's position
        // to the canvas's bounding rect each frame.
        var canvasRect = canvas.getBoundingClientRect();
        var parentRect = canvas.parentElement.getBoundingClientRect();
        overlayCanvas.style.left = (canvasRect.left - parentRect.left) + 'px';
        overlayCanvas.style.top = (canvasRect.top - parentRect.top) + 'px';
        if (overlayCanvas.width !== w || overlayCanvas.height !== h) {
            overlayCanvas.width = w;
            overlayCanvas.height = h;
            overlayCanvas.style.width = S.cssW + 'px';
            overlayCanvas.style.height = S.cssH + 'px';
        }

        // Rebuild GPU buffers only when the visible set changes.
        if (dirty) rebuildFullBuffers();

        gl.viewport(0, 0, w, h);
        gl.clearColor(0, 0, 0, 0);
        gl.clear(gl.COLOR_BUFFER_BIT);
        gl.enable(gl.BLEND);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

        // Draw edges (one draw call).
        if (edgeInstanceCount > 0) {
            gl.useProgram(edgeProg);
            setEdgeUniforms(edgeProg);
            gl.bindVertexArray(edgeVao);
            gl.drawArraysInstanced(gl.TRIANGLE_STRIP, 0, 4, edgeInstanceCount);
            gl.bindVertexArray(null);
        }

        // Draw nodes (one draw call).
        if (nodeInstanceCount > 0) {
            gl.useProgram(nodeProg);
            setNodeUniforms(nodeProg);
            gl.bindVertexArray(nodeVao);
            gl.drawArraysInstanced(gl.TRIANGLE_STRIP, 0, 4, nodeInstanceCount);
            gl.bindVertexArray(null);
        }

        // Draw labels (per-frame, viewport-culled, screen coordinates).
        drawLabels();

        // Minimap.
        renderMinimap();
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

    // ---- Labels (2D overlay, drawn per-frame in screen coordinates) --------
    // Only on-screen nodes are labelled, so cost is bounded by viewport, not
    // graph size.  This replaces the previous world-coordinate label cache
    // which was broken during pan/zoom (labels didn't move with the world).

    function drawLabels() {
        S = RV.state;
        var ctx = overlayCtx;
        var w = overlayCanvas.width;
        var h = overlayCanvas.height;
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.clearRect(0, 0, w, h);
        ctx.scale(S.dpr, S.dpr);

        ctx.textBaseline = 'middle';
        ctx.font = '12px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';

        // Screen-space viewport bounds (CSS pixels).
        var halfW = S.cssW / 2;
        var halfH = S.cssH / 2;
        var margin = RV.NODE_W; // generous margin so partially-visible labels show

        var ids = visibleNodeIds || [];
        for (var i = 0; i < ids.length; i++) {
            var id = ids[i];
            var pos = S.positions[id];
            if (!pos) continue;
            // World -> screen (CSS pixels).
            var sx = (pos.x - S.camera.x) * S.zoom + halfW;
            var sy = (pos.y - S.camera.y) * S.zoom + halfH;
            // Viewport cull: skip nodes whose bounding box is fully off-screen.
            if (sx < -margin || sx > S.cssW + margin ||
                sy < -margin || sy > S.cssH + margin) continue;

            var node = S.nodesById[id];
            var bright = RV.nodeBrightness(id);
            var textColor = RV.NODE_TEXT_COLORS[node.type] || '#fff';

            // Node box in screen pixels.
            var boxW = RV.NODE_W * S.zoom;
            var boxH = RV.NODE_H * S.zoom;
            var left = sx - boxW / 2;
            // var top = sy - boxH / 2;  // not needed for text baseline

            ctx.globalAlpha = bright;

            // Glyph (left side).
            ctx.textAlign = 'left';
            ctx.fillStyle = textColor;
            var glyph = RV.NODE_GLYPH[node.type] || '';
            if (glyph) ctx.fillText(glyph, left + 6 * S.zoom, sy);

            // Label (center, truncated to fit).
            var label = node.simpleName || node.id;
            var maxLabelW = boxW - 24 * S.zoom;
            ctx.textAlign = 'center';
            drawLabelWrapped(ctx, label, sx + 6 * S.zoom, sy, maxLabelW, bright, textColor);

            // Expand/collapse badge (right side).
            var hasChildren = (S.childrenByParent[id] || []).length > 0;
            if (hasChildren) {
                var expanded = S.expanded[id];
                ctx.textAlign = 'right';
                ctx.font = 'bold 10px -apple-system, sans-serif';
                ctx.fillStyle = expanded ? '#ffffff' : 'rgba(255,255,255,0.5)';
                if (expanded) {
                    ctx.fillText('\u2212', sx + boxW / 2 - 4 * S.zoom, sy);
                } else {
                    var count = (S.childrenByParent[id] || []).length;
                    ctx.fillText('+' + count, sx + boxW / 2 - 4 * S.zoom, sy);
                }
                ctx.font = '12px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
            }
        }
        ctx.globalAlpha = 1;
    }

    function drawLabelWrapped(ctx, text, cx, cy, maxW, alpha, color) {
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

    // ---- Minimap (2D, content cached, viewport rect drawn per-frame) -------

    var minimapCacheCanvas = null;
    var minimapCacheCtx = null;
    var minimapCacheValid = false;

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
            minimapCacheValid = false;
        }

        // Lazily create the offscreen content cache.
        if (!minimapCacheCanvas) {
            minimapCacheCanvas = document.createElement('canvas');
            minimapCacheCtx = minimapCacheCanvas.getContext('2d');
        }
        if (minimapCacheCanvas.width !== mm.width || minimapCacheCanvas.height !== mm.height) {
            minimapCacheCanvas.width = mm.width;
            minimapCacheCanvas.height = mm.height;
            minimapCacheValid = false;
        }

        // Rebuild the cached content only when the visible set changes.
        if (!minimapCacheValid || dirty) {
            rebuildMinimapCache(w, h, dpr);
        }

        // Blit cached content.
        mctx.setTransform(1, 0, 0, 1, 0, 0);
        mctx.clearRect(0, 0, mm.width, mm.height);
        mctx.drawImage(minimapCacheCanvas, 0, 0);

        // Draw viewport rectangle (changes every frame).
        var b = S.bounds;
        var bw = Math.max(1, b.maxX - b.minX);
        var bh = Math.max(1, b.maxY - b.minY);
        var scale = Math.min(w / bw, h / bh) * 0.9;
        var offX = (w - bw * scale) / 2;
        var offY = (h - bh * scale) / 2;
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

    function rebuildMinimapCache(w, h, dpr) {
        S = RV.state;
        var ctx = minimapCacheCtx;
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.clearRect(0, 0, minimapCacheCanvas.width, minimapCacheCanvas.height);
        ctx.scale(dpr, dpr);

        var b = S.bounds;
        var bw = Math.max(1, b.maxX - b.minX);
        var bh = Math.max(1, b.maxY - b.minY);
        var scale = Math.min(w / bw, h / bh) * 0.9;
        var offX = (w - bw * scale) / 2;
        var offY = (h - bh * scale) / 2;

        // Module blobs.
        ctx.fillStyle = 'rgba(88,166,255,0.08)';
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
            ctx.fillRect(
                offX + (mMinX - b.minX) * scale,
                offY + (mMinY - b.minY) * scale,
                Math.max(2, (mMaxX - mMinX) * scale),
                Math.max(2, (mMaxY - mMinY) * scale)
            );
        }

        // Node dots.
        for (var id in S.positions) {
            if (!S.positions.hasOwnProperty(id)) continue;
            var node = S.nodesById[id];
            if (!RV.isNodeVisible(node)) continue;
            var pos = S.positions[id];
            ctx.fillStyle = RV.NODE_COLORS[node.type] || '#888';
            ctx.fillRect(
                offX + (pos.x - b.minX) * scale - 1,
                offY + (pos.y - b.minY) * scale - 1,
                2, 2
            );
        }

        minimapCacheValid = true;
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
