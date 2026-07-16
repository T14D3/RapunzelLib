/* =====================================================================
 * Codebase Visualizer - interaction
 *
 * Pan/zoom, node drag (radial/cluster), hover hit-testing for nodes
 * and edges, click selection, keyboard shortcuts, context menu.
 * ===================================================================== */
(function (RV) {
    'use strict';

    var S;

    // ---- Coordinate conversion --------------------------------------------

    function screenToWorld(sx, sy) {
        S = RV.state;
        return {
            x: (sx - S.cssW / 2) / S.zoom + S.camera.x,
            y: (sy - S.cssH / 2) / S.zoom + S.camera.y
        };
    }

    function hitTestNode(sx, sy) {
        S = RV.state;
        var world = screenToWorld(sx, sy);
        var ids = Object.keys(S.positions);
        for (var i = ids.length - 1; i >= 0; i--) {
            var id = ids[i];
            var pos = S.positions[id];
            var node = S.nodesById[id];
            if (!RV.isNodeVisible(node)) continue;
            if (Math.abs(world.x - pos.x) < RV.NODE_W / 2 &&
                Math.abs(world.y - pos.y) < RV.NODE_H / 2) {
                return id;
            }
        }
        return null;
    }

    // Edge hit-testing: sample points along each visible cross edge's curve.
    // Only runs on mousemove (not per-frame) and is culled to viewport.
    function hitTestEdge(sx, sy) {
        var world = screenToWorld(sx, sy);
        var threshold = 6 / S.zoom;
        var best = null;
        var bestDist = threshold;
        for (var i = 0; i < S.crossEdges.length; i++) {
            var edge = S.crossEdges[i];
            if (!RV.isEdgeVisible(edge)) continue;
            var src = S.positions[edge.source];
            var tgt = S.positions[edge.target];
            if (!src || !tgt) continue;
            var x1 = src.x, y1 = src.y;
            var x2 = tgt.x, y2 = tgt.y;
            var mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
            var dx = x2 - x1, dy = y2 - y1;
            var len = Math.sqrt(dx * dx + dy * dy) || 1;
            var offset = Math.min(60, len * 0.18);
            var nx = -dy / len * offset;
            var ny = dx / len * offset;
            var cx = mx + nx, cy = my + ny;
            for (var t = 0; t <= 1; t += 1 / 12) {
                var u = 1 - t;
                var px = u * u * x1 + 2 * u * t * cx + t * t * x2;
                var py = u * u * y1 + 2 * u * t * cy + t * t * y2;
                var d = Math.sqrt((px - world.x) * (px - world.x) + (py - world.y) * (py - world.y));
                if (d < bestDist) {
                    bestDist = d;
                    best = edge;
                }
            }
        }
        return best;
    }

    // ---- Setup -------------------------------------------------------------

    function setup() {
        S = RV.state;
        var canvas = S.canvas;
        var dragStart = null;
        var mouseDown = null;
        var isDragging = false;
        var draggingNode = null;
        var potentialDragNode = null;
        var dragNodeOrigin = null;

        canvas.addEventListener('mousedown', function (e) {
            if (e.button === 2) return;
            var rect = canvas.getBoundingClientRect();
            var mx = e.clientX - rect.left;
            var my = e.clientY - rect.top;
            mouseDown = { x: mx, y: my };
            dragStart = { x: e.clientX, y: e.clientY, camX: S.camera.x, camY: S.camera.y };
            isDragging = false;
            draggingNode = null;

            var hitId = hitTestNode(mx, my);
            // Don't start dragging yet - wait for actual mouse movement.
            // A simple click should select, not drag.
            if (hitId && S.mode !== 'tree') {
                potentialDragNode = hitId;
            } else {
                potentialDragNode = null;
            }
        });

        canvas.addEventListener('mousemove', function (e) {
            var rect = canvas.getBoundingClientRect();
            var mx = e.clientX - rect.left;
            var my = e.clientY - rect.top;

            if (dragStart) {
                var dx = e.clientX - dragStart.x;
                var dy = e.clientY - dragStart.y;
                if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                    // User has moved enough to qualify as a drag.
                    if (!isDragging && potentialDragNode) {
                        // Start dragging the node that was hit on mousedown.
                        draggingNode = potentialDragNode;
                        canvas.classList.add('dragging-node');
                        S.pinned[draggingNode] = true;
                        // Record original position so we can offset children on drop.
                        dragNodeOrigin = S.positions[draggingNode]
                            ? { x: S.positions[draggingNode].x, y: S.positions[draggingNode].y } : null;
                    }
                    isDragging = true;
                }

                if (draggingNode) {
                    var world = screenToWorld(mx, my);
                    S.userPositions[draggingNode] = { x: world.x, y: world.y };
                    S.positions[draggingNode] = { x: world.x, y: world.y };
                    // Fast GPU update during drag - update only this node's
                    // position and connected edge endpoints in the buffer,
                    // avoiding a full rebuildFullBuffers().
                    if (RV.updateNodePosition) {
                        RV.updateNodePosition(draggingNode, world.x, world.y);
                    } else {
                        RV.computeBounds();
                        if (RV.markRenderDirty) RV.markRenderDirty();
                    }
                    RV.render();
                } else if (isDragging) {
                    S.camera.x = dragStart.camX - dx / S.zoom;
                    S.camera.y = dragStart.camY - dy / S.zoom;
                    RV.render();
                }
            } else {
                var prevNode = S.hoveredNode;
                var prevEdge = S.hoveredEdge;
                var hitId = hitTestNode(mx, my);
                if (hitId) {
                    S.hoveredNode = hitId;
                    S.hoveredEdge = null;
                } else {
                    S.hoveredNode = null;
                    var edge = hitTestEdge(mx, my);
                    S.hoveredEdge = edge;
                }
                if (S.hoveredNode !== prevNode || S.hoveredEdge !== prevEdge) {
                    updateTooltip(e.clientX, e.clientY);
                    if (RV.markRenderDirty) RV.markRenderDirty();
                    RV.render();
                } else if (S.hoveredNode || S.hoveredEdge) {
                    updateTooltip(e.clientX, e.clientY);
                }
            }
        });

        canvas.addEventListener('mouseup', function () {
            canvas.classList.remove('dragging-node');
            if (draggingNode && isDragging) {
                // On drop, apply the move as a angular/radial transformation
                // around the global center (root), so the subtree keeps its
                // circular structure relative to the root.
                if (dragNodeOrigin && S.positions[draggingNode]) {
                    var finalPos = S.positions[draggingNode];
                    var oldR = Math.sqrt(dragNodeOrigin.x * dragNodeOrigin.x + dragNodeOrigin.y * dragNodeOrigin.y);
                    var oldA = Math.atan2(dragNodeOrigin.y, dragNodeOrigin.x);
                    var newR = Math.sqrt(finalPos.x * finalPos.x + finalPos.y * finalPos.y);
                    var newA = Math.atan2(finalPos.y, finalPos.x);
                    var descendants = RV.getDescendantIds(draggingNode);
                    for (var di = 0; di < descendants.length; di++) {
                        if (S.pinned[descendants[di]]) continue;
                        var cp = S.positions[descendants[di]];
                        if (!cp) continue;
                        var cr = Math.sqrt(cp.x * cp.x + cp.y * cp.y);
                        var ca = Math.atan2(cp.y, cp.x);
                        var nr = oldR > 0 ? cr * (newR / oldR) : cr;
                        var na = ca + (newA - oldA);
                        S.positions[descendants[di]] = { x: nr * Math.cos(na), y: nr * Math.sin(na) };
                        S.userPositions[descendants[di]] = { x: nr * Math.cos(na), y: nr * Math.sin(na) };
                    }
                }
                RV.persistState();
                // Rebuild full buffers with final positions of all descendants.
                RV.computeBounds();
                if (RV.markRenderDirty) RV.markRenderDirty();
                RV.render();
            } else if (!isDragging && mouseDown) {
                var id = hitTestNode(mouseDown.x, mouseDown.y);
                if (id) {
                    var world = screenToWorld(mouseDown.x, mouseDown.y);
                    handleNodeClick(id, world.x, world.y);
                } else {
                    S.selected = null;
                    RV.recomputeFocusLayer();
                    RV.hideDetailPanel();
                    RV.updateBreadcrumb();
                    RV.render();
                }
            }
            dragStart = null;
            mouseDown = null;
            isDragging = false;
            draggingNode = null;
            potentialDragNode = null;
            dragNodeOrigin = null;
        });

        canvas.addEventListener('wheel', function (e) {
            e.preventDefault();
            var rect = canvas.getBoundingClientRect();
            var mx = e.clientX - rect.left;
            var my = e.clientY - rect.top;
            var world = screenToWorld(mx, my);
            var factor = e.deltaY < 0 ? 1.15 : 0.87;
            S.zoom *= factor;
            S.zoom = Math.max(0.05, Math.min(8, S.zoom));
            var newWorld = screenToWorld(mx, my);
            S.camera.x += world.x - newWorld.x;
            S.camera.y += world.y - newWorld.y;
            RV.render();
        }, { passive: false });

        canvas.addEventListener('contextmenu', function (e) {
            e.preventDefault();
            var rect = canvas.getBoundingClientRect();
            var mx = e.clientX - rect.left;
            var my = e.clientY - rect.top;
            var id = hitTestNode(mx, my);
            if (id) RV.showContextMenu(e.clientX, e.clientY, id);
            else RV.hideContextMenu();
        });

        canvas.addEventListener('mouseleave', function () {
            S.hoveredNode = null;
            S.hoveredEdge = null;
            RV.hideTooltip();
            RV.render();
        });

        // Minimap interaction.
        var mm = S.minimap;
        var mmDragging = false;
        mm.addEventListener('mousedown', function (e) {
            mmDragging = true;
            minimapPan(e);
        });
        window.addEventListener('mousemove', function (e) {
            if (mmDragging) minimapPan(e);
        });
        window.addEventListener('mouseup', function () { mmDragging = false; });

        function minimapPan(e) {
            var rect = mm.getBoundingClientRect();
            var mx = e.clientX - rect.left;
            var my = e.clientY - rect.top;
            var world = RV.minimapToWorld(mx, my);
            S.camera.x = world.x;
            S.camera.y = world.y;
            RV.render();
        }

        // Keyboard shortcuts.
        document.addEventListener('keydown', function (e) {
            if (e.target.tagName === 'INPUT' && e.target.type !== 'range') return;
            if (e.key === 'Escape') {
                S.selected = null;
                RV.recomputeFocusLayer();
                RV.hideDetailPanel();
                RV.updateBreadcrumb();
                RV.hideContextMenu();
                RV.render();
            } else if (e.key === 'f' || e.key === 'F') {
                RV.fitToView();
                RV.render();
            } else if (e.key === 'm' || e.key === 'M') {
                mm.classList.toggle('hidden');
            } else if (e.key === '/') {
                e.preventDefault();
                document.getElementById('search').focus();
            } else if (e.key >= '1' && e.key <= '3') {
                var idx = parseInt(e.key, 10) - 1;
                if (RV.MODES[idx]) RV.setMode(RV.MODES[idx].key);
            } else if (e.key === 'ArrowUp' || e.key === 'ArrowDown' ||
                       e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
                if (S.selected) {
                    navigateToNeighbor(e.key);
                    e.preventDefault();
                }
            }
        });
    }

    function navigateToNeighbor(key) {
        var id = S.selected;
        var neighbors = [];
        var out = S.edgesBySource[id] || [];
        var inc = S.edgesByTarget[id] || [];
        for (var i = 0; i < out.length; i++) neighbors.push(out[i].target);
        for (var j = 0; j < inc.length; j++) neighbors.push(inc[j].source);
        if (neighbors.length === 0) return;
        var pos = S.positions[id];
        if (!pos) return;
        var scored = [];
        for (var k = 0; k < neighbors.length; k++) {
            var npos = S.positions[neighbors[k]];
            if (!npos) continue;
            var dx = npos.x - pos.x, dy = npos.y - pos.y;
            var score = 0;
            if (key === 'ArrowRight') score = dx - Math.abs(dy) * 0.5;
            else if (key === 'ArrowLeft') score = -dx - Math.abs(dy) * 0.5;
            else if (key === 'ArrowDown') score = dy - Math.abs(dx) * 0.5;
            else if (key === 'ArrowUp') score = -dy - Math.abs(dx) * 0.5;
            scored.push({ id: neighbors[k], score: score });
        }
        scored.sort(function (a, b) { return b.score - a.score; });
        if (scored.length > 0) RV.focusNode(scored[0].id);
    }

    /** Check whether screen coordinates (sx, sy) hit the expand/collapse badge on a node at pos. */
    function isOnExpandBadge(sx, sy, pos) {
        var badgeLeft = pos.x + RV.NODE_W / 2 - 30;
        var badgeRight = pos.x + RV.NODE_W / 2;
        var badgeTop = pos.y - RV.NODE_H / 2;
        var badgeBottom = pos.y + RV.NODE_H / 2;
        return sx >= badgeLeft && sx <= badgeRight && sy >= badgeTop && sy <= badgeBottom;
    }

    /** Toggle expand/collapse for a node and re-layout. */
    function toggleExpandNode(id) {
        S = RV.state;
        var hasChildren = (S.childrenByParent[id] || []).length > 0;
        if (!hasChildren) return;
        S.selected = id;

        // Preserve the node's current position so it doesn't jump when
        // the layout recalculates around its new expand/collapse state.
        var savedPos = S.positions[id] ? { x: S.positions[id].x, y: S.positions[id].y } : null;

        if (S.expanded[id]) {
            delete S.expanded[id];
            RV.collapseDescendants(id);
        } else {
            S.expanded[id] = true;
        }
        if (RV.markRenderDirty) RV.markRenderDirty();
        RV.computeLayout().then(function () {
            // Restore position after re-layout.
            if (savedPos) {
                S.positions[id] = savedPos;
                S.userPositions[id] = savedPos;
                S.pinned[id] = true;
            }
            RV.computeBounds();
            RV.recomputeFocusLayer();
            RV.showDetailPanel(id);
            RV.updateBreadcrumb();
            RV.render();
        });
    }

    /** Handle a click on a node - dispatches to expand-toggle or focus. */
    function handleNodeClick(id, worldX, worldY) {
        S = RV.state;
        var pos = S.positions[id];
        // Click on the right-side expand/collapse badge toggles, otherwise just focus.
        if (pos && isOnExpandBadge(worldX, worldY, pos)) {
            toggleExpandNode(id);
        } else {
            RV.focusNode(id);
        }
    }

    // ---- Tooltip -----------------------------------------------------------

    function updateTooltip(clientX, clientY) {
        S = RV.state;
        var tooltip = document.getElementById('tooltip');
        if (S.hoveredEdge) {
            var edge = S.hoveredEdge;
            var src = S.nodesById[edge.source];
            var tgt = S.nodesById[edge.target];
            var html = '<div class="tt-row">';
            html += '<span class="tt-type type-' + (src ? src.type : '') + '">' + (src ? src.type : '') + '</span>';
            html += '<span class="tt-name">' + RV.escapeHtml(src ? (src.simpleName || src.id) : edge.source) + '</span>';
            html += '<span class="tt-arrow">\u2192</span>';
            html += '<span class="tt-type type-' + (tgt ? tgt.type : '') + '">' + (tgt ? tgt.type : '') + '</span>';
            html += '<span class="tt-name">' + RV.escapeHtml(tgt ? (tgt.simpleName || tgt.id) : edge.target) + '</span>';
            html += '</div>';
            html += '<div class="tt-row" style="color:var(--accent);font-weight:600;margin-top:4px">' + RV.escapeHtml(edge.type) + '</div>';
            if (src && src.qualifiedName) {
                html += '<div class="tt-qname">' + RV.escapeHtml(src.qualifiedName) + '</div>';
            }
            if (tgt && tgt.qualifiedName) {
                html += '<div class="tt-qname">\u2192 ' + RV.escapeHtml(tgt.qualifiedName) + '</div>';
            }
            tooltip.innerHTML = html;
            showTooltipAt(clientX, clientY);
        } else if (S.hoveredNode) {
            var node = S.nodesById[S.hoveredNode];
            var h = '<div class="tt-row">';
            h += '<span class="tt-type type-' + node.type + '">' + node.type + '</span>';
            h += '<span class="tt-name">' + RV.escapeHtml(node.simpleName || node.id) + '</span>';
            h += '</div>';
            if (node.qualifiedName) h += '<div class="tt-qname">' + RV.escapeHtml(node.qualifiedName) + '</div>';
            tooltip.innerHTML = h;
            showTooltipAt(clientX, clientY);
        } else {
            RV.hideTooltip();
        }
    }

    function showTooltipAt(clientX, clientY) {
        var tooltip = document.getElementById('tooltip');
        tooltip.classList.add('visible');
        var x = clientX + 14;
        var y = clientY + 14;
        var rect = tooltip.getBoundingClientRect();
        if (x + rect.width > window.innerWidth - 8) x = clientX - rect.width - 14;
        if (y + rect.height > window.innerHeight - 8) y = clientY - rect.height - 14;
        tooltip.style.left = x + 'px';
        tooltip.style.top = y + 'px';
    }

    function hideTooltip() {
        S = RV.state;
        document.getElementById('tooltip').classList.remove('visible');
    }

    RV.screenToWorld = screenToWorld;
    RV.hitTestNode = hitTestNode;
    RV.setupInteraction = setup;
    RV.toggleExpandNode = toggleExpandNode;
    RV.updateTooltip = updateTooltip;
    RV.hideTooltip = hideTooltip;
})(window.RV = window.RV || {});
