/* =====================================================================
 * Codebase Visualizer - radial layout (recursive angular sectors)
 *
 * Two-pass algorithm to minimise child overlap across different parents:
 *
 *   Pass 1 (bottom-up): compute the minimum angular footprint each subtree
 *   needs at its placement radius.  A node at radius r needs at least
 *   nodeWidth / r radians to not overlap itself.  A subtree needs at least
 *   the sum of its children's footprints (plus gaps), because all children
 *   sit on the same ring.
 *
 *   Pass 2 (top-down): allocate angular sectors proportional to the larger
 *   of (leaf-count weight, minimum footprint).  This ensures that a subtree
 *   with many children gets enough angular space even if its leaf count is
 *   small relative to siblings.
 *
 * The ring radius for each depth is computed from the maximum subtree
 * footprint at that depth, so deeper subtrees get more radial space.
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    // Minimum angular gap between adjacent children on the same ring.
    var MIN_ANGULAR_GAP = 0.02; // ~1.1 degrees

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

        var spacing = S.radialSpacing || 1.0;

        // ---- Pass 1: compute subtree metadata (leaf count + node depth) ------
        var meta = {};
        computeMeta(S.rootId, meta, 0);

        // ---- Compute ring radii per depth ----------------------------------
        // The ring radius for depth d must be large enough that the widest
        // subtree at depth d+1 fits without its children overlapping.
        var maxDepth = 0;
        for (var id in meta) {
            if (meta[id].nodeDepth > maxDepth) maxDepth = meta[id].nodeDepth;
        }

        var ringRadii = [0]; // depth 0 = root, at center
        var baseRing = RV.RADIAL_RING * spacing;
        for (var d = 1; d <= maxDepth; d++) {
            // The ring at depth d must fit ALL nodes at that depth around the
            // full circle.  Count the total number of nodes at depth d.
            var totalNodes = 0;
            var maxChildren = 1;
            for (var mid in meta) {
                if (meta[mid].nodeDepth !== d) continue;
                totalNodes++;
                if (S.expanded[mid]) {
                    var kids = RV.getChildren(mid);
                    if (kids.length > maxChildren) maxChildren = kids.length;
                }
            }
            // Minimum radius so all nodes at this depth fit around the circle.
            // Add a 1.5× padding factor to account for gaps between groups
            // and the angular overhead of intermediate-level nodes.
            var angularMinAll = totalNodes * (RV.NODE_W + RV.GAP) / (2 * Math.PI) * 1.5;
            var angularMinMax = maxChildren * (RV.NODE_W + RV.GAP) / (2 * Math.PI);
            var angularMin = Math.max(angularMinAll, angularMinMax);
            // Ensure rings are far enough apart that nodes on adjacent rings
            // don't overlap.  The minimum radial gap is NODE_H + a margin,
            // but since nodes at adjacent rings can be at different angles,
            // we use NODE_W as a safe minimum.
            var linearStep = Math.max(RV.NODE_W + RV.GAP, baseRing * (0.6 + 0.1 * d));
            ringRadii[d] = Math.max(angularMin, ringRadii[d - 1] + linearStep);
        }

        var fullCircle = Math.PI * 2;

        // ---- Pass 2: allocate sectors -------------------------------------
        // Sectors are proportional to leaf count (which equals the number of
        // deepest-level descendants).  Ring radii are pre-computed to be large
        // enough that each sector has enough arc length for its children.
        var weights = [];
        var totalWeight = 0;
        for (var w = 0; w < children.length; w++) {
            var lw = meta[children[w].id] ? meta[children[w].id].leaves : 1;
            weights.push(lw);
            totalWeight += lw;
        }
        if (totalWeight === 0) totalWeight = 1;

        // Minimum angular span for a single node at this ring radius.
        var minNodeSpan = (RV.NODE_W + RV.GAP) / ringRadii[1];

        // Compute raw spans, then clamp to minimum and renormalize.
        var rawSpans = [];
        for (var ri = 0; ri < children.length; ri++) {
            rawSpans.push((weights[ri] / totalWeight) * fullCircle);
        }
        // Iteratively clamp to minimum: sectors below min are set to min,
        // and the remaining sectors are scaled down to fit.
        var clamped = new Array(children.length).fill(false);
        for (var ci = 0; ci < children.length; ci++) {
            var totalUnclamped = 0;
            var unclampedWeight = 0;
            for (var cj = 0; cj < children.length; cj++) {
                if (!clamped[cj]) {
                    totalUnclamped += rawSpans[cj];
                    unclampedWeight += weights[cj];
                }
            }
            if (totalUnclamped <= 0 || unclampedWeight <= 0) break;
            // Find the smallest unclamped sector.
            var minIdx = -1, minVal = Infinity;
            for (var ck = 0; ck < children.length; ck++) {
                if (clamped[ck]) continue;
                if (rawSpans[ck] < minVal) { minVal = rawSpans[ck]; minIdx = ck; }
            }
            if (minIdx < 0) break;
            if (minVal >= minNodeSpan) break;
            // Clamp this sector to minNodeSpan.
            clamped[minIdx] = true;
            rawSpans[minIdx] = minNodeSpan;
            // Redistribute remaining space among unclamped sectors.
            var remaining = fullCircle;
            for (var cr = 0; cr < children.length; cr++) {
                if (clamped[cr]) remaining -= rawSpans[cr];
            }
            for (var cu = 0; cu < children.length; cu++) {
                if (!clamped[cu]) {
                    rawSpans[cu] = (weights[cu] / unclampedWeight) * remaining;
                }
            }
        }

        var angle = -Math.PI / 2;
        for (var j = 0; j < children.length; j++) {
            var span = Math.max(rawSpans[j], minNodeSpan);
            var gap = Math.min(MIN_ANGULAR_GAP, span * 0.1);
            assignSector(children[j].id, 0, 0, ringRadii[1],
                angle + gap / 2, angle + span - gap / 2, 1, ringRadii, meta, spacing);
            angle += span;
        }
        applyUserPositions();
    }

    // ---- Pass 1: bottom-up metadata ----------------------------------------

    function computeMeta(id, meta, nodeDepth) {
        var children = RV.getChildren(id);
        var leaves = 1;
        var maxSubtreeDepth = 0;
        if (S.expanded[id] && children.length > 0) {
            leaves = 0;
            for (var i = 0; i < children.length; i++) {
                computeMeta(children[i].id, meta, nodeDepth + 1);
                var cm = meta[children[i].id];
                leaves += cm.leaves;
                if (cm.subtreeDepth + 1 > maxSubtreeDepth) maxSubtreeDepth = cm.subtreeDepth + 1;
            }
        }
        meta[id] = { leaves: Math.max(1, leaves), nodeDepth: nodeDepth, subtreeDepth: maxSubtreeDepth };
    }

    // ---- Pass 2: top-down sector assignment --------------------------------

    function assignSector(id, cx, cy, radius, startAngle, endAngle, depth, ringRadii, meta, spacing) {
        var mid = (startAngle + endAngle) / 2;
        var x = cx + Math.cos(mid) * radius;
        var y = cy + Math.sin(mid) * radius;
        S.positions[id] = { x: x, y: y };

        if (!S.expanded[id]) return;
        var children = RV.getChildren(id);
        if (children.length === 0) return;

        var span = endAngle - startAngle;
        var childDepth = depth + 1;
        var childRadius = ringRadii[childDepth] || (radius + RV.RADIAL_RING * spacing);

        // Allocate child sectors proportional to leaf count.
        var weights = [];
        var totalWeight = 0;
        for (var w = 0; w < children.length; w++) {
            var lw = meta[children[w].id] ? meta[children[w].id].leaves : 1;
            weights.push(lw);
            totalWeight += lw;
        }
        if (totalWeight === 0) totalWeight = 1;

        // Minimum angular span for a single node at the child ring radius.
        var minNodeSpan = (RV.NODE_W + RV.GAP) / childRadius;

        // Compute raw spans, then clamp to minimum and renormalize.
        var rawSpans = [];
        for (var ri = 0; ri < children.length; ri++) {
            rawSpans.push((weights[ri] / totalWeight) * span);
        }
        var clamped = new Array(children.length).fill(false);
        for (var ci = 0; ci < children.length; ci++) {
            var unclampedWeight = 0;
            for (var cj = 0; cj < children.length; cj++) {
                if (!clamped[cj]) unclampedWeight += weights[cj];
            }
            if (unclampedWeight <= 0) break;
            var minIdx = -1, minVal = Infinity;
            for (var ck = 0; ck < children.length; ck++) {
                if (clamped[ck]) continue;
                if (rawSpans[ck] < minVal) { minVal = rawSpans[ck]; minIdx = ck; }
            }
            if (minIdx < 0 || minVal >= minNodeSpan) break;
            clamped[minIdx] = true;
            rawSpans[minIdx] = minNodeSpan;
            var remaining = span;
            for (var cr = 0; cr < children.length; cr++) {
                if (clamped[cr]) remaining -= rawSpans[cr];
            }
            for (var cu = 0; cu < children.length; cu++) {
                if (!clamped[cu]) {
                    rawSpans[cu] = (weights[cu] / unclampedWeight) * remaining;
                }
            }
        }

        var cursor = startAngle;
        for (var j = 0; j < children.length; j++) {
            var childSpan = Math.max(rawSpans[j], minNodeSpan);
            var gap = Math.min(MIN_ANGULAR_GAP, childSpan * 0.1);
            assignSector(children[j].id, cx, cy, childRadius,
                cursor + gap / 2, cursor + childSpan - gap / 2,
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

    RV.layouts = RV.layouts || {};
    RV.layouts.radial = compute;
})(window.RV = window.RV || {});
