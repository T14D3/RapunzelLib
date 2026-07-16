/* =====================================================================
 * Codebase Visualizer - renderer
 *
 * Draws nodes and edges to the main canvas with:
 *   - viewport culling
 *   - offscreen cache for the contains layer
 *   - curved edges + arrowheads
 *   - focus-layer dimming
 *   - child-count badges, type glyphs, multi-line labels
 *   - minimap
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    // ---- Bounds ------------------------------------------------------------

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

    // ---- Main render -------------------------------------------------------

    function render() {
        S = RV.state;
        var canvas = S.canvas;
        // Lazily acquire the 2D context (only when the fallback is used).
        if (!S.ctx) S.ctx = canvas.getContext('2d');
        var ctx = S.ctx;
        var dpr = S.dpr;

        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.scale(dpr, dpr);
        ctx.translate(S.cssW / 2, S.cssH / 2);
        ctx.scale(S.zoom, S.zoom);
        ctx.translate(-S.camera.x, -S.camera.y);

        // Viewport rectangle in world coords (for culling).
        var viewW = S.cssW / S.zoom;
        var viewH = S.cssH / S.zoom;
        var viewLeft = S.camera.x - viewW / 2 - RV.NODE_W;
        var viewRight = S.camera.x + viewW / 2 + RV.NODE_W;
        var viewTop = S.camera.y - viewH / 2 - RV.NODE_H;
        var viewBottom = S.camera.y + viewH / 2 + RV.NODE_H;

        // Edges first (so nodes draw on top).
        drawEdges(ctx, viewLeft, viewRight, viewTop, viewBottom);

        // Nodes.
        drawNodes(ctx, viewLeft, viewRight, viewTop, viewBottom);

        ctx.setTransform(1, 0, 0, 1, 0, 0);

        renderMinimap();
    }

    // ---- Edges -------------------------------------------------------------

    function drawEdges(ctx, vl, vr, vt, vb) {
        var i, edge, src, tgt;
        // Contains edges: drawn from cache if available & clean.
        // For simplicity (and because contains edges change on expand/collapse),
        // we draw them directly but culled.
        for (i = 0; i < S.graph.edges.length; i++) {
            edge = S.graph.edges[i];
            if (edge.type !== 'contains') continue;
            if (!RV.isEdgeVisible(edge)) continue;
            src = S.positions[edge.source];
            tgt = S.positions[edge.target];
            if (!src || !tgt) continue;
            if (!inView(src, tgt, vl, vr, vt, vb)) continue;
            drawContainsEdge(ctx, src, tgt, edge);
        }
        // Cross edges.
        for (i = 0; i < S.crossEdges.length; i++) {
            edge = S.crossEdges[i];
            if (!RV.isEdgeVisible(edge)) continue;
            src = S.positions[edge.source];
            tgt = S.positions[edge.target];
            if (!src || !tgt) continue;
            if (!inView(src, tgt, vl, vr, vt, vb)) continue;
            drawCrossEdge(ctx, src, tgt, edge);
        }
    }

    function inView(src, tgt, vl, vr, vt, vb) {
        var minX = Math.min(src.x, tgt.x) - RV.NODE_W;
        var maxX = Math.max(src.x, tgt.x) + RV.NODE_W;
        var minY = Math.min(src.y, tgt.y) - RV.NODE_H;
        var maxY = Math.max(src.y, tgt.y) + RV.NODE_H;
        return !(maxX < vl || minX > vr || maxY < vt || minY > vb);
    }

    function drawContainsEdge(ctx, src, tgt, edge) {
        var group = RV.EDGE_GROUPS[edge.type];
        var baseAlpha = S.layerAlpha[group] || 0.45;
        var bright = RV.edgeBrightness(edge);
        var alpha = baseAlpha * bright;
        if (alpha < 0.02) return;

        ctx.strokeStyle = RV.EDGE_COLORS[edge.type];
        ctx.lineWidth = 1;
        ctx.globalAlpha = alpha;

        // Vertical S-curve from bottom of parent to top of child.
        var x1 = src.x, y1 = src.y + RV.NODE_H / 2;
        var x2 = tgt.x, y2 = tgt.y - RV.NODE_H / 2;
        var dy = (y2 - y1) / 2;
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.bezierCurveTo(x1, y1 + dy, x2, y2 - dy, x2, y2);
        ctx.stroke();
        ctx.globalAlpha = 1;
    }

    function drawCrossEdge(ctx, src, tgt, edge) {
        var group = RV.EDGE_GROUPS[edge.type];
        var baseAlpha = S.layerAlpha[group] || 0.5;
        var bright = RV.edgeBrightness(edge);
        var alpha = baseAlpha * bright;
        if (alpha < 0.02) return;

        var isHovered = S.hoveredEdge && S.hoveredEdge.source === edge.source &&
            S.hoveredEdge.target === edge.target && S.hoveredEdge.type === edge.type;

        ctx.strokeStyle = RV.EDGE_COLORS[edge.type];
        ctx.lineWidth = isHovered ? 2.5 : 1.4;
        ctx.globalAlpha = isHovered ? Math.min(1, alpha + 0.4) : alpha;

        // Curved arc: control point offset perpendicular to the line.
        var x1 = src.x, y1 = src.y;
        var x2 = tgt.x, y2 = tgt.y;
        var mx = (x1 + x2) / 2;
        var my = (y1 + y2) / 2;
        var dx = x2 - x1, dy = y2 - y1;
        var len = Math.sqrt(dx * dx + dy * dy) || 1;
        // Perpendicular offset proportional to length, capped.
        var offset = Math.min(60, len * 0.18);
        var nx = -dy / len * offset;
        var ny = dx / len * offset;
        var cx = mx + nx, cy = my + ny;

        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.quadraticCurveTo(cx, cy, x2, y2);
        ctx.stroke();

        // Arrowhead at target end.
        drawArrowhead(ctx, cx, cy, x2, y2, RV.EDGE_COLORS[edge.type], ctx.globalAlpha);

        ctx.globalAlpha = 1;
    }

    function drawArrowhead(ctx, fromX, fromY, toX, toY, color, alpha) {
        var dx = toX - fromX, dy = toY - fromY;
        var len = Math.sqrt(dx * dx + dy * dy) || 1;
        var ux = dx / len, uy = dy / len;
        var size = 7;
        var bx = toX - ux * RV.NODE_W / 2;
        var by = toY - uy * RV.NODE_H / 2;
        ctx.globalAlpha = alpha;
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.moveTo(bx, by);
        ctx.lineTo(bx - ux * size - uy * size * 0.5, by - uy * size + ux * size * 0.5);
        ctx.lineTo(bx - ux * size + uy * size * 0.5, by - uy * size - ux * size * 0.5);
        ctx.closePath();
        ctx.fill();
        ctx.globalAlpha = 1;
    }

    // ---- Nodes -------------------------------------------------------------

    function drawNodes(ctx, vl, vr, vt, vb) {
        for (var id in S.positions) {
            if (!S.positions.hasOwnProperty(id)) continue;
            // Skip collapsed nodes - they merge into their parent visually.
            if (RV.isCollapsed && RV.isCollapsed(id)) continue;
            var node = S.nodesById[id];
            if (!RV.isNodeVisible(node)) continue;
            var pos = S.positions[id];
            if (pos.x < vl || pos.x > vr || pos.y < vt || pos.y > vb) continue;
            drawNode(ctx, node, pos);
        }
    }

    function drawNode(ctx, node, pos) {
        var x = pos.x - RV.NODE_W / 2;
        var y = pos.y - RV.NODE_H / 2;
        var selected = S.selected === node.id;
        var hovered = S.hoveredNode === node.id;
        var expanded = S.expanded[node.id];
        var hasChildren = (S.childrenByParent[node.id] || []).length > 0;
        var bright = RV.nodeBrightness(node.id);

        var bgColor = RV.NODE_COLORS[node.type] || '#555';
        var textColor = RV.NODE_TEXT_COLORS[node.type] || '#fff';

        // Dim non-focus nodes by drawing a translucent dark overlay after fill.
        ctx.globalAlpha = 1;
        ctx.fillStyle = bgColor;
        ctx.strokeStyle = selected ? '#ffffff' : (hovered ? RV.NODE_COLORS[node.type] : 'rgba(255,255,255,0.15)');
        // Make hover/selected border more visible.
        if (hovered && !selected) ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = selected ? 3 : (hovered ? 2 : 1);
        roundRect(ctx, x, y, RV.NODE_W, RV.NODE_H, 6);
        ctx.fill();
        ctx.stroke();

        // Focus dimming overlay.
        if (bright < 1) {
            ctx.globalAlpha = 1 - bright;
            ctx.fillStyle = '#0d1117';
            roundRect(ctx, x, y, RV.NODE_W, RV.NODE_H, 6);
            ctx.fill();
            ctx.globalAlpha = 1;
        }

        // Glyph (type indicator).
        ctx.fillStyle = textColor;
        ctx.globalAlpha = bright;
        ctx.font = 'bold 11px -apple-system, sans-serif';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'middle';
        var glyph = RV.NODE_GLYPH[node.type] || '';
        if (glyph) ctx.fillText(glyph, x + 6, pos.y);

        // Label (wrap to 2 lines if long).
        ctx.font = '12px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        var label = RV.getDisplayLabel ? (RV.getDisplayLabel(node.id) || node.simpleName || node.id) : (node.simpleName || node.id);
        drawWrappedLabel(ctx, label, pos.x + 6, pos.y, RV.NODE_W - 24, bright, textColor);

        // Expand/collapse indicator + child count badge.
        if (hasChildren) {
            var badgeX = x + RV.NODE_W - 4;
            var badgeY = y + RV.NODE_H / 2;
            ctx.globalAlpha = bright;
            ctx.fillStyle = expanded ? '#ffffff' : 'rgba(255,255,255,0.5)';
            ctx.font = 'bold 10px -apple-system, sans-serif';
            ctx.textAlign = 'right';
            ctx.textBaseline = 'middle';
            if (expanded) {
                ctx.fillText('\u2212', badgeX, badgeY);
            } else {
                var count = (S.childrenByParent[node.id] || []).length;
                ctx.fillText('+' + count, badgeX, badgeY);
            }
        }
        ctx.globalAlpha = 1;
    }

    function drawWrappedLabel(ctx, text, cx, cy, maxW, alpha, color) {
        ctx.fillStyle = color;
        ctx.globalAlpha = alpha;
        // Try single line first.
        if (ctx.measureText(text).width <= maxW) {
            ctx.fillText(text, cx, cy);
            return;
        }
        // Truncate to fit maxW with ellipsis.
        var t = text;
        while (t.length > 1 && ctx.measureText(t + '\u2026').width > maxW) {
            t = t.substring(0, t.length - 1);
        }
        ctx.fillText(t + '\u2026', cx, cy);
        ctx.globalAlpha = 1;
    }

    function roundRect(ctx, x, y, w, h, r) {
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.lineTo(x + w - r, y);
        ctx.quadraticCurveTo(x + w, y, x + w, y + r);
        ctx.lineTo(x + w, y + h - r);
        ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
        ctx.lineTo(x + r, y + h);
        ctx.quadraticCurveTo(x, y + h, x, y + h - r);
        ctx.lineTo(x, y + r);
        ctx.quadraticCurveTo(x, y, x + r, y);
        ctx.closePath();
    }

    // ---- Minimap -----------------------------------------------------------

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

        // Module blobs.
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

        // Node dots.
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

        // Viewport rectangle.
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

    RV.computeBounds2d = computeBounds;
    RV._render2d = render;
    RV.renderMinimap2d = renderMinimap;
    RV.minimapToWorld2d = minimapToWorld;
})(window.RV = window.RV || {});
