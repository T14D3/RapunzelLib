/* =====================================================================
 * Codebase Visualizer - cluster layout (force-directed, module grouping)
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    function compute() {
        S = RV.state;
        S.positions = {};
        seed();
        applyUserPositions();
        if (!S.sim) S.sim = newSim();
        S.sim.init();
        S.sim.run(220);
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

        function step() {
            var N = nodes.length;
            var cap = Math.min(N, 1500);
            for (var i = 0; i < cap; i++) {
                var a = nodes[i];
                for (var j = i + 1; j < cap; j++) {
                    var b = nodes[j];
                    var dx = a.x - b.x;
                    var dy = a.y - b.y;
                    var d2 = dx * dx + dy * dy + 0.01;
                    var d = Math.sqrt(d2);
                    var rep = 1400 / d2;
                    var fx = (dx / d) * rep;
                    var fy = (dy / d) * rep;
                    a.vx += fx; a.vy += fy;
                    b.vx -= fx; b.vy -= fy;
                }
            }
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
            for (var n = 0; n < N; n++) {
                var nd = nodes[n];
                if (nd.fixed) { nd.vx = 0; nd.vy = 0; continue; }
                nd.vx *= 0.85; nd.vy *= 0.85;
                nd.x += nd.vx * alpha;
                nd.y += nd.vy * alpha;
            }
            alpha *= 0.99;
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

        function run(iters) {
            for (var i = 0; i < iters; i++) step();
            commit();
        }

        function commit() {
            for (var i = 0; i < nodes.length; i++) {
                var nd = nodes[i];
                S.positions[nd.id] = { x: nd.x, y: nd.y };
            }
        }

        return { init: init, step: step, run: run, commit: commit };
    }

    RV.layouts = RV.layouts || {};
    RV.layouts.cluster = compute;
})(window.RV = window.RV || {});
