/* =====================================================================
 * Codebase Visualizer - constants & palette
 * Loaded first; exposes window.RV namespace.
 * ===================================================================== */
(function (RV) {
    'use strict';

    RV.NODE_W = 150;
    RV.NODE_H = 38;
    RV.GAP = 26;
    RV.LEVEL_H = 96;
    RV.RADIAL_RING = 180;
    RV.RADIAL_GAP = 0.04;
    RV.MAX_DPR = 2;

    RV.NODE_COLORS = {
        project: '#1a1a2e',
        module: '#16213e',
        sourceSet: '#0f3460',
        package: '#0f4c75',
        class: '#e76f51',
        interface: '#f4a261',
        enum: '#2a9d8f',
        record: '#e9c46a',
        annotation: '#b5651d',
        method: '#48cae4',
        constructor: '#90b4ce',
        field: '#a8dadc'
    };

    RV.NODE_TEXT_COLORS = {
        project: '#e0e0e0', module: '#c0c0c0', sourceSet: '#b0d0e0',
        package: '#d0e0f0', class: '#fff', interface: '#000', enum: '#fff',
        record: '#000', annotation: '#fff', method: '#000',
        constructor: '#000', field: '#000'
    };

    RV.NODE_GLYPH = {
        project: '\u25A0', module: '\u25A0', sourceSet: '\u25A4',
        package: '\u25A4', class: 'C', interface: 'I', enum: 'E',
        record: 'R', annotation: '@', method: '\u0192',
        constructor: '\u00A7', field: 'F'
    };

    RV.EDGE_COLORS = {
        contains: '#444c56', extends: '#e63946', implements: '#f77f00',
        calls: '#4ea8de', references: '#52b788', uses: '#40916c',
        returns: '#9d4edd', throws: '#ff70a6', creates: '#fcbf49',
        annotatedBy: '#ffd166', dependsOn: '#b08968'
    };

    RV.EDGE_GROUPS = {
        contains: 'structure', extends: 'inheritance', implements: 'inheritance',
        calls: 'calls', references: 'references', uses: 'references',
        returns: 'references', throws: 'references', creates: 'references',
        annotatedBy: 'annotations', dependsOn: 'gradle'
    };

    RV.GROUP_LABELS = {
        structure: 'Structure', inheritance: 'Inheritance',
        calls: 'Method calls', references: 'References',
        annotations: 'Annotations', gradle: 'Gradle deps'
    };

    RV.GROUP_ORDER = ['structure', 'inheritance', 'calls', 'references', 'annotations', 'gradle'];

    RV.GROUP_DEFAULT_ALPHA = {
        structure: 0.45, inheritance: 0.85, calls: 0.55,
        references: 0.55, annotations: 0.7, gradle: 0.7
    };

    // Falloff of brightness per hop from the focus node.
    RV.FOCUS_FALLOFF = [1.0, 1.0, 0.55, 0.30, 0.18, 0.10, 0.06];

    RV.MODES = [
        { key: 'tree', label: 'Tree' },
        { key: 'radial', label: 'Radial' },
        { key: 'cluster', label: 'Cluster' }
    ];

    RV.STORAGE_KEY = 'rapunzel-viz-state';

})(window.RV = window.RV || {});
