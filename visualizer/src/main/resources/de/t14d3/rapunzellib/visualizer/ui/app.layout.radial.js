/* =====================================================================
 * Codebase Visualizer - radial layout (recursive angular sectors)
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

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

        var weights = [];
        var totalWeight = 0;
        for (var i = 0; i < children.length; i++) {
            var w = countLeaves(children[i].id);
            weights.push(w);
            totalWeight += w;
        }
        if (totalWeight === 0) totalWeight = 1;

        // User-adjustable spacing multiplier (1.0 = default, slider range 0.3-3.0)
        var spacing = S.radialSpacing || 1.0;

        // Compute ring radius: base value plus a sqrt-of-weight term so large
        // groups spread out naturally without exploding linearly.
        // sqrt gives a softer spread than a linear term.
        var ringRadius = Math.max(RV.RADIAL_RING,
            RV.RADIAL_RING * spacing * (0.5 + 0.5 * Math.sqrt(totalWeight / Math.max(children.length, 1))));

        var angle = -Math.PI / 2;
        var fullCircle = Math.PI * 2;
        for (var j = 0; j < children.length; j++) {
            var rawSpan = (weights[j] / totalWeight) * fullCircle;
            // Tiny minimum so zero-leaf nodes still get a sliver
            var minSpan = 0.015;
            var span = Math.max(rawSpan, minSpan);
            var gap = Math.min(RV.RADIAL_GAP, span * 0.1);
            assignSector(children[j].id, 0, 0, ringRadius,
                angle + gap / 2, angle + span - gap / 2, 1, spacing);
            angle += span;
        }
        applyUserPositions();
    }

    function countLeaves(id) {
        if (!S.expanded[id]) return 1;
        var children = RV.getChildren(id);
        if (children.length === 0) return 1;
        var sum = 0;
        for (var i = 0; i < children.length; i++) sum += countLeaves(children[i].id);
        return Math.max(1, sum);
    }

    function assignSector(id, cx, cy, radius, startAngle, endAngle, depth, spacing) {
        var mid = (startAngle + endAngle) / 2;
        var x = cx + Math.cos(mid) * radius;
        var y = cy + Math.sin(mid) * radius;
        S.positions[id] = { x: x, y: y };

        if (!S.expanded[id]) return;
        var children = RV.getChildren(id);
        if (children.length === 0) return;

        var weights = [];
        var totalWeight = 0;
        for (var i = 0; i < children.length; i++) {
            var w = countLeaves(children[i].id);
            weights.push(w);
            totalWeight += w;
        }
        if (totalWeight === 0) totalWeight = 1;

        var span = endAngle - startAngle;
        // Child ring radius: parent radius plus a depth increment, scaled by
        // the sqrt of children to spread large groups without exploding.
        spacing = spacing || (S.radialSpacing || 1.0);
        var ringStep = RV.RADIAL_RING * (0.5 + 0.1 * depth) * spacing;
        var childRadius = radius + ringStep * (0.5 + 0.5 * Math.sqrt(totalWeight / Math.max(children.length, 1)));
        var cursor = startAngle;
        for (var j = 0; j < children.length; j++) {
            var rawChildSpan = (weights[j] / totalWeight) * span;
            // Tiny minimum so zero-leaf nodes still get a sliver
            var minSpan = 0.015;
            var childSpan = Math.max(rawChildSpan, minSpan);
            assignSector(children[j].id, cx, cy, childRadius, cursor, cursor + childSpan, depth + 1, spacing);
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
