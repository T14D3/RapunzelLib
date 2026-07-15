/* =====================================================================
 * Codebase Visualizer - indices, tree helpers, expansion
 * ===================================================================== */
(function (RV) {
    'use strict';

    // NOTE: RV.state is set by app.js which loads after this module.
    // We cannot capture S eagerly - it must be resolved lazily.
    // buildIndices() is called by app.js after it has set RV.state,
    // so we grab the reference at that point.
    var S;

    function buildIndices() {
        S = RV.state;
        var i, node, edge;
        for (i = 0; i < S.graph.nodes.length; i++) {
            node = S.graph.nodes[i];
            S.nodesById[node.id] = node;
            if (node.type === 'project' && !S.rootId) S.rootId = node.id;
        }
        for (i = 0; i < S.graph.edges.length; i++) {
            edge = S.graph.edges[i];
            if (edge.type === 'contains') {
                if (!S.childrenByParent[edge.source]) S.childrenByParent[edge.source] = [];
                S.childrenByParent[edge.source].push(edge.target);
                S.parentByChild[edge.target] = edge.source;
            } else {
                S.crossEdges.push(edge);
            }
            if (!S.edgesBySource[edge.source]) S.edgesBySource[edge.source] = [];
            S.edgesBySource[edge.source].push(edge);
            if (!S.edgesByTarget[edge.target]) S.edgesByTarget[edge.target] = [];
            S.edgesByTarget[edge.target].push(edge);
        }
        for (i = 0; i < S.graph.nodes.length; i++) {
            node = S.graph.nodes[i];
            if (node.type === 'module') {
                S.moduleMembers[node.id] = collectDescendants(node.id);
            }
        }
    }

    function collectDescendants(id) {
        var out = [];
        var stack = [id];
        var seen = {};
        while (stack.length) {
            var cur = stack.pop();
            if (seen[cur]) continue;
            seen[cur] = true;
            out.push(cur);
            var kids = S.childrenByParent[cur] || [];
            for (var i = 0; i < kids.length; i++) stack.push(kids[i]);
        }
        return out;
    }

    function getChildren(id) {
        var children = S.childrenByParent[id] || [];
        var result = [];
        for (var i = 0; i < children.length; i++) {
            var n = S.nodesById[children[i]];
            if (n) result.push(n);
        }
        return result;
    }

    function initExpansion() {
        S.expanded = {};
        expandToDepth(S.rootId, S.expandDepth, 0);
        for (var i = 0; i < RV.GROUP_ORDER.length; i++) {
            var g = RV.GROUP_ORDER[i];
            if (S.visibleLayers[g] === undefined) S.visibleLayers[g] = true;
            if (S.layerAlpha[g] === undefined) S.layerAlpha[g] = RV.GROUP_DEFAULT_ALPHA[g];
        }
    }

    function expandToDepth(id, depth, current) {
        if (!id || current >= depth) return;
        var node = S.nodesById[id];
        if (!node) return;
        var kids = S.childrenByParent[id] || [];
        if (kids.length === 0) return;
        S.expanded[id] = true;
        for (var i = 0; i < kids.length; i++) {
            expandToDepth(kids[i], depth, current + 1);
        }
    }

    function collapseDescendants(id) {
        var children = S.childrenByParent[id] || [];
        for (var i = 0; i < children.length; i++) {
            delete S.expanded[children[i]];
            collapseDescendants(children[i]);
        }
    }

    function expandDescendants(id) {
        var children = S.childrenByParent[id] || [];
        if (children.length === 0) return;
        S.expanded[id] = true;
        for (var i = 0; i < children.length; i++) {
            expandDescendants(children[i]);
        }
    }

    function expandAncestors(id) {
        var current = S.parentByChild[id];
        while (current) {
            S.expanded[current] = true;
            current = S.parentByChild[current];
        }
    }

    function moduleOf(id) {
        var node = S.nodesById[id];
        if (!node) return null;
        if (node.containingModule) return node.containingModule;
        var cur = S.parentByChild[id];
        while (cur) {
            var n = S.nodesById[cur];
            if (n && n.type === 'module') return cur;
            cur = S.parentByChild[cur];
        }
        return null;
    }

    function isNodeVisible(node) {
        if (!node) return false;
        if (S.filters.hideExternal && node.properties && node.properties.external) return false;
        if (S.filters.hideTest && isTestNode(node)) return false;
        if (S.filters.hidePrivate && hasModifier(node, 'private')) return false;
        if (S.excludePathPatterns.length > 0 && node.sourceFile && matchesExcludePath(node.sourceFile)) return false;
        return true;
    }

    function matchesExcludePath(path) {
        // Normalize to forward slashes for matching.
        var p = path.replace(/\\/g, '/');
        for (var i = 0; i < S.excludePathPatterns.length; i++) {
            if (S._excludeRegexes[i].test(p)) return true;
        }
        return false;
    }

    /**
     * Convert a glob pattern to a RegExp. Supports:
     *   **  - matches any sequence of characters (including /)
     *   *   - matches any sequence except /
     *   ?   - matches a single character except /
     */
    RV.globToRegex = function (glob) {
        var s = glob.trim();
        var out = '';
        var i = 0;
        while (i < s.length) {
            var c = s.charAt(i);
            if (c === '*' && i + 1 < s.length && s.charAt(i + 1) === '*') {
                out += '.*';
                i += 2;
            } else if (c === '*') {
                out += '[^/]*';
                i++;
            } else if (c === '?') {
                out += '[^/]';
                i++;
            } else if ('.+()[]{}^$|\\'.indexOf(c) >= 0) {
                out += '\\' + c;
                i++;
            } else {
                out += c;
                i++;
            }
        }
        return new RegExp(out, 'i');
    };

    /**
     * Update the exclude-path patterns from a comma-separated string.
     * Compiles regexes once and stores them in S._excludeRegexes.
     */
    RV.updateExcludePatterns = function (csv) {
        S = RV.state;
        var parts = (csv || '').split(',').map(function (p) { return p.trim(); }).filter(function (p) { return p.length > 0; });
        S.excludePathPatterns = parts;
        S._excludeRegexes = parts.map(function (p) { return RV.globToRegex(p); });
    };

    function isEdgeVisible(edge) {
        var group = RV.EDGE_GROUPS[edge.type];
        if (!group || !S.visibleLayers[group]) return false;
        var src = S.nodesById[edge.source];
        var tgt = S.nodesById[edge.target];
        return isNodeVisible(src) && isNodeVisible(tgt);
    }

    function isTestNode(node) {
        if (node.type === 'sourceSet') return node.simpleName === 'test';
        if (node.sourceFile && node.sourceFile.indexOf('/test/') >= 0) return true;
        return false;
    }

    function hasModifier(node, mod) {
        return node && node.properties && node.properties.modifiers &&
            node.properties.modifiers.indexOf(mod) >= 0;
    }

    function getDescendantIds(id) {
        var out = [];
        var stack = [id];
        var seen = {};
        while (stack.length) {
            var cur = stack.pop();
            if (seen[cur]) continue;
            seen[cur] = true;
            if (cur !== id) out.push(cur);
            var kids = S.childrenByParent[cur] || [];
            for (var i = 0; i < kids.length; i++) stack.push(kids[i]);
        }
        return out;
    }

    function ancestorPath(id) {
        var path = [];
        var cur = id;
        var seen = {};
        while (cur && !seen[cur]) {
            seen[cur] = true;
            var n = S.nodesById[cur];
            if (n) path.unshift(n);
            cur = S.parentByChild[cur];
        }
        return path;
    }

    RV.buildIndices = buildIndices;
    RV.getChildren = getChildren;
    RV.initExpansion = initExpansion;
    RV.expandToDepth = expandToDepth;
    RV.collapseDescendants = collapseDescendants;
    RV.expandDescendants = expandDescendants;
    RV.expandAncestors = expandAncestors;
    RV.moduleOf = moduleOf;
    RV.isNodeVisible = isNodeVisible;
    RV.isEdgeVisible = isEdgeVisible;
    RV.ancestorPath = ancestorPath;
    RV.getDescendantIds = getDescendantIds;
    RV.matchesExcludePath = matchesExcludePath;
})(window.RV = window.RV || {});
