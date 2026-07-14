/* =====================================================================
 * Codebase Visualizer - focus-layer model
 *
 * Computes hop distance from the selected node across ALL edge types
 * (not just contains). Nodes within focusDepth hops are rendered at
 * full brightness; farther nodes dim progressively.
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    function recompute() {
        S = RV.state;
        S.focusLayer = {};
        if (!S.selected) return;

        var queue = [S.selected];
        S.focusLayer[S.selected] = 0;
        var head = 0;
        while (head < queue.length) {
            var id = queue[head++];
            var d = S.focusLayer[id];
            if (d >= S.focusDepth) continue;
            // Outgoing edges
            var out = S.edgesBySource[id] || [];
            for (var i = 0; i < out.length; i++) {
                var t = out[i].target;
                if (S.focusLayer[t] === undefined) {
                    S.focusLayer[t] = d + 1;
                    queue.push(t);
                }
            }
            // Incoming edges
            var inc = S.edgesByTarget[id] || [];
            for (var j = 0; j < inc.length; j++) {
                var s = inc[j].source;
                if (S.focusLayer[s] === undefined) {
                    S.focusLayer[s] = d + 1;
                    queue.push(s);
                }
            }
        }
    }

    // Returns brightness multiplier (0..1) for a node given the current focus.
    function nodeBrightness(id) {
        S = RV.state;
        if (!S.selected) return 1.0;
        if (id === S.selected) return 1.0;
        var d = S.focusLayer[id];
        if (d === undefined) return RV.FOCUS_FALLOFF[RV.FOCUS_FALLOFF.length - 1];
        return RV.FOCUS_FALLOFF[Math.min(d, RV.FOCUS_FALLOFF.length - 1)];
    }

    // Returns brightness multiplier for an edge given the current focus.
    function edgeBrightness(edge) {
        S = RV.state;
        if (!S.selected) return 1.0;
        var sb = S.focusLayer[edge.source];
        var tb = S.focusLayer[edge.target];
        // Edge is bright if either endpoint is in the focus layer.
        if (sb !== undefined || tb !== undefined) return 1.0;
        return RV.FOCUS_FALLOFF[RV.FOCUS_FALLOFF.length - 1];
    }

    RV.recomputeFocusLayer = recompute;
    RV.nodeBrightness = nodeBrightness;
    RV.edgeBrightness = edgeBrightness;
})(window.RV = window.RV || {});
