/* =====================================================================
 * Codebase Visualizer - radial layout (recursive local circles)
 *
 * Instead of placing all nodes at a given depth on a single global ring
 * (which wastes huge amounts of space between rings), each expanded
 * parent becomes the centre of its OWN local circle and arranges its
 * children around itself at a radius proportional to the number of
 * children.
 *
 * This produces an organic, nested-circle layout similar to Obsidian's
 * graph view: each subtree is self-contained, and the layout naturally
 * fills space without leaving massive gaps between levels.
 *
 *   root
 *    │
 *    ├── module A ── sourceSet A1 ── package A1a
 *    │              └── sourceSet A2 ── package A2a
 *    └── module B ── sourceSet B1 ── package B1a
 *                                    package B1b
 *
 * A module with 17 packages packs tightly (small local circle for the
 * sourceSet), while a module with 3 packages packs even tighter - no
 * global depth ring forces everything to the same outer radius.
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    function compute() {
        S = RV.state;
        if (!S.rootId) return;
        S.positions = {};
        // Place root at the origin (or user-defined position).
        S.positions[S.rootId] = { x: 0, y: 0 };
        if (S.userPositions[S.rootId]) {
            S.positions[S.rootId] = { x: S.userPositions[S.rootId].x, y: S.userPositions[S.rootId].y };
        }
        // Recursively place children.
        placeChildren(S.rootId);
        applyUserPositions();
    }
    RV.layouts = RV.layouts || {};
    RV.layouts.radial = compute;

    // ---- Recursive child placement -----------------------------------------

    function placeChildren(id) {
        if (!S.expanded[id]) return;
        var children = RV.getChildren(id);
        var n = children.length;
        if (n === 0) return;

        var spacing = S.radialSpacing || 1.0;

        // Radius of the local circle: exactly enough to seat all children
        // around the parent without angular overlap.
        var arcPerChild = RV.NODE_W + RV.GAP;
        var radius = n * arcPerChild / (2 * Math.PI);
        // Never place children closer than NODE_W + GAP (they'd overlap the
        // parent's bounding box), and never tighter than NODE_H + GAP.
        var minRadius = Math.max(RV.NODE_W, RV.NODE_H) + RV.GAP;
        radius = Math.max(radius, minRadius) * spacing;

        // Equal angular slices around the full circle.
        var angleStep = 2 * Math.PI / n;
        // Start at 12 o'clock so the layout is balanced.
        var angle = -Math.PI / 2;

        var parentX = S.positions[id].x;
        var parentY = S.positions[id].y;

        for (var i = 0; i < n; i++) {
            var child = children[i];
            var cx = parentX + Math.cos(angle) * radius;
            var cy = parentY + Math.sin(angle) * radius;
            S.positions[child.id] = { x: cx, y: cy };

            // Recursively place this child's children.
            placeChildren(child.id);

            angle += angleStep;
        }
    }

    function applyUserPositions() {
        for (var id in S.userPositions) {
            if (!S.userPositions.hasOwnProperty(id)) continue;
            if (S.nodesById[id]) {
                S.positions[id] = {
                    x: S.userPositions[id].x,
                    y: S.userPositions[id].y
                };
            }
        }
    }
})(window.RV = window.RV || {});
