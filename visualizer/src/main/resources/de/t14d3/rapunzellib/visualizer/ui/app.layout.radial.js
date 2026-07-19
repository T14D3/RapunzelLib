/* =====================================================================
 * Codebase Visualizer - radial layout (physics bubble-packing)
 *
 * Each node's subtree is surrounded by a "bubble" (a circle whose
 * radius equals the farthest distance from the node to any descendant).
 * A simple physics simulation pushes overlapping bubbles apart while
 * a centering force keeps the graph compact.
 *
 * This produces a natural, organic layout where:
 *   - Large subtrees (many packages) are placed farther from centre
 *   - Small subtrees sit closer to centre
 *   - No wasted space between concentric rings
 *   - No cross-subtree overlap
 *   - The spacing slider controls overall tightness
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;
    var FULL_CIRCLE = 2 * Math.PI;
    var collapsed = {};
    var displayLabel = {};

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

        // ---- Phase 1: compute subtree bubble radii -----------------------

        var bubble = {};  // id -> radius of the circle that encloses the entire subtree

        function computeBubble(id) {
            if (bubble[id] !== undefined) return bubble[id];
            var kids = getDisplayChildren(id);
            if (kids.length === 0) {
                bubble[id] = Math.max(RV.NODE_W, RV.NODE_H) / 2 + RV.GAP;
                return bubble[id];
            }
            // Local circle radius for direct children.
            var localR = localCircleRadius(kids.length, spacing);
            // For each child, the farthest point of that child's subtree from
            // THIS node is: localR (distance to child centre) + child's bubble
            // radius (distance from child centre to its farthest descendant).
            var maxExtent = localR;
            for (var i = 0; i < kids.length; i++) {
                var cb = computeBubble(kids[i].id);
                var extent = localR + cb;
                if (extent > maxExtent) maxExtent = extent;
            }
            bubble[id] = maxExtent;
            return bubble[id];
        }

        // Compute bubbles for all root children (modules).
        var childBubbles = [];    // { id, r, angle }
        for (var i = 0; i < rootChildren.length; i++) {
            var r = computeBubble(rootChildren[i].id);
            childBubbles.push({ id: rootChildren[i].id, r: r, angle: 0, x: 0, y: 0 });
        }

        // ---- Phase 2: physics simulation to place modules -----------------

        // Heuristic: total angular space needed is sum of (2*bubble[i]) / avg_r.
        // We use this to set initial radii and run a simple simulation.
        var simIter = 200;
        var minDist = RV.NODE_W + RV.GAP;  // minimum centre-to-centre distance

        // Initialise: place modules evenly on a circle tight enough that
        // they must push apart.
        var totalR = 0;
        for (var i = 0; i < childBubbles.length; i++) totalR += childBubbles[i].r;
        var initRadius = Math.max(
            totalR / childBubbles.length * 0.6,
            childBubbles.length * minDist / FULL_CIRCLE
        ) * spacing;

        // Spread modules evenly around the initial circle.
        for (var i = 0; i < childBubbles.length; i++) {
            var a = -Math.PI / 2 + i * FULL_CIRCLE / childBubbles.length;
            childBubbles[i].angle = a;
            childBubbles[i].x = Math.cos(a) * initRadius;
            childBubbles[i].y = Math.sin(a) * initRadius;
        }

        // Physics simulation: push overlapping bubbles apart.
        for (var iter = 0; iter < simIter; iter++) {
            var fx = new Array(childBubbles.length).fill(0);
            var fy = new Array(childBubbles.length).fill(0);
            var maxMove = 0;

            // Repulsion between every pair of bubbles.
            for (var i = 0; i < childBubbles.length; i++) {
                for (var j = i + 1; j < childBubbles.length; j++) {
                    var bi = childBubbles[i], bj = childBubbles[j];
                    var dx = bj.x - bi.x;
                    var dy = bj.y - bi.y;
                    var dist = Math.sqrt(dx * dx + dy * dy);
                    // Bubbles must not overlap: desired = sum of radii + gap.
                    var desired = bi.r + bj.r + minDist;
                    if (dist < desired && dist > 0.01) {
                        var overlap = desired - dist;
                        // Stronger force: proportional to overlap, not normalised.
                        var force = overlap * 0.5;
                        var nx = dx / dist, ny = dy / dist;
                        fx[i] -= nx * force;
                        fy[i] -= ny * force;
                        fx[j] += nx * force;
                        fy[j] += ny * force;
                    }
                }
            }

            // Centering force (pull toward root) - weak, to keep compact.
            for (var i = 0; i < childBubbles.length; i++) {
                var bi = childBubbles[i];
                var d = Math.sqrt(bi.x * bi.x + bi.y * bi.y);
                if (d > 1) {
                    var centerPull = 0.02;
                    fx[i] -= (bi.x / d) * centerPull * d;
                    fy[i] -= (bi.y / d) * centerPull * d;
                }
            }

            // Damping: reduce as simulation progresses.
            var damping = Math.max(0.05, 1 - iter / simIter);

            // Apply forces.
            for (var i = 0; i < childBubbles.length; i++) {
                var bi = childBubbles[i];
                var mx = fx[i] * damping;
                var my = fy[i] * damping;
                // Clamp max move per iteration to avoid oscillation.
                var maxStep = bi.r * 0.25;
                var m = Math.sqrt(mx * mx + my * my);
                if (m > maxStep) { mx = mx / m * maxStep; my = my / m * maxStep; }
                bi.x += mx;
                bi.y += my;
                var move = Math.sqrt(mx * mx + my * my);
                if (move > maxMove) maxMove = move;
            }

            // Recompute angles for the next iteration.
            for (var i = 0; i < childBubbles.length; i++) {
                childBubbles[i].angle = Math.atan2(childBubbles[i].y, childBubbles[i].x);
            }

            if (maxMove < 0.1) break;  // converged
        }

        // ---- Phase 3: place all nodes ------------------------------------

        // Place root children (modules) at their simulated positions.
        for (var i = 0; i < childBubbles.length; i++) {
            var cb = childBubbles[i];
            S.positions[cb.id] = { x: cb.x, y: cb.y };
            placeCollapsedRecursive(cb.id, cb.x, cb.y);

            // Place this module's children locally.
            placeLocalChildren(cb.id, cb.x, cb.y, spacing);
        }

        // ---- Post-pass: resolve any remaining overlaps --------------------
        resolveOverlaps(childBubbles, spacing);

        applyUserPositions();
    }
    RV.layouts = RV.layouts || {};
    RV.layouts.radial = compute;

    // ---- Chain collapsing (unchanged) -------------------------------------

    function collapseChains() {
        collapsed = {};
        displayLabel = {};
        function walk(id, chain) {
            var node = S.nodesById[id];
            var label = node && node.label ? node.label : id;
            if (!S.expanded[id]) { assignCollapsed(chain); return; }
            var children = RV.getChildren(id);
            if (children.length === 0) { assignCollapsed(chain); return; }
            if (children.length === 1 && id !== S.rootId) {
                walk(children[0].id, chain.concat([{ id: id, label: label }]));
            } else {
                assignCollapsed(chain, id);
                for (var i = 0; i < children.length; i++) walk(children[i].id, []);
            }
        }
        function assignCollapsed(chain, nextId) {
            if (chain.length === 0 && !nextId) return;
            if (chain.length === 0) return;
            var displayId = chain[0].id;
            var labels = [chain[0].label];
            for (var i = 1; i < chain.length; i++) {
                collapsed[chain[i].id] = displayId;
                labels.push(chain[i].label);
            }
            if (nextId) {
                collapsed[nextId] = displayId;
                var nn = S.nodesById[nextId];
                if (nn && nn.label) labels.push(nn.label);
            }
            displayLabel[displayId] = labels.join(' / ');
        }
        walk(S.rootId, []);
    }

    // ---- Display-tree helpers ---------------------------------------------

    function getDisplayChildren(id) {
        if (!S.expanded[id]) return [];
        var children = RV.getChildren(id);
        var out = [];
        for (var i = 0; i < children.length; i++) {
            var cid = children[i].id;
            if (collapsed[cid]) {
                var grandkids = getDisplayChildren(cid);
                for (var g = 0; g < grandkids.length; g++) out.push(grandkids[g]);
            } else {
                out.push(children[i]);
            }
        }
        return out;
    }

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

    function localCircleRadius(n, spacing) {
        var r = n * (RV.NODE_W + RV.GAP) / FULL_CIRCLE;
        return Math.max(r, Math.max(RV.NODE_W, RV.NODE_H) + RV.GAP) * (spacing || 1);
    }

    function placeLocalChildren(id, px, py, spacing) {
        if (!S.expanded[id]) return;
        var children = getDisplayChildren(id);
        var n = children.length;
        if (n === 0) return;

        var r = localCircleRadius(n, spacing);
        var step = FULL_CIRCLE / n;
        var a = -Math.PI / 2 + step / 2;

        for (var j = 0; j < n; j++) {
            var cx = px + Math.cos(a) * r;
            var cy = py + Math.sin(a) * r;
            S.positions[children[j].id] = { x: cx, y: cy };
            placeCollapsedRecursive(children[j].id, cx, cy);
            placeLocalChildren(children[j].id, cx, cy, spacing);
            a += step;
        }
    }

    // ---- Post-pass: resolve remaining overlaps -----------------------------
    // Instead of pushing individual nodes (which destroys local-circle
    // structure), we detect overlaps between nodes of DIFFERENT subtrees
    // and push the corresponding module (subtree root) apart, then re-place
    // children.  This preserves the circular structure within each subtree.

    function resolveOverlaps(childBubbles, spacing) {
        // Build a map: node id -> module id (the root child that owns it).
        var ownerModule = {};
        for (var i = 0; i < childBubbles.length; i++) {
            var modId = childBubbles[i].id;
            ownerModule[modId] = modId;
            var desc = RV.getDescendantIds(modId);
            for (var d = 0; d < desc.length; d++) ownerModule[desc[d]] = modId;
        }

        var moduleById = {};
        for (var i = 0; i < childBubbles.length; i++) moduleById[childBubbles[i].id] = childBubbles[i];

        var NW = RV.NODE_W, NH = RV.NODE_H;
        var ids = Object.keys(S.positions);

        for (var iter = 0; iter < 10; iter++) {
            // Grid for broad-phase overlap detection.
            var grid = {};
            for (var i = 0; i < ids.length; i++) {
                var p = S.positions[ids[i]];
                if (!p) continue;
                var key = Math.floor(p.x / NW) + ',' + Math.floor(p.y / NH);
                if (!grid[key]) grid[key] = [];
                grid[key].push(ids[i]);
            }

            // Accumulate push vectors per module.
            var modPushX = {}, modPushY = {};
            for (var i = 0; i < childBubbles.length; i++) {
                modPushX[childBubbles[i].id] = 0;
                modPushY[childBubbles[i].id] = 0;
            }

            var anyOverlap = false;

            for (var i = 0; i < ids.length; i++) {
                var pid = ids[i];
                var pp = S.positions[pid];
                if (!pp) continue;
                var modI = ownerModule[pid];
                if (!modI) continue;
                var gx = Math.floor(pp.x / NW);
                var gy = Math.floor(pp.y / NH);

                for (var dx = -1; dx <= 1; dx++) {
                    for (var dy = -1; dy <= 1; dy++) {
                        var bucket = grid[(gx + dx) + ',' + (gy + dy)];
                        if (!bucket) continue;
                        for (var k = 0; k < bucket.length; k++) {
                            var qid = bucket[k];
                            if (qid === pid) continue;
                            var modQ = ownerModule[qid];
                            if (!modQ || modQ === modI) continue;  // same subtree: skip
                            var qp = S.positions[qid];
                            if (!qp) continue;
                            var ddx = pp.x - qp.x;
                            var ddy = pp.y - qp.y;
                            var adx = Math.abs(ddx), ady = Math.abs(ddy);
                            if (adx < NW && ady < NH) {
                                anyOverlap = true;
                                var overlapX = NW - adx;
                                var overlapY = NH - ady;
                                // Push the two modules apart along the axis of least resistance.
                                if (overlapX < overlapY) {
                                    var sign = ddx >= 0 ? 1 : -1;
                                    modPushX[modI] += sign * overlapX * 0.5;
                                    modPushX[modQ] -= sign * overlapX * 0.5;
                                } else {
                                    var sign = ddy >= 0 ? 1 : -1;
                                    modPushY[modI] += sign * overlapY * 0.5;
                                    modPushY[modQ] -= sign * overlapY * 0.5;
                                }
                            }
                        }
                    }
                }
            }

            if (!anyOverlap) break;

            // Apply pushes to modules and re-place their children.
            for (var i = 0; i < childBubbles.length; i++) {
                var cb = childBubbles[i];
                var px = modPushX[cb.id], py = modPushY[cb.id];
                if (px === 0 && py === 0) continue;
                cb.x += px;
                cb.y += py;
                S.positions[cb.id] = { x: cb.x, y: cb.y };
                placeCollapsedRecursive(cb.id, cb.x, cb.y);
                placeLocalChildren(cb.id, cb.x, cb.y, spacing);
            }
        }
    }

    // ---- Exposed helpers ----------------------------------------------------

    RV.getDisplayLabel = function (id) {
        return displayLabel[id] || null;
    };

    RV.isCollapsed = function (id) {
        return collapsed[id] || null;   // returns the display-parent id, or null
    };

    // Called during drag to reposition a node and all its descendants live.
    RV.radialDragNode = function (id, x, y, spacing) {
        if (S.mode !== 'radial') return;
        if (typeof spacing === 'undefined') spacing = S.radialSpacing || 1.0;
        S.positions[id] = { x: x, y: y };
        // Reposition collapsed children (they share our position) AND their
        // non-collapsed children (which sit on a local circle around the
        // collapsed node, which is now at our position).
        placeCollapsedRecursive(id, x, y);
        // Place our own display children on a local circle.
        placeLocalChildren(id, x, y, spacing);
        // Place children of collapsed nodes (they inherit our position, so
        // their children must be placed relative to us).
        var children = RV.getChildren(id);
        for (var i = 0; i < children.length; i++) {
            var cid = children[i].id;
            if (collapsed[cid]) {
                // This child is collapsed into us - its children need to be
                // placed on a local circle around OUR position.
                placeLocalChildren(cid, x, y, spacing);
            }
        }
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
