/* =====================================================================
 * Codebase Visualizer - UI wiring
 *
 * Search, filters, layer toggles + opacity, mode selector, depth
 * sliders, breadcrumb, detail panel, context menu, fit view, buttons.
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    // ---- Search ------------------------------------------------------------

    function setupSearch() {
        S = RV.state;
        var input = document.getElementById('search');
        var results = document.getElementById('search-results');

        input.addEventListener('input', function () {
            var query = input.value.toLowerCase().trim();
            if (query.length < 2) {
                results.classList.remove('visible');
                results.innerHTML = '';
                return;
            }
            var matches = [];
            for (var i = 0; i < S.graph.nodes.length; i++) {
                var node = S.graph.nodes[i];
                if (!RV.isNodeVisible(node)) continue;
                var name = (node.simpleName || '').toLowerCase();
                var qname = (node.qualifiedName || '').toLowerCase();
                if (name.indexOf(query) >= 0 || qname.indexOf(query) >= 0) {
                    matches.push(node);
                    if (matches.length >= 50) break;
                }
            }
            results.innerHTML = '';
            for (var j = 0; j < matches.length; j++) {
                var m = matches[j];
                var el = document.createElement('div');
                el.className = 'search-result';
                var qnameDisplay = m.qualifiedName || '';
                el.innerHTML =
                    '<span class="result-type type-' + m.type + '">' + m.type + '</span>' +
                    '<span class="result-name">' + RV.escapeHtml(m.simpleName || m.id) + '</span>' +
                    '<span class="result-qname">' + RV.escapeHtml(qnameDisplay) + '</span>';
                (function (nodeId) {
                    el.addEventListener('click', function () {
                        RV.focusNode(nodeId);
                        results.classList.remove('visible');
                        input.value = '';
                    });
                })(m.id);
                results.appendChild(el);
            }
            results.classList.add('visible');
        });

        document.addEventListener('click', function (e) {
            if (!input.contains(e.target) && !results.contains(e.target)) {
                results.classList.remove('visible');
            }
        });
    }

    // ---- Filters -----------------------------------------------------------

    function setupFilters() {
        S = RV.state;
        var container = document.getElementById('filters');
        var filters = [
            { key: 'hideExternal', label: 'Hide external/JDK' },
            { key: 'hideTest', label: 'Hide test sources' },
            { key: 'hidePrivate', label: 'Hide private members' }
        ];
        for (var i = 0; i < filters.length; i++) {
            (function (f) {
                var label = document.createElement('label');
                label.className = 'filter-toggle';
                var cb = document.createElement('input');
                cb.type = 'checkbox';
                cb.addEventListener('change', function () {
                    S.filters[f.key] = cb.checked;
                    RV.computeLayout();
                    RV.computeBounds();
                    RV.render();
                });
                label.appendChild(cb);
                label.appendChild(document.createTextNode(' ' + f.label));
                container.appendChild(label);
            })(filters[i]);
        }
    }

    // ---- Layer toggles + opacity sliders -----------------------------------

    function setupLayerToggles() {
        S = RV.state;
        var container = document.getElementById('layers');
        for (var i = 0; i < RV.GROUP_ORDER.length; i++) {
            (function (group) {
                var label = document.createElement('label');
                label.className = 'layer-toggle';
                var cb = document.createElement('input');
                cb.type = 'checkbox';
                cb.checked = S.visibleLayers[group] !== false;
                cb.addEventListener('change', function () {
                    S.visibleLayers[group] = cb.checked;
                    RV.render();
                });
                label.appendChild(cb);
                label.appendChild(document.createTextNode(' ' + (RV.GROUP_LABELS[group] || group)));

                var slider = document.createElement('input');
                slider.type = 'range';
                slider.className = 'layer-opacity';
                slider.min = '0';
                slider.max = '1';
                slider.step = '0.05';
                slider.value = String(S.layerAlpha[group] !== undefined ? S.layerAlpha[group] : RV.GROUP_DEFAULT_ALPHA[group]);
                slider.addEventListener('input', function () {
                    S.layerAlpha[group] = parseFloat(slider.value);
                    RV.render();
                });
                slider.addEventListener('change', function () {
                    RV.persistState();
                });
                label.appendChild(slider);

                container.appendChild(label);
            })(RV.GROUP_ORDER[i]);
        }
    }

    // ---- Mode selector -----------------------------------------------------

    function setupModeSelector() {
        S = RV.state;
        var container = document.getElementById('mode-selector');
        for (var i = 0; i < RV.MODES.length; i++) {
            (function (mode, idx) {
                var btn = document.createElement('button');
                btn.className = 'mode-btn' + (S.mode === mode.key ? ' active' : '');
                btn.textContent = mode.label;
                btn.title = mode.key + ' layout (key ' + (idx + 1) + ')';
                btn.addEventListener('click', function () {
                    RV.setMode(mode.key);
                });
                container.appendChild(btn);
            })(RV.MODES[i], i);
        }
    }

    function setMode(key) {
        S = RV.state;
        if (S.mode === key) return;
        S.mode = key;
        var btns = document.querySelectorAll('.mode-btn');
        for (var i = 0; i < btns.length; i++) {
            btns[i].classList.toggle('active', RV.MODES[i].key === key);
        }
        // Show radial spacing slider only in radial mode.
        var radialCtrl = document.getElementById('radial-spacing-control');
        if (radialCtrl) {
            radialCtrl.style.display = (key === 'radial') ? '' : 'none';
        }
        if (key !== 'cluster') S.sim = null;
        RV.computeLayout();
        RV.computeBounds();
        RV.fitToView();
        RV.render();
        RV.persistState();
    }

    function setupRadialSpacing() {
        S = RV.state;
        var slider = document.getElementById('radial-spacing');
        var valueDisplay = document.getElementById('radial-spacing-value');
        if (!slider || !valueDisplay) return;
        // Show only in radial mode.
        var radialCtrl = document.getElementById('radial-spacing-control');
        if (radialCtrl) {
            radialCtrl.style.display = (S.mode === 'radial') ? '' : 'none';
        }
        // Store as 0.3-3.0, slider shows 3-30 (divide by 10)
        slider.value = String(Math.round((S.radialSpacing || 1.0) * 10));
        valueDisplay.textContent = (S.radialSpacing || 1.0).toFixed(1);
        slider.addEventListener('input', function () {
            S.radialSpacing = parseInt(slider.value, 10) / 10;
            valueDisplay.textContent = S.radialSpacing.toFixed(1);
            if (S.mode === 'radial') {
                RV.computeLayout();
                RV.computeBounds();
                RV.render();
            }
        });
        slider.addEventListener('change', function () {
            if (S.mode === 'radial') {
                RV.fitToView();
                RV.render();
            }
            RV.persistState();
        });
    }

    // ---- Depth sliders -----------------------------------------------------

    function setupDepthSliders() {
        S = RV.state;
        var fd = document.getElementById('focus-depth');
        var fdv = document.getElementById('focus-depth-value');
        fd.value = String(S.focusDepth);
        fdv.textContent = String(S.focusDepth);
        fd.addEventListener('input', function () {
            S.focusDepth = parseInt(fd.value, 10);
            fdv.textContent = String(S.focusDepth);
            RV.recomputeFocusLayer();
            RV.render();
        });
        fd.addEventListener('change', RV.persistState);

        var ed = document.getElementById('expand-depth');
        var edv = document.getElementById('expand-depth-value');
        ed.value = String(S.expandDepth);
        edv.textContent = String(S.expandDepth);
        ed.addEventListener('input', function () {
            S.expandDepth = parseInt(ed.value, 10);
            edv.textContent = String(S.expandDepth);
        });
        ed.addEventListener('change', function () {
            RV.initExpansion();
            RV.computeLayout();
            RV.computeBounds();
            RV.fitToView();
            RV.render();
            RV.persistState();
        });
    }

    // ---- Buttons -----------------------------------------------------------

    function setupButtons() {
        S = RV.state;
        document.getElementById('fit-view').addEventListener('click', function () {
            RV.fitToView();
            RV.render();
        });
        document.getElementById('collapse-all').addEventListener('click', function () {
            RV.initExpansion();
            RV.computeLayout();
            RV.computeBounds();
            RV.fitToView();
            RV.render();
        });
        document.getElementById('reset-layout').addEventListener('click', function () {
            S.userPositions = {};
            S.pinned = {};
            S.sim = null;
            RV.computeLayout();
            RV.computeBounds();
            RV.fitToView();
            RV.render();
            RV.persistState();
        });
    }

    // ---- Fit view ----------------------------------------------------------

    function fitToView() {
        S = RV.state;
        var count = 0;
        var minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        for (var id in S.positions) {
            if (!S.positions.hasOwnProperty(id)) continue;
            var pos = S.positions[id];
            var node = S.nodesById[id];
            if (!RV.isNodeVisible(node)) continue;
            count++;
            if (pos.x - RV.NODE_W / 2 < minX) minX = pos.x - RV.NODE_W / 2;
            if (pos.x + RV.NODE_W / 2 > maxX) maxX = pos.x + RV.NODE_W / 2;
            if (pos.y - RV.NODE_H / 2 < minY) minY = pos.y - RV.NODE_H / 2;
            if (pos.y + RV.NODE_H / 2 > maxY) maxY = pos.y + RV.NODE_H / 2;
        }
        if (count === 0) return;
        var w = Math.max(maxX - minX, 1);
        var h = Math.max(maxY - minY, 1);
        S.zoom = Math.min(S.cssW / w, S.cssH / h) * 0.85;
        S.zoom = Math.max(0.05, Math.min(8, S.zoom));
        S.camera.x = (minX + maxX) / 2;
        S.camera.y = (minY + maxY) / 2;
    }

    // ---- Focus node --------------------------------------------------------

    function focusNode(id) {
        S = RV.state;
        RV.expandAncestors(id);
        S.selected = id;
        RV.computeLayout();
        var pos = S.positions[id];
        if (pos) {
            S.camera.x = pos.x;
            S.camera.y = pos.y;
            S.zoom = Math.max(S.zoom, 1.2);
        }
        RV.recomputeFocusLayer();
        RV.showDetailPanel(id);
        RV.updateBreadcrumb();
        RV.render();
    }

    // ---- Breadcrumb --------------------------------------------------------

    function updateBreadcrumb() {
        S = RV.state;
        var bc = document.getElementById('breadcrumb');
        bc.innerHTML = '';
        if (!S.selected) return;
        var path = RV.ancestorPath(S.selected);
        for (var i = 0; i < path.length; i++) {
            if (i > 0) {
                var sep = document.createElement('span');
                sep.className = 'crumb-sep';
                sep.textContent = '\u203A';
                bc.appendChild(sep);
            }
            var node = path[i];
            var crumb = document.createElement('span');
            crumb.className = 'crumb' + (i === path.length - 1 ? ' current' : '');
            crumb.textContent = node.simpleName || node.id;
            (function (nodeId) {
                crumb.addEventListener('click', function () {
                    RV.focusNode(nodeId);
                });
            })(node.id);
            bc.appendChild(crumb);
        }
    }

    // ---- Detail panel ------------------------------------------------------

    function showDetailPanel(nodeId) {
        S = RV.state;
        var node = S.nodesById[nodeId];
        if (!node) return;
        var panel = document.getElementById('detail-panel');
        panel.classList.add('visible');

        var html = '<div class="detail-header">';
        html += '<span class="detail-type type-' + node.type + '">' + node.type + '</span>';
        html += '<span class="detail-name">' + RV.escapeHtml(node.simpleName || node.id) + '</span>';
        html += '</div>';

        html += '<div class="detail-section">';
        html += detailRow('Qualified name', node.qualifiedName);
        if (node.containingModule) html += detailRow('Module', node.containingModule);
        if (node.containingPackage) html += detailRow('Package', node.containingPackage);
        if (node.sourceFile) html += detailRow('File', node.sourceFile);
        if (node.properties && node.properties.modifiers && node.properties.modifiers.length > 0) {
            html += detailRow('Modifiers', node.properties.modifiers.join(', '));
        }
        if (node.properties && node.properties.external) {
            html += detailRow('External', 'yes');
        }
        html += '</div>';

        var outgoing = S.edgesBySource[nodeId] || [];
        var incoming = S.edgesByTarget[nodeId] || [];
        if (outgoing.length > 0 || incoming.length > 0) {
            html += '<div class="detail-section"><h3>Relationships</h3>';
            var byType = {};
            for (var i = 0; i < outgoing.length; i++) {
                var e = outgoing[i];
                var otherId = e.target;
                var other = S.nodesById[otherId];
                var otherName = (other && other.simpleName) || otherId;
                if (!byType[e.type]) byType[e.type] = [];
                byType[e.type].push({ dir: '\u2192', name: otherName, id: otherId });
            }
            for (var j = 0; j < incoming.length; j++) {
                var e2 = incoming[j];
                var otherId2 = e2.source;
                var other2 = S.nodesById[otherId2];
                var otherName2 = (other2 && other2.simpleName) || otherId2;
                if (!byType[e2.type]) byType[e2.type] = [];
                byType[e2.type].push({ dir: '\u2190', name: otherName2, id: otherId2 });
            }
            for (var type in byType) {
                if (!byType.hasOwnProperty(type)) continue;
                html += '<div class="detail-relationship-group">';
                html += '<span class="detail-relationship-type">' + type + '</span>';
                for (var k = 0; k < byType[type].length; k++) {
                    var rel = byType[type][k];
                    html += '<div class="detail-relationship" data-node-id="' + RV.escapeHtml(rel.id) + '">' +
                        '<span class="dir">' + rel.dir + '</span>' + RV.escapeHtml(rel.name) + '</div>';
                }
                html += '</div>';
            }
            html += '</div>';
        }

        panel.innerHTML = html;

        var rels = panel.querySelectorAll('.detail-relationship');
        for (var r = 0; r < rels.length; r++) {
            (function (el) {
                el.addEventListener('click', function () {
                    var targetId = el.getAttribute('data-node-id');
                    if (targetId) RV.focusNode(targetId);
                });
            })(rels[r]);
        }
    }

    function hideDetailPanel() {
        S = RV.state;
        var panel = document.getElementById('detail-panel');
        panel.classList.remove('visible');
        panel.innerHTML = '';
    }

    function detailRow(label, value) {
        if (!value) return '';
        return '<div class="detail-row"><span class="detail-label">' + RV.escapeHtml(label) +
            '</span><span class="detail-value">' + RV.escapeHtml(value) + '</span></div>';
    }

    // ---- Context menu ------------------------------------------------------

    function showContextMenu(clientX, clientY, nodeId) {
        S = RV.state;
        var menu = document.getElementById('context-menu');
        var node = S.nodesById[nodeId];
        if (!node) return;
        menu.innerHTML = '';
        menu.classList.remove('hidden');

        var items = [
            { label: 'Focus', action: function () { RV.focusNode(nodeId); } }
        ];
        if (node.type === 'module') {
            items.push({ label: 'Fit module in view', action: function () { fitModule(nodeId); } });
        }
        if ((S.childrenByParent[nodeId] || []).length > 0) {
            items.push({ sep: true });
            items.push({ label: 'Collapse all', action: function () {
                delete S.expanded[nodeId];
                RV.collapseDescendants(nodeId);
                RV.computeLayout();
                RV.computeBounds();
                RV.recomputeFocusLayer();
                RV.render();
            } });
            if (S.expanded[nodeId]) {
                items.push({ label: 'Expand all', action: function () {
                    RV.expandDescendants(nodeId);
                    RV.computeLayout();
                    RV.computeBounds();
                    RV.render();
                } });
            } else {
                items.push({ label: 'Expand', action: function () {
                    S.expanded[nodeId] = true;
                    RV.computeLayout();
                    RV.computeBounds();
                    RV.render();
                } });
            }
        }
        if (S.mode !== 'tree') {
            items.push({ sep: true });
            items.push({ label: 'Unpin position', action: function () {
                delete S.userPositions[nodeId];
                delete S.pinned[nodeId];
                RV.computeLayout();
                RV.render();
                RV.persistState();
            } });
            if ((S.childrenByParent[nodeId] || []).length > 0) {
                items.push({ label: 'Recalculate children', action: function () {
                    // Save the parent's user-placed position so we can re-apply
                    // the angular/radial offset after the fresh layout.
                    var parentPos = S.positions[nodeId]
                        ? { x: S.positions[nodeId].x, y: S.positions[nodeId].y } : null;
                    var oldParentR = parentPos
                        ? Math.sqrt(parentPos.x * parentPos.x + parentPos.y * parentPos.y) : 0;
                    var oldParentA = parentPos
                        ? Math.atan2(parentPos.y, parentPos.x) : 0;

                    var descendants = RV.getDescendantIds(nodeId);

                    // Unpin the parent and descendants so the layout can reposition freely.
                    delete S.userPositions[nodeId];
                    delete S.pinned[nodeId];
                    for (var di = 0; di < descendants.length; di++) {
                        delete S.userPositions[descendants[di]];
                        delete S.pinned[descendants[di]];
                    }

                    RV.computeLayout();

                    // The layout has now placed everything relative to the root.
                    // Compute the polar delta between where the layout put the parent
                    // and where the user placed it, then apply that delta to the
                    // entire subtree (parent + descendants) using polar coordinates,
                    // so the circular "arch" around the root is preserved.
                    if (parentPos && S.positions[nodeId]) {
                        var newLayoutPos = S.positions[nodeId];
                        var newParentR = Math.sqrt(newLayoutPos.x * newLayoutPos.x + newLayoutPos.y * newLayoutPos.y);
                        var newParentA = Math.atan2(newLayoutPos.y, newLayoutPos.x);
                        var rRatio = newParentR > 0 ? oldParentR / newParentR : 1;
                        var aDelta = oldParentA - newParentA;

                        var allSubtree = [nodeId].concat(descendants);
                        for (var si = 0; si < allSubtree.length; si++) {
                            var p = S.positions[allSubtree[si]];
                            if (!p) continue;
                            var cr = Math.sqrt(p.x * p.x + p.y * p.y);
                            var ca = Math.atan2(p.y, p.x);
                            var nr = cr * rRatio;
                            var na = ca + aDelta;
                            S.positions[allSubtree[si]] = { x: nr * Math.cos(na), y: nr * Math.sin(na) };
                            // Do NOT pin - the transformation is relative, so future
                            // parent moves will still affect children correctly.
                        }
                    }

                    RV.computeBounds();
                    RV.render();
                    RV.persistState();
                } });
            }
        }

        for (var i = 0; i < items.length; i++) {
            var it = items[i];
            if (it.sep) {
                var sep = document.createElement('div');
                sep.className = 'ctx-sep';
                menu.appendChild(sep);
            } else {
                (function (item) {
                    var el = document.createElement('div');
                    el.className = 'ctx-item';
                    el.textContent = item.label;
                    el.addEventListener('click', function () {
                        item.action();
                        RV.hideContextMenu();
                    });
                    menu.appendChild(el);
                })(it);
            }
        }

        menu.style.left = '0px';
        menu.style.top = '0px';
        var rect = menu.getBoundingClientRect();
        var x = clientX, y = clientY;
        if (x + rect.width > window.innerWidth - 8) x = window.innerWidth - rect.width - 8;
        if (y + rect.height > window.innerHeight - 8) y = window.innerHeight - rect.height - 8;
        menu.style.left = x + 'px';
        menu.style.top = y + 'px';
    }

    function hideContextMenu() {
        S = RV.state;
        document.getElementById('context-menu').classList.add('hidden');
    }

    function fitModule(moduleId) {
        var members = S.moduleMembers[moduleId] || [moduleId];
        var first = true;
        var minX = 0, maxX = 0, minY = 0, maxY = 0;
        for (var i = 0; i < members.length; i++) {
            var p = S.positions[members[i]];
            if (!p) continue;
            var node = S.nodesById[members[i]];
            if (!RV.isNodeVisible(node)) continue;
            if (first) { minX = maxX = p.x; minY = maxY = p.y; first = false; }
            else {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
        }
        if (first) return;
        var w = Math.max(maxX - minX + RV.NODE_W, 1);
        var h = Math.max(maxY - minY + RV.NODE_H, 1);
        S.zoom = Math.min(S.cssW / w, S.cssH / h) * 0.85;
        S.zoom = Math.max(0.05, Math.min(8, S.zoom));
        S.camera.x = (minX + maxX) / 2;
        S.camera.y = (minY + maxY) / 2;
        RV.render();
    }

    // Close context menu on outside click.
    document.addEventListener('click', function (e) {
        var menu = document.getElementById('context-menu');
        if (!menu.contains(e.target)) hideContextMenu();
    });

    RV.setupSearch = setupSearch;
    RV.setupFilters = setupFilters;
    RV.setupLayerToggles = setupLayerToggles;
    RV.setupModeSelector = setupModeSelector;
    RV.setMode = setMode;
    RV.setupRadialSpacing = setupRadialSpacing;
    RV.setupDepthSliders = setupDepthSliders;
    RV.setupButtons = setupButtons;
    RV.fitToView = fitToView;
    RV.focusNode = focusNode;
    RV.updateBreadcrumb = updateBreadcrumb;
    RV.showDetailPanel = showDetailPanel;
    RV.hideDetailPanel = hideDetailPanel;
    RV.showContextMenu = showContextMenu;
    RV.hideContextMenu = hideContextMenu;
})(window.RV = window.RV || {});
