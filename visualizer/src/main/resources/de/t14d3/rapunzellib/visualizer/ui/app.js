/* =====================================================================
 * Codebase Visualizer - app.js (entry point / bootstrap)
 *
 * This file sets up the shared `window.RV` state object and wires
 * together the modules loaded via separate <script> tags:
 *
 *   app.constants.js   - colors, dimensions, edge groups, modes
 *   app.indices.js     - graph indices, tree helpers, expansion, visibility
 *   app.layout.tree.js - tree layout (module-aware bands)
 *   app.layout.radial.js - radial layout (recursive angular sectors)
 *   app.layout.cluster.js - force-directed layout (module grouping)
 *   app.focus.js       - focus-layer model (N-hop dimming)
 *   app.render.js      - canvas renderer (nodes, edges, minimap, culling)
 *   app.interact.js    - pan/zoom/drag/hover/click/keyboard/tooltip
 *   app.ui.js          - search, filters, layers, mode selector, sliders,
 *                        breadcrumb, detail panel, context menu, fit view
 *
 * All modules attach functions to `window.RV`. This file defines the
 * shared `RV.state`, the `computeLayout` dispatcher, canvas setup,
 * persistence, and the init/start sequence.
 * ===================================================================== */
(function (RV) {
    'use strict';

    // ---- Shared state ------------------------------------------------------
    // Defined here so every module loaded afterwards sees it via RV.state.
    RV.state = {
        graph: null,
        nodesById: {},
        childrenByParent: {},
        parentByChild: {},
        edgesBySource: {},
        edgesByTarget: {},
        crossEdges: [],
        moduleMembers: {},
        rootId: null,

        expanded: {},
        selected: null,
        hoveredNode: null,
        hoveredEdge: null,

        mode: 'tree',
        radialSpacing: 1.0,
        visibleLayers: {},
        layerAlpha: {},
        filters: { hideExternal: false, hideTest: false, hidePrivate: false },
        excludePathPatterns: [],

        focusDepth: 2,
        expandDepth: 3,

        camera: { x: 0, y: 0 },
        zoom: 1,
        positions: {},
        userPositions: {},
        pinned: {},

        focusLayer: {},

        canvas: null,
        ctx: null,
        cssW: 0,
        cssH: 0,
        dpr: 1,

        minimap: null,
        minimapCtx: null,

        sim: null,

        bounds: { minX: 0, maxX: 0, minY: 0, maxY: 0 }
    };

    var S = RV.state;

    // ---- Helpers -----------------------------------------------------------

    RV.escapeHtml = function (str) {
        if (str == null) return '';
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(String(str)));
        return div.innerHTML;
    };

    // ---- Layout dispatcher -------------------------------------------------

    RV.computeLayout = function () {
        S.positions = {};
        if (S.mode === 'tree') {
            RV.layouts.tree();
            RV.computeBounds();
            if (RV.markRenderDirty) RV.markRenderDirty();
            return Promise.resolve();
        } else if (S.mode === 'radial') {
            RV.layouts.radial();
            RV.computeBounds();
            if (RV.markRenderDirty) RV.markRenderDirty();
            return Promise.resolve();
        } else if (S.mode === 'cluster') {
            var prom = RV.layouts.cluster();
            return prom.then(function () {
                RV.computeBounds();
                if (RV.markRenderDirty) RV.markRenderDirty();
            });
        }
        return Promise.resolve();
    };

    // Async-friendly helpers that chain computeLayout -> optional fn -> render.
    RV.relayoutAndRender = function () {
        return RV.computeLayout().then(function () { RV.render(); });
    };
    RV.relayoutAndThen = function (fn) {
        return RV.computeLayout().then(function () { fn(); RV.render(); });
    };

    // ---- Persistence -------------------------------------------------------

    RV.persistState = function () {
        try {
            var data = {
                mode: S.mode,
                focusDepth: S.focusDepth,
                expandDepth: S.expandDepth,
                userPositions: S.userPositions,
                layerAlpha: S.layerAlpha
            };
            localStorage.setItem(RV.STORAGE_KEY, JSON.stringify(data));
        } catch (e) { /* ignore quota */ }
    };

    function loadPersistedState() {
        try {
            var raw = localStorage.getItem(RV.STORAGE_KEY);
            if (!raw) return;
            var saved = JSON.parse(raw);
            if (saved.mode && isModeKey(saved.mode)) S.mode = saved.mode;
            if (typeof saved.focusDepth === 'number') S.focusDepth = saved.focusDepth;
            if (typeof saved.expandDepth === 'number') S.expandDepth = saved.expandDepth;
            if (saved.userPositions) S.userPositions = saved.userPositions;
            if (saved.layerAlpha) {
                for (var k in saved.layerAlpha) {
                    if (saved.layerAlpha.hasOwnProperty(k)) S.layerAlpha[k] = saved.layerAlpha[k];
                }
            }
        } catch (e) { /* ignore */ }
    }

    function isModeKey(k) {
        for (var i = 0; i < RV.MODES.length; i++) if (RV.MODES[i].key === k) return true;
        return false;
    }

    // ---- Canvas setup ------------------------------------------------------

    function setupCanvas() {
        var canvas = document.getElementById('graph-canvas');
        S.canvas = canvas;
        // NOTE: Do NOT call getContext('2d') here.  A canvas can only have one
        // context type - calling getContext('2d') first would permanently lock
        // it and cause getContext('webgl2') to return null in app.webgl.js.
        // The 2D context is obtained lazily by the fallback renderer if needed.

        var resizeTimer = null;
        function resize() {
            var dpr = Math.min(window.devicePixelRatio || 1, RV.MAX_DPR);
            var rect = canvas.getBoundingClientRect();
            canvas.width = rect.width * dpr;
            canvas.height = rect.height * dpr;
            S.cssW = rect.width;
            S.cssH = rect.height;
            S.dpr = dpr;
            RV.render();
        }
        window.addEventListener('resize', function () {
            if (resizeTimer) clearTimeout(resizeTimer);
            resizeTimer = setTimeout(resize, 80);
        });
        resize();
    }

    function setupMinimap() {
        var mm = document.getElementById('minimap');
        S.minimap = mm;
        S.minimapCtx = mm.getContext('2d');
    }

    // ---- Init --------------------------------------------------------------

    function init() {
        RV.loadGraph().then(function (data) {
            console.log('[RV] init: graph loaded, starting app');
            S.graph = data;
            start();
        }).catch(function (err) {
            console.error('[RV] init: loadGraph FAILED:', err);
            document.body.innerHTML =
                '<div style="padding:40px;color:#c9d1d9;font-family:sans-serif">' +
                '<h2>Failed to load graph data</h2>' +
                '<p>If you opened this file directly, make sure <code>graph-data.js</code> is present next to <code>index.html</code>.</p>' +
                '<pre style="margin-top:12px;color:#f85149;font-size:12px;white-space:pre-wrap">' +
                (err && err.message ? err.message : String(err)) + '</pre>' +
                '</div>';
        });
    }

    function start() {
        loadPersistedState();
        RV.buildIndices();
        RV.initExpansion();
        setupCanvas();
        setupMinimap();
        RV.setupInteraction();
        RV.setupSearch();
        RV.setupFilters();
        RV.setupExcludePaths();
        RV.setupLayerToggles();
        RV.setupModeSelector();
        RV.setupDepthSliders();
        RV.setupRadialSpacing();
        RV.setupButtons();

        // Layout is async for cluster mode (runs via requestAnimationFrame).
        RV.computeLayout().then(function () {
            RV.recomputeFocusLayer();
            RV.updateBreadcrumb();
            RV.fitToView();
            RV.render();
        });
    }

    // ---- Go ---------------------------------------------------------------

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(window.RV = window.RV || {});
