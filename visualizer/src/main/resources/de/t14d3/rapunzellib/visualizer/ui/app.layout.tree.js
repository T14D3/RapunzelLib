/* =====================================================================
 * Codebase Visualizer - tree layout (module-aware bands)
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    function compute() {
        S = RV.state;
        if (!S.rootId) return;
        S.positions = {};
        S.positions[S.rootId] = { x: 0, y: 0 };

        var modules = RV.getChildren(S.rootId);
        if (modules.length === 0) return;

        var widths = [];
        var totalW = 0;
        for (var i = 0; i < modules.length; i++) {
            var w = subtreeWidth(modules[i].id);
            widths.push(w);
            totalW += w;
        }
        totalW += RV.GAP * (modules.length - 1);

        var cursorX = -totalW / 2;
        for (var j = 0; j < modules.length; j++) {
            var cx = cursorX + widths[j] / 2;
            assign(modules[j].id, cx, RV.LEVEL_H);
            cursorX += widths[j] + RV.GAP;
        }
    }

    function subtreeWidth(id) {
        if (!S.expanded[id]) return RV.NODE_W;
        var children = RV.getChildren(id);
        if (children.length === 0) return RV.NODE_W;
        var total = 0;
        for (var i = 0; i < children.length; i++) {
            total += subtreeWidth(children[i].id);
        }
        return Math.max(RV.NODE_W, total + RV.GAP * (children.length - 1));
    }

    function assign(id, x, y) {
        S.positions[id] = { x: x, y: y };
        if (!S.expanded[id]) return;
        var children = RV.getChildren(id);
        if (children.length === 0) return;

        var total = 0;
        var widths = [];
        for (var i = 0; i < children.length; i++) {
            var w = subtreeWidth(children[i].id);
            widths.push(w);
            total += w;
        }
        total += RV.GAP * (children.length - 1);

        var startX = x - total / 2;
        for (var j = 0; j < children.length; j++) {
            assign(children[j].id, startX + widths[j] / 2, y + RV.LEVEL_H);
            startX += widths[j] + RV.GAP;
        }
    }

    RV.layouts = RV.layouts || {};
    RV.layouts.tree = compute;
})(window.RV = window.RV || {});
