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

        var bubble = {};  // id -> farthest distance from node to any descendant

        function computeBubble(id) {
            if (bubble[id] !== undefined) return bubble[id];
            var kids = getDisplayChildren(id);
            if (kids.length === 0) {
                bubble[id] = Math.max(RV.NODE_W, RV.NODE_H) / 2 + RV.GAP;
                return bubble[id];
            }
            // Local circle radius for direct children.
            var localR = kids.length * (RV.NODE_W + RV.GAP) / FULL_CIRCLE;
            localR = Math.max(localR, Math.max(RV.NODE_W, RV.NODE_H) + RV.GAP) * spacing;
            // Find farthest child.
            var maxChild = 0;
            for (var i = 0; i < kids.length; i++) {
                var cb = computeBubble(kids[i].id);
                // Distance from this node to farthest descendant through child i.
                var d = localR + cb;
                if (d > maxChild) maxChild = d;
            }
            bubble[id] = Math.max(localR, maxChild);
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
        var simIter = 80;
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

        // Physics simulation.
        for (var iter = 0; iter < simIter; iter++) {
            var fx = new Array(childBubbles.length).fill(0);
            var fy = new Array(childBubbles.length).fill(0);
            var maxMove = 0;

            // Repulsion between every pair.
            for (var i = 0; i < childBubbles.length; i++) {
                for (var j = i + 1; j < childBubbles.length; j++) {
                    var bi = childBubbles[i], bj = childBubbles[j];
                    var dx = bj.x - bi.x;
                    var dy = bj.y - bi.y;
                    var dist = Math.sqrt(dx * dx + dy * dy);
                    var desired = bi.r + bj.r + minDist;
                    if (dist < desired && dist > 0.01) {
                        var overlap = desired - dist;
                        var force = overlap / desired * 0.5;  // normalised push
                        var nx = dx / dist, ny = dy / dist;
                        fx[i] -= nx * force;
                        fy[i] -= ny * force;
                        fx[j] += nx * force;
                        fy[j] += ny * force;
                    }
                }
            }

            // Centering force (pull toward root).
            for (var i = 0; i < childBubbles.length; i++) {
                var bi = childBubbles[i];
                var d = Math.sqrt(bi.x * bi.x + bi.y * bi.y);
                if (d > 1) {
                    var centerPull = d / (bi.r + initRadius) * 0.1;
                    fx[i] -= (bi.x / d) * centerPull;
                    fy[i] -= (bi.y / d) * centerPull;
                }
            }

            // Damping: reduce as simulation progresses.
            var damping = Math.max(0.05, 1 - iter / simIter);
            var moveScale = Math.max(initRadius, 1) * damping;

            // Apply forces.
            for (var i = 0; i < childBubbles.length; i++) {
                var bi = childBubbles[i];
                var m = Math.sqrt(fx[i] * fx[i] + fy[i] * fy[i]);
                if (m > 1) { fx[i] /= m; fy[i] /= m; }  // clamp magnitude
                var mx = fx[i] * moveScale;
                var my = fy[i] * moveScale;
                bi.x += mx;
                bi.y += my;
                var move = Math.sqrt(mx * mx + my * my);
                if (move > maxMove) maxMove = move;
            }

            // Recompute angles for the next iteration.
            for (var i = 0; i < childBubbles.length; i++) {
                childBubbles[i].angle = Math.atan2(childBubbles[i].y, childBubbles[i].x);
            }

            if (maxMove < 0.5) break;  // converged
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
        resolveOverlaps();

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

    function resolveOverlaps() {
        var ids = Object.keys(S.positions);
        var n = ids.length;
        if (n < 2) return;
        var NW = RV.NODE_W, NH = RV.NODE_H;

        for (var iter = 0; iter < 8; iter++) {
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
                            var ddx = Math.abs(pp.x - qp.x);
                            var ddy = Math.abs(pp.y - qp.y);
                            if (ddx < NW && ddy < NH) {
                                var pAngle = Math.atan2(pp.y, pp.x);
                                var qAngle = Math.atan2(qp.y, qp.x);
                                var overlapX = NW - ddx;
                                var overlapY = NH - ddy;
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

    // ---- Exposed helpers ----------------------------------------------------

    RV.getDisplayLabel = function (id) {
        return displayLabel[id] || null;
    };

    RV.isCollapsed = function (id) {
        return collapsed[id] || null;   // returns the display-parent id, or null
    };

    // Called during drag to reposition a module and all its descendants live.
    RV.radialDragNode = function (id, x, y, spacing) {
        if (S.mode !== 'radial') return;
        if (typeof spacing === 'undefined') spacing = S.radialSpacing || 1.0;
        S.positions[id] = { x: x, y: y };
        placeCollapsedRecursive(id, x, y);
        placeLocalChildren(id, x, y, spacing);
        for (var did in collapsed) {
            if (collapsed[did] === id) {
                S.positions[did] = { x: x, y: y };
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
