/* =====================================================================
 * Codebase Visualizer - radial layout (collapsed chains + local circles)
 *
 * Single-child chains (e.g., module -> sourceSet) are collapsed into a
 * single displayed node.  Only branch points and leaves remain visible.
 * The root's children are placed on a concentric ring; everything deeper
 * uses local circles around the parent.
 *
 * ┌─ root
 * │   ├─ module::api (package A1, package A2, …)    ← sourceSet collapsed
 * │   ├─ module::commands (package B1, package B2)   ← sourceSet collapsed
 * │   └─ …
 * └──
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;
    var FULL_CIRCLE = 2 * Math.PI;
    var collapsed = {};       // child id -> display-parent id this child merges into
    var displayLabel = {};    // display-parent id -> combined label text

    // ---- public entry point ------------------------------------------------

    function compute() {
        S = RV.state;
        if (!S.rootId) return;
        S.positions = {};
        S.positions[S.rootId] = { x: 0, y: 0 };
        if (S.userPositions[S.rootId]) {
            S.positions[S.rootId] = { x: S.userPositions[S.rootId].x, y: S.userPositions[S.rootId].y };
        }

        // ---- Collapse single-child chains ----------------------------------
        collapsed = {};
        displayLabel = {};
        collapseChains();

        var rootChildren = getDisplayChildren(S.rootId);
        if (rootChildren.length === 0) {
            applyUserPositions();
            return;
        }

        var spacing = S.radialSpacing || 1.0;

        // ---- Concentric ring for root children (modules) ------------------
        // Compute ring radius from the number of modules.
        var R1 = Math.max(
            rootChildren.length * (RV.NODE_W + RV.GAP) / FULL_CIRCLE,
            RV.NODE_W + RV.GAP
        ) * spacing;

        // Enlarge R1 so that packages on the inward side of their local
        // circle don't overlap modules in adjacent sectors.
        var maxLocalR = 0;
        for (var i = 0; i < rootChildren.length; i++) {
            var kids = getDisplayChildren(rootChildren[i].id);
            if (kids.length > 0) {
                var lr = kids.length * (RV.NODE_W + RV.GAP) / FULL_CIRCLE;
                lr = Math.max(lr, Math.max(RV.NODE_W, RV.NODE_H) + RV.GAP) * spacing;
                if (lr > maxLocalR) maxLocalR = lr;
            }
        }
        if (maxLocalR > 0) {
            R1 = Math.max(R1, (RV.NODE_W + maxLocalR) * spacing);
        }

        // ---- Allocate sectors to root children ----------------------------
        var totalLeaves = 0;
        var weights = [];
        for (var w = 0; w < rootChildren.length; w++) {
            var lw = 1;
            var kids = getDisplayChildren(rootChildren[w].id);
            for (var kw = 0; kw < kids.length; kw++) lw += 1;
            weights.push(lw);
            totalLeaves += lw;
        }
        if (totalLeaves === 0) totalLeaves = 1;

        var minNs = (RV.NODE_W + RV.GAP) / R1;
        var rawSpans = [];
        for (var ri = 0; ri < rootChildren.length; ri++) {
            rawSpans.push((weights[ri] / totalLeaves) * FULL_CIRCLE);
        }
        clampSpans(rawSpans, weights, minNs, FULL_CIRCLE);

        // ---- Place nodes --------------------------------------------------
        var angle = -Math.PI / 2;
        var gap = Math.min(0.02, FULL_CIRCLE / rootChildren.length * 0.1);

        for (var j = 0; j < rootChildren.length; j++) {
            var endAngle = angle + rawSpans[j];
            var mid = (angle + gap / 2 + endAngle - gap / 2) / 2;
            var px = Math.cos(mid) * R1;
            var py = Math.sin(mid) * R1;
            S.positions[rootChildren[j].id] = { x: px, y: py };

            // Collapsed descendants get the same position.
            placeCollapsedRecursive(rootChildren[j].id, px, py);

            // Children (packages) go on a local circle around this module.
            placeLocalChildren(rootChildren[j].id, px, py, spacing);

            angle = endAngle;
        }

        // ---- Resolve overlaps with radial repulsion -----------------------
        resolveOverlaps();

        applyUserPositions();
    }
    RV.layouts = RV.layouts || {};
    RV.layouts.radial = compute;

    // ---- Chain collapsing --------------------------------------------------

    function collapseChains() {
        collapsed = {};
        displayLabel = {};

        function walk(id, chain) {
            // chain is a list of { id, label } leading up to (but not including) `id`.
            var node = S.nodesById[id];
            var label = node && node.label ? node.label : id;

            if (!S.expanded[id]) {
                assignCollapsed(chain);
                return;
            }
            var children = RV.getChildren(id);
            if (children.length === 0) {
                assignCollapsed(chain);
                return;
            }
            if (children.length === 1 && id !== S.rootId) {
                // Continue the chain.
                walk(children[0].id, chain.concat([{ id: id, label: label }]));
            } else {
                // Branch or leaf: the current node is the child of the last
                // chain entry.  Collapse everything up to and including id.
                assignCollapsed(chain, id);
                // Recurse into children as new runs.
                for (var i = 0; i < children.length; i++) {
                    walk(children[i].id, []);
                }
            }
        }

        function assignCollapsed(chain, nextId) {
            // chain = ancestors that form a single-child run (first = display parent)
            // nextId = the node that follows the chain (optional)
            if (chain.length === 0 && !nextId) return;
            if (chain.length === 0) {
                // nextId is the first node - it's the display parent itself.
                return;
            }
            var displayId = chain[0].id;
            var labels = [chain[0].label];
            // Collapse chain tail into display parent.
            for (var i = 1; i < chain.length; i++) {
                collapsed[chain[i].id] = displayId;
                labels.push(chain[i].label);
            }
            // Collapse nextId (the child that follows the chain) too.
            if (nextId) {
                collapsed[nextId] = displayId;
                var nn = S.nodesById[nextId];
                if (nn && nn.label) labels.push(nn.label);
            }
            displayLabel[displayId] = labels.join(' / ');
        }

        walk(S.rootId, []);
    }

    // ---- Display-tree helpers ----------------------------------------------

    function getDisplayChildren(id) {
        // NOTE: id may itself be collapsed (when called from a parent that
        // is inlining its collapsed child's grandchildren).  We do NOT
        // redirect to the display parent - that would create infinite
        // recursion.  We just process this node's own visible children and
        // skip any that are themselves collapsed.
        if (!S.expanded[id]) return [];
        var children = RV.getChildren(id);
        var out = [];
        for (var i = 0; i < children.length; i++) {
            var cid = children[i].id;
            if (collapsed[cid]) {
                // cid merges into its parent - inline its effective children.
                var grandkids = getDisplayChildren(cid);
                for (var g = 0; g < grandkids.length; g++) out.push(grandkids[g]);
            } else {
                out.push(children[i]);
            }
        }
        return out;
    }

    // ---- Position collapsed descendants at their display parent -----------

    function placeCollapsedRecursive(id, px, py) {
        var children = RV.getChildren(id);
        for (var i = 0; i < children.length; i++) {
            var cid = children[i].id;
            if (collapsed[cid]) {
                S.positions[cid] = { x: px, y: py };
                placeCollapsedRecursive(cid, px, py);
            }
        }
    }

    // ---- Local-circle placement (depth 2+) ---------------------------------

    function placeLocalChildren(id, px, py, spacing) {
        if (!S.expanded[id]) return;
        var children = getDisplayChildren(id);
        var n = children.length;
        if (n === 0) return;

        // Local circle radius.
        var r = n * (RV.NODE_W + RV.GAP) / FULL_CIRCLE;
        r = Math.max(r, Math.max(RV.NODE_W, RV.NODE_H) + RV.GAP) * spacing;

        var step = FULL_CIRCLE / n;
        var a = -Math.PI / 2 + step / 2;   // centre first child at 12 o'clock

        for (var j = 0; j < n; j++) {
            var cx = px + Math.cos(a) * r;
            var cy = py + Math.sin(a) * r;
            S.positions[children[j].id] = { x: cx, y: cy };

            // Collapsed descendants of this child get the same position.
            placeCollapsedRecursive(children[j].id, cx, cy);

            // Recurse for this child's own children.
            placeLocalChildren(children[j].id, cx, cy, spacing);

            a += step;
        }
    }

    // ---- Post-pass: resolve overlaps with radial repulsion -----------------

    function resolveOverlaps() {
        var ids = Object.keys(S.positions);
        var n = ids.length;
        if (n < 2) return;
        var NW = RV.NODE_W, NH = RV.NODE_H;

        for (var iter = 0; iter < 5; iter++) {
            var moved = false;
            var pushX = {}, pushY = {};
            for (var i = 0; i < n; i++) pushX[ids[i]] = 0, pushY[ids[i]] = 0;

            var grid = {};
            for (var i = 0; i < n; i++) {
                var p = S.positions[ids[i]];
                if (!p) continue;
                var key = Math.floor(p.x / NW) + ',' + Math.floor(p.y / NH);
                if (!grid[key]) grid[key] = [];
                grid[key].push(ids[i]);
            }

            for (var i = 0; i < n; i++) {
                var pid = ids[i];
                var pp = S.positions[pid];
                if (!pp) continue;
                var gx = Math.floor(pp.x / NW);
                var gy = Math.floor(pp.y / NH);

                for (var dx = -1; dx <= 1; dx++) {
                    for (var dy = -1; dy <= 1; dy++) {
                        var bucket = grid[(gx + dx) + ',' + (gy + dy)];
                        if (!bucket) continue;
                        for (var k = 0; k < bucket.length; k++) {
                            var qid = bucket[k];
                            if (qid === pid) continue;
                            var qp = S.positions[qid];
                            if (!qp) continue;
                            if (Math.abs(pp.x - qp.x) < NW && Math.abs(pp.y - qp.y) < NH) {
                                var pAngle = Math.atan2(pp.y, pp.x);
                                var qAngle = Math.atan2(qp.y, qp.x);
                                var overlapX = NW - Math.abs(pp.x - qp.x);
                                var overlapY = NH - Math.abs(pp.y - qp.y);
                                var push = Math.max(overlapX / NW, overlapY / NH) * 0.3;

                                pushX[pid] += Math.cos(pAngle) * push * NW;
                                pushY[pid] += Math.sin(pAngle) * push * NH;
                                pushX[qid] -= Math.cos(qAngle) * push * NW;
                                pushY[qid] -= Math.sin(qAngle) * push * NH;
                            }
                        }
                    }
                }
            }

            for (var i = 0; i < n; i++) {
                var id = ids[i];
                if (pushX[id] !== 0 || pushY[id] !== 0) {
                    var p = S.positions[id];
                    if (p) { p.x += pushX[id]; p.y += pushY[id]; moved = true; }
                }
            }
            if (!moved) break;
        }
    }

    // ---- Span clamping ----------------------------------------------------

    function clampSpans(rawSpans, weights, minSpan, totalSpan) {
        var n = rawSpans.length;
        if (n === 0) return;
        if (n * minSpan > totalSpan) {
            var totalW = 0;
            for (var wi = 0; wi < n; wi++) totalW += weights[wi];
            if (totalW === 0) totalW = 1;
            for (var si = 0; si < n; si++) rawSpans[si] = (weights[si] / totalW) * totalSpan;
            return;
        }
        var clamped = new Array(n).fill(false);
        for (;;) {
            var unclampedWeight = 0;
            for (var cj = 0; cj < n; cj++) if (!clamped[cj]) unclampedWeight += weights[cj];
            if (unclampedWeight <= 0) break;
            var minIdx = -1, minVal = Infinity;
            for (var ck = 0; ck < n; ck++) {
                if (clamped[ck]) continue;
                if (rawSpans[ck] < minVal) { minVal = rawSpans[ck]; minIdx = ck; }
            }
            if (minIdx < 0 || minVal >= minSpan) break;
            clamped[minIdx] = true;
            rawSpans[minIdx] = minSpan;
            var remaining = totalSpan;
            for (var cr = 0; cr < n; cr++) if (clamped[cr]) remaining -= rawSpans[cr];
            for (var cu = 0; cu < n; cu++) {
                if (!clamped[cu]) rawSpans[cu] = (weights[cu] / unclampedWeight) * remaining;
            }
        }
    }

    // Expose display labels so the renderer can show combined names.
    RV.getDisplayLabel = function (id) {
        return displayLabel[id] || null;
    };

    function applyUserPositions() {
        for (var id in S.userPositions) {
            if (!S.userPositions.hasOwnProperty(id)) continue;
            if (S.nodesById[id]) {
                S.positions[id] = { x: S.userPositions[id].x, y: S.userPositions[id].y };
            }
        }
    }
})(window.RV = window.RV || {});
