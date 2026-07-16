/* =====================================================================
 * Codebase Visualizer - radial layout (recursive angular sectors)
 *
 * Algorithm:
 *
 *   1. Ring radii are computed bottom-up so each depth has exactly
 *      enough circumference to fit all its nodes without angular overlap.
 *
 *   2. Minimum radial gap between adjacent rings is NODE_W + GAP so
 *      nodes at the same angle on different rings never overlap.
 *
 *   3. Within a ring, angular sectors are proportional to leaf count,
 *      clamped to a minimum of (NODE_W + GAP) / radius so a single
 *      wide node always fits, with iterative renormalisation.
 *
 *   4. Staggering: when a parent's angular sector is much wider than
 *      its few children need, the children are placed at a *smaller*
 *      radius (pulled inward) to fill the hole in the centre, subject
 *      to radial-separation constraints that prevent parent-child and
 *      cross-ring overlap.
 *
 *   5. The radial-spacing slider (S.radialSpacing) scales all ring
 *      radii uniformly, making the whole layout tighter or looser.
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;
    var MIN_ANG_GAP = 0.02;          // ~1.1° gap between adjacent siblings
    var FULL_CIRCLE = 2 * Math.PI;

    // ---- public entry point ------------------------------------------------

    function compute() {
        S = RV.state;
        if (!S.rootId) return;
        S.positions = {};
        S.positions[S.rootId] = { x: 0, y: 0 };
        if (S.userPositions[S.rootId]) {
            S.positions[S.rootId] = S.userPositions[S.rootId];
        }
        var children = RV.getChildren(S.rootId);
        if (children.length === 0) {
            applyUserPositions();
            return;
        }

        // ---- Pass 1: bottom-up metadata (leaf count & depth) -------------
        var meta = {};
        computeMeta(S.rootId, meta, 0);
        var maxDepth = 0;
        for (var id in meta) {
            if (meta[id].nodeDepth > maxDepth) maxDepth = meta[id].nodeDepth;
        }

        // ---- Ring radii --------------------------------------------------
        // For each depth compute the exact radius that fits all its nodes,
        // then enforce a minimum radial gap so nodes at the same angle on
        // adjacent rings never overlap.
        var spacing = S.radialSpacing || 1.0;
        var ringRadii = [0];                           // depth 0 = root
        for (var d = 1; d <= maxDepth; d++) {
            var cnt = 0;
            for (var mid in meta) {
                if (meta[mid].nodeDepth === d) cnt++;
            }
            // Minimum radius so all nodes at this depth fit around the circle
            // without angular overlap.  Spacing scales this proportionally.
            var angularMin = cnt * (RV.NODE_W + RV.GAP) / FULL_CIRCLE;
            // Enforce minimum radial gap so nodes at the same angle on
            // adjacent rings are separated by at least NODE_W + GAP.
            // This gap is NOT scaled by spacing - it's a fixed geometric
            // constraint that prevents cross-ring bounding-box overlap.
            var prevRing = ringRadii[d - 1];
            var minRadGap = RV.NODE_W + RV.GAP;
            ringRadii[d] = Math.max(angularMin, prevRing + minRadGap);
        }

        // Apply the spacing slider uniformly to all ring radii.
        for (var rd = 1; rd <= maxDepth; rd++) {
            ringRadii[rd] *= spacing;
        }

        // ---- Pass 2: allocate sectors for root children ------------------
        var totalWeight = 0;
        var weights = [];
        for (var w = 0; w < children.length; w++) {
            var lw = meta[children[w].id] ? meta[children[w].id].leaves : 1;
            weights.push(lw);
            totalWeight += lw;
        }
        if (totalWeight === 0) totalWeight = 1;

        var minNodeSpan = (RV.NODE_W + RV.GAP) / ringRadii[1];
        var rawSpans = [];
        for (var ri = 0; ri < children.length; ri++) {
            rawSpans.push((weights[ri] / totalWeight) * FULL_CIRCLE);
        }
        clampSpans(rawSpans, weights, minNodeSpan, FULL_CIRCLE);

        var angle = -Math.PI / 2;
        var gap = Math.min(MIN_ANG_GAP, FULL_CIRCLE / children.length * 0.1);
        for (var j = 0; j < children.length; j++) {
            var endAngle = angle + rawSpans[j];
            assignSector(children[j].id, 0, 0, ringRadii[1],
                angle + gap / 2, endAngle - gap / 2,
                1, ringRadii, meta, spacing);
            angle = endAngle;
        }

        applyUserPositions();
    }
    RV.layouts = RV.layouts || {};
    RV.layouts.radial = compute;

    // ---- Pass 1: bottom-up metadata ----------------------------------------

    function computeMeta(id, meta, nodeDepth) {
        var children = RV.getChildren(id);
        var leaves = 1;
        var maxSubDepth = 0;
        if (S.expanded[id] && children.length > 0) {
            leaves = 0;
            for (var i = 0; i < children.length; i++) {
                computeMeta(children[i].id, meta, nodeDepth + 1);
                var cm = meta[children[i].id];
                leaves += cm.leaves;
                if (cm.subtreeDepth + 1 > maxSubDepth) maxSubDepth = cm.subtreeDepth + 1;
            }
        }
        meta[id] = {
            leaves: Math.max(1, leaves),
            nodeDepth: nodeDepth,
            subtreeDepth: maxSubDepth
        };
    }

    // ---- Span clamping ----------------------------------------------------
    // Iteratively clamp sectors below minNodeSpan up to minNodeSpan and
    // redistribute remaining space proportionally by weight.
    //
    // If n * minSpan > totalSpan (the "death spiral" case), fall back to
    // proportional sizing - each child gets an equal share of the available
    // space.  This prevents negative spans from overflowing parent sectors.

    function clampSpans(rawSpans, weights, minSpan, totalSpan) {
        var n = rawSpans.length;
        if (n === 0) return;
        // Guard: if the minimum possible total exceeds the available span,
        // distribute proportionally by weight (truncated to totalSpan).
        if (n * minSpan > totalSpan) {
            var totalW = 0;
            for (var wi = 0; wi < n; wi++) totalW += weights[wi];
            if (totalW === 0) totalW = 1;
            var remaining = totalSpan;
            // Assign proportional spans, then clip to totalSpan.
            for (var si = 0; si < n; si++) {
                rawSpans[si] = (weights[si] / totalW) * totalSpan;
            }
            return;
        }
        var clamped = new Array(n).fill(false);
        for (;;) {
            var unclampedWeight = 0;
            for (var cj = 0; cj < n; cj++) {
                if (!clamped[cj]) unclampedWeight += weights[cj];
            }
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
            for (var cr = 0; cr < n; cr++) {
                if (clamped[cr]) remaining -= rawSpans[cr];
            }
            for (var cu = 0; cu < n; cu++) {
                if (!clamped[cu]) {
                    rawSpans[cu] = (weights[cu] / unclampedWeight) * remaining;
                }
            }
        }
    }

    // ---- Pass 2: top-down sector assignment --------------------------------

    function assignSector(id, cx, cy, radius, startAngle, endAngle, depth,
                          ringRadii, meta, spacing) {
        var mid = (startAngle + endAngle) / 2;
        S.positions[id] = {
            x: cx + Math.cos(mid) * radius,
            y: cy + Math.sin(mid) * radius
        };

        if (!S.expanded[id]) return;
        var children = RV.getChildren(id);
        if (children.length === 0) return;

        var span = endAngle - startAngle;
        var childDepth = depth + 1;
        var normalRadius = ringRadii[childDepth] ||
            (radius + (RV.NODE_W + RV.GAP) * spacing);
        var childRadius = normalRadius;

        // ---- Staggering ----------------------------------------------------
        // If this subtree has very few children (≤2) and the angular
        // sector is much wider than needed, pull the children inward to
        // fill the empty centre.
        if (children.length <= 2 && span > 0) {
            // Minimum radius needed to fit these children side-by-side
            // without angular overlap.
            var neededRadius =
                children.length * (RV.NODE_W + RV.GAP) / span;
            // Must not overlap with parent or with the previous ring.
            var minAllowed =
                Math.max(radius + RV.NODE_W + RV.GAP,
                         (ringRadii[childDepth - 1] || 0) + RV.NODE_W + RV.GAP);
            // Must not exceed the normal child radius.
            var maxAllowed = normalRadius;
            // How far inward would we like to go?
            var idealRadius = Math.max(neededRadius, radius + RV.NODE_H + RV.GAP);
            if (idealRadius < normalRadius * 0.85) {
                // Only stagger if the ideal radius is noticeably smaller.
                childRadius = Math.max(minAllowed,
                                       Math.min(maxAllowed, idealRadius));
            }
        }

        // ---- Allocate child sectors ----------------------------------------
        var weights = [];
        var totalWeight = 0;
        for (var w = 0; w < children.length; w++) {
            var lw = meta[children[w].id] ? meta[children[w].id].leaves : 1;
            weights.push(lw);
            totalWeight += lw;
        }
        if (totalWeight === 0) totalWeight = 1;

        var minNodeSpan = (RV.NODE_W + RV.GAP) / childRadius;
        var rawSpans = [];
        for (var ri = 0; ri < children.length; ri++) {
            rawSpans.push((weights[ri] / totalWeight) * span);
        }
        clampSpans(rawSpans, weights, minNodeSpan, span);

        var cursor = startAngle;
        for (var j = 0; j < children.length; j++) {
            var childSpan = rawSpans[j];
            var g = Math.min(MIN_ANG_GAP, childSpan * 0.1);
            assignSector(children[j].id, cx, cy, childRadius,
                cursor + g / 2, cursor + childSpan - g / 2,
                childDepth, ringRadii, meta, spacing);
            cursor += childSpan;
        }
    }

    function applyUserPositions() {
        for (var id in S.userPositions) {
            if (!S.userPositions.hasOwnProperty(id)) continue;
            if (S.nodesById[id]) {
                S.positions[id] = S.userPositions[id];
            }
        }
    }
})(window.RV = window.RV || {});
