/* =====================================================================
 * Codebase Visualizer - cluster layout (force-directed, module grouping)
 *
 * Uses a numerically stable force simulation with:
 *   - a spatial-hash-grid O(N) repulsion approximation (scales to 20k+ nodes)
 *   - bounded per-step displacement (prevents NaN explosions)
 *   - contains-springs to keep parent/child proximity
 *   - module-center gravity to group modules
 *   - a cooling schedule (alpha decay) that converges
 *
 * KEY: The simulation runs asynchronously via requestAnimationFrame to
 * avoid blocking the main thread for seconds. A loading overlay is
 * shown during computation. The compute() function returns a Promise
 * that resolves when layout is complete.
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    // How many simulation steps per requestAnimationFrame batch.
    var STEPS_PER_FRAME = 15;
    // Total iterations (reduced from 300 - with faster decay, 150 is enough).
    var TOTAL_ITERATIONS = 150;

    function compute() {
        S = RV.state;
        S.positions = {};
        seed();
        applyUserPositions();
        if (!S.sim) S.sim = newSim();
        S.sim.init();

        // Show loading overlay.
        showLoading();

        // Run asynchronously via RAF.
        var stepsDone = 0;
        return new Promise(function (resolve) {
            function batch() {
                var start = performance.now();
                // Run as many steps as we can in ~8ms to leave time for paint.
                var budget = 8; // ms
                while (stepsDone < TOTAL_ITERATIONS && (performance.now() - start) < budget) {
                    S.sim.step();
                    stepsDone++;
                }
                if (stepsDone >= TOTAL_ITERATIONS) {
                    S.sim.commit();
                    hideLoading();
                    resolve();
                } else {
                    requestAnimationFrame(batch);
                }
            }
            requestAnimationFrame(batch);
        });
    }

    function showLoading() {
        var el = document.getElementById('layout-loading');
        if (!el) {
            el = document.createElement('div');
            el.id = 'layout-loading';
            el.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);' +
                'color:#c9d1d9;font-family:sans-serif;font-size:14px;' +
                'background:rgba(13,17,23,0.85);padding:16px 24px;border-radius:8px;' +
                'border:1px solid #30363d;z-index:1000;pointer-events:none;';
            el.textContent = 'Processing cluster layout\u2026';
            document.body.appendChild(el);
        }
        el.style.display = '';
    }

    function hideLoading() {
        var el = document.getElementById('layout-loading');
        if (el) el.style.display = 'none';
    }

    function seed() {
        if (!S.rootId) return;
        S.positions[S.rootId] = { x: 0, y: 0 };
        var modules = RV.getChildren(S.rootId);
        var cols = Math.ceil(Math.sqrt(modules.length));
        var spacing = 700;
        for (var i = 0; i < modules.length; i++) {
            var mx = ((i % cols) * spacing) - (cols - 1) * spacing / 2;
            var my = (Math.floor(i / cols) * spacing) -
                (Math.floor((modules.length - 1) / cols) * spacing) / 2;
            S.positions[modules[i].id] = { x: mx, y: my };
            seedSubtree(modules[i].id, mx, my, 1);
        }
    }

    function seedSubtree(id, cx, cy, depth) {
        var children = RV.getChildren(id);
        for (var i = 0; i < children.length; i++) {
            var angle = (i / Math.max(1, children.length)) * Math.PI * 2;
            var r = 80 * depth;
            var x = cx + Math.cos(angle) * r + (Math.random() - 0.5) * 40;
            var y = cy + Math.sin(angle) * r + (Math.random() - 0.5) * 40;
            S.positions[children[i].id] = { x: x, y: y };
            seedSubtree(children[i].id, x, y, depth + 1);
        }
    }

    function applyUserPositions() {
        for (var id in S.userPositions) {
            if (!S.userPositions.hasOwnProperty(id)) continue;
            if (S.nodesById[id]) S.positions[id] = S.userPositions[id];
        }
    }

    // ---- Force simulation --------------------------------------------------

    var MAX_DISPLACEMENT = 50;
    var REPULSION_RADIUS = 120;
    var REPULSION_STRENGTH = 800;

    function newSim() {
        var nodes = [];
        var nodeIndex = {};
        var containsSprings = [];
        var crossSprings = [];
        var moduleCenters = {};
        var alpha = 1;

        function init() {
            nodes = []; nodeIndex = {};
            containsSprings = []; crossSprings = [];
            moduleCenters = {};

            for (var id in S.positions) {
                if (!S.positions.hasOwnProperty(id)) continue;
                var node = S.nodesById[id];
                if (!node) continue;
                var p = S.positions[id];
                if (!isFinite(p.x) || !isFinite(p.y) || isNaN(p.x) || isNaN(p.y)) {
                    p = { x: (Math.random() - 0.5) * 200, y: (Math.random() - 0.5) * 200 };
                    S.positions[id] = p;
                }
                var entry = {
                    id: id, x: p.x, y: p.y, vx: 0, vy: 0,
                    fixed: !!S.pinned[id] || !!S.userPositions[id],
                    module: RV.moduleOf(id)
                };
                nodes.push(entry);
                nodeIndex[id] = entry;
            }

            for (var i = 0; i < S.graph.edges.length; i++) {
                var e = S.graph.edges[i];
                if (e.type !== 'contains') continue;
                var a = nodeIndex[e.source];
                var b = nodeIndex[e.target];
                if (!a || !b) continue;
                containsSprings.push({ a: a, b: b, len: 120, k: 0.04 });
            }
            for (var j = 0; j < S.crossEdges.length; j++) {
                var ce = S.crossEdges[j];
                var ca = nodeIndex[ce.source];
                var cb = nodeIndex[ce.target];
                if (!ca || !cb) continue;
                var group = RV.EDGE_GROUPS[ce.type];
                if (group === 'annotations' || group === 'references') continue;
                crossSprings.push({ a: ca, b: cb, len: 260, k: 0.008 });
            }

            for (var mId in S.moduleMembers) {
                if (!S.moduleMembers.hasOwnProperty(mId)) continue;
                var members = S.moduleMembers[mId];
                var sx = 0, sy = 0, n = 0;
                for (var k = 0; k < members.length; k++) {
                    var ent = nodeIndex[members[k]];
                    if (ent) { sx += ent.x; sy += ent.y; n++; }
                }
                if (n > 0) moduleCenters[mId] = { x: sx / n, y: sy / n };
            }
            alpha = 1;
        }

        function applyRepulsion() {
            var N = nodes.length;
            var cell = REPULSION_RADIUS;
            var grid = {};
            for (var i = 0; i < N; i++) {
                var nd = nodes[i];
                var gx = Math.floor(nd.x / cell);
                var gy = Math.floor(nd.y / cell);
                var key = gx + ',' + gy;
                if (!grid[key]) grid[key] = [];
                grid[key].push(nd);
            }
            for (var n = 0; n < N; n++) {
                var a = nodes[n];
                var agx = Math.floor(a.x / cell);
                var agy = Math.floor(a.y / cell);
                for (var dx = -1; dx <= 1; dx++) {
                    for (var dy = -1; dy <= 1; dy++) {
                        var bucket = grid[(agx + dx) + ',' + (agy + dy)];
                        if (!bucket) continue;
                        for (var b = 0; b < bucket.length; b++) {
                            var other = bucket[b];
                            if (other === a) continue;
                            var ddx = a.x - other.x;
                            var ddy = a.y - other.y;
                            var d2 = ddx * ddx + ddy * ddy;
                            if (d2 > cell * cell) continue;
                            if (d2 < 1) d2 = 1;
                            var d = Math.sqrt(d2);
                            var force = REPULSION_STRENGTH / d2;
                            if (force > 20) force = 20;
                            var fx = (ddx / d) * force;
                            var fy = (ddy / d) * force;
                            if (!a.fixed) { a.vx += fx; a.vy += fy; }
                        }
                    }
                }
            }
        }

        function step() {
            applyRepulsion();
            for (var s = 0; s < containsSprings.length; s++) applySpring(containsSprings[s]);
            for (var t = 0; t < crossSprings.length; t++) applySpring(crossSprings[t]);

            for (var mId in moduleCenters) {
                if (!moduleCenters.hasOwnProperty(mId)) continue;
                var c = moduleCenters[mId];
                var members = S.moduleMembers[mId];
                for (var mi = 0; mi < members.length; mi++) {
                    var ent = nodeIndex[members[mi]];
                    if (!ent || ent.fixed) continue;
                    ent.vx += (c.x - ent.x) * 0.002 * alpha;
                    ent.vy += (c.y - ent.y) * 0.002 * alpha;
                }
            }

            for (var n = 0; n < nodes.length; n++) {
                var nd = nodes[n];
                if (nd.fixed) { nd.vx = 0; nd.vy = 0; continue; }
                nd.vx *= 0.85; nd.vy *= 0.85;
                var speed = Math.sqrt(nd.vx * nd.vx + nd.vy * nd.vy);
                if (speed > MAX_DISPLACEMENT) {
                    nd.vx = (nd.vx / speed) * MAX_DISPLACEMENT;
                    nd.vy = (nd.vy / speed) * MAX_DISPLACEMENT;
                }
                nd.x += nd.vx * alpha;
                nd.y += nd.vy * alpha;
                if (isNaN(nd.x) || !isFinite(nd.x)) nd.x = 0;
                if (isNaN(nd.y) || !isFinite(nd.y)) nd.y = 0;
            }
            alpha *= 0.97; // faster decay than 0.985, converges in ~150 steps
        }

        function applySpring(sp) {
            var a = sp.a, b = sp.b;
            var dx = b.x - a.x;
            var dy = b.y - a.y;
            var d = Math.sqrt(dx * dx + dy * dy) + 0.01;
            var f = (d - sp.len) * sp.k;
            var fx = (dx / d) * f;
            var fy = (dy / d) * f;
            if (!a.fixed) { a.vx += fx; a.vy += fy; }
            if (!b.fixed) { b.vx -= fx; b.vy -= fy; }
        }

        function commit() {
            for (var i = 0; i < nodes.length; i++) {
                var nd = nodes[i];
                S.positions[nd.id] = { x: nd.x, y: nd.y };
            }
        }

        return { init: init, step: step, commit: commit };
    }

    RV.layouts = RV.layouts || {};
    RV.layouts.cluster = compute;
})(window.RV = window.RV || {});
