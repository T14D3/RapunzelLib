package de.t14d3.rapunzellib.visualizer.collector;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import de.t14d3.rapunzellib.visualizer.model.Edge;
import de.t14d3.rapunzellib.visualizer.model.EdgeType;
import de.t14d3.rapunzellib.visualizer.model.Graph;
import de.t14d3.rapunzellib.visualizer.model.Node;
import de.t14d3.rapunzellib.visualizer.model.NodeType;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link SourceCollector} backed by the JDK's {@code com.sun.source.*}
 * Trees API.
 *
 * <p>Resolves:
 * <ul>
 *   <li>{@code contains} edges from source set -> package -> type -> member</li>
 *   <li>{@code extends} / {@code implements} edges between types</li>
 *   <li>{@code references} edges from types to types used in fields, return types, throws, and type parameters</li>
 *   <li>{@code calls} edges from methods to invoked methods (best-effort, requires the callee to be on the compile classpath)</li>
 * </ul>
 *
 * <p>External symbols (JDK, third-party jars) are recorded as nodes too so
 * the UI can choose to filter them out; they are flagged via the
 * {@code external} property.
 */
public final class JavacSourceCollector implements SourceCollector {

    @Override
    public void collect(Graph graph, String sourceSetId, String moduleName,
                        List<File> sourceFiles, List<File> auxiliaryFiles,
                        Collection<File> compileClasspath, Collection<File> sourceRoots) {
        if (sourceFiles == null || sourceFiles.isEmpty()) {
            return;
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("System Java compiler is unavailable; cannot run codebase visualizer on a JRE-only JDK.");
        }

        List<File> classpathEntries = filterExisting(compileClasspath);
        List<File> sourcePathEntries = filterExisting(sourceRoots);

        // Only compile this source set's own files. Cross-module symbol resolution
        // is handled via -sourcepath (source roots of all modules), not by compiling
        // other modules' sources. Compiling all modules together causes duplicate
        // class errors when modules share packages (e.g., :commands and :commands-shared
        // both define classes in de.t14d3.rapunzellib.commands).
        List<File> filesToCompile = new ArrayList<>();
        Set<String> fileCanonicalSet = new HashSet<>();
        for (File f : sourceFiles) {
            if (f.isFile() && f.getName().endsWith(".java") && fileCanonicalSet.add(canonicalPath(f))) {
                filesToCompile.add(f);
            }
        }

        // Track which files belong to this source set (only these create nodes).
        // Use canonical paths for reliable comparison.
        Set<String> primaryCanonical = new HashSet<>();
        for (File f : sourceFiles) {
            primaryCanonical.add(canonicalPath(f));
        }

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        try {
            if (!classpathEntries.isEmpty()) {
                try {
                    fileManager.setLocation(StandardLocation.CLASS_PATH, classpathEntries);
                } catch (java.io.IOException ignored) {
                    // Best-effort: if classpath can't be set, symbol resolution for external types will be incomplete.
                }
            }
            if (!sourcePathEntries.isEmpty()) {
                try {
                    fileManager.setLocation(StandardLocation.SOURCE_PATH, sourcePathEntries);
                } catch (java.io.IOException ignored) {
                    // Best-effort: cross-module source resolution may be incomplete.
                }
            }
            Iterable<? extends JavaFileObject> units =
                fileManager.getJavaFileObjectsFromFiles(filesToCompile);
            List<String> options = new ArrayList<>();
            options.add("-proc:none");
            options.add("-Xlint:none");
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, null, options, null, units);
            if (!(task instanceof JavacTask javacTask)) {
                return;
            }
            try {
                List<CompilationUnitTree> parsed = new ArrayList<>();
                for (CompilationUnitTree cu : javacTask.parse()) {
                    parsed.add(cu);
                }
                Trees trees = Trees.instance(javacTask);
                javacTask.analyze();
                for (CompilationUnitTree cu : parsed) {
                    // Only create nodes for files belonging to this source set.
                    File unitFile = unitToFile(cu);
                    if (unitFile != null && !primaryCanonical.contains(canonicalPath(unitFile))) {
                        continue;
                    }
                    new UnitScanner(graph, sourceSetId, moduleName, cu, trees).scan(cu, null);
                }
            } catch (Exception ignored) {
                // Best-effort: a single unparseable file should not abort the whole report.
            }
        } finally {
            try {
                fileManager.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    /** Returns the canonical path of a file, falling back to absolute path on error. */
    private static String canonicalPath(File f) {
        if (f == null) return "";
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }
    }

    private static File unitToFile(CompilationUnitTree cu) {
        if (cu.getSourceFile() == null) return null;
        try {
            return new File(cu.getSourceFile().toUri());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<File> filterExisting(Collection<File> files) {
        if (files == null) {
            return List.of();
        }
        List<File> out = new ArrayList<>();
        for (File f : files) {
            if (f.exists()) {
                out.add(f);
            }
        }
        return out;
    }

    /** Standard location constant - avoids importing {@code javax.tools.StandardLocation} everywhere. */
    private static final class StandardLocation {
        static final javax.tools.JavaFileManager.Location CLASS_PATH =
            javax.tools.StandardLocation.CLASS_PATH;
        static final javax.tools.JavaFileManager.Location SOURCE_PATH =
            javax.tools.StandardLocation.SOURCE_PATH;
    }

    /**
     * AST scanner for a single compilation unit. Builds package, type, and
     * member nodes plus their edges.
     */
    private static final class UnitScanner extends TreeScanner<Void, Void> {
        private final Graph graph;
        private final String sourceSetId;
        private final String moduleName;
        private final CompilationUnitTree unit;
        private final Trees trees;
        private String packageName;
        private String packageNodeId;
        private final Set<String> declaredTypeIds = new HashSet<>();

        UnitScanner(Graph graph, String sourceSetId, String moduleName,
                    CompilationUnitTree unit, Trees trees) {
            this.graph = graph;
            this.sourceSetId = sourceSetId;
            this.moduleName = moduleName;
            this.unit = unit;
            this.trees = trees;
        }

        @Override
        public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
            packageName = node.getPackageName() == null ? "" : node.getPackageName().toString();
            packageNodeId = ensurePackageNode(packageName);
            super.visitCompilationUnit(node, p);
            return null;
        }

        @Override
        public Void visitClass(ClassTree tree, Void p) {
            Element el = elementFor(tree);
            if (!(el instanceof TypeElement typeElement)) {
                return super.visitClass(tree, p);
            }
            String typeId = externalTypeId(typeElement);
            Node typeNode = graph.addNode(buildTypeNode(typeId, typeElement, tree));
            declaredTypeIds.add(typeId);
            graph.addEdge(new Edge(packageNodeId, typeId, EdgeType.CONTAINS));

            // extends
            Tree extendsTree = tree.getExtendsClause();
            if (extendsTree != null) {
                TypeMirror sup = typeMirrorFor(extendsTree);
                if (sup != null && sup.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                    Element superEl = ((DeclaredType) sup).asElement();
                    if (superEl instanceof TypeElement superType) {
                        String superId = externalTypeId(superType);
                        ensureExternalTypeNode(superType, superId);
                        graph.addEdge(new Edge(typeId, superId, EdgeType.EXTENDS));
                    }
                }
            }

            // implements
            for (Tree impl : tree.getImplementsClause()) {
                TypeMirror implType = typeMirrorFor(impl);
                if (implType != null && implType.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                    Element implEl = ((DeclaredType) implType).asElement();
                    if (implEl instanceof TypeElement implTypeEl) {
                        String implId = externalTypeId(implTypeEl);
                        ensureExternalTypeNode(implTypeEl, implId);
                        graph.addEdge(new Edge(typeId, implId, EdgeType.IMPLEMENTS));
                    }
                }
            }

            // type parameters (references)
            for (TypeParameterElement tp : typeElement.getTypeParameters()) {
                for (TypeMirror bound : tp.getBounds()) {
                    addReferenceEdge(typeId, bound);
                }
            }

            super.visitClass(tree, p);
            return null;
        }

        @Override
        public Void visitMethod(MethodTree tree, Void p) {
            Element el = elementFor(tree);
            if (el == null) {
                return super.visitMethod(tree, p);
            }
            String ownerId = ownerTypeId(el);
            if (ownerId == null) {
                return super.visitMethod(tree, p);
            }
            boolean isCtor = tree.getReturnType() == null;
            String memberId = ownerId + (isCtor ? "#ctor" : "#" + el.getSimpleName() + signature(el));
            Node memberNode = new Node(memberId, isCtor ? NodeType.CONSTRUCTOR : NodeType.METHOD);
            memberNode.setSimpleName(el.getSimpleName().toString());
            memberNode.setQualifiedName(ownerQualifiedName(el) + "." + el.getSimpleName());
            memberNode.setContainingModule(moduleName);
            memberNode.setContainingPackage(packageName);
            memberNode.setSourceFile(sourceFile());
            memberNode.putProperty("modifiers", modifiersOf(el));
            graph.addNode(memberNode);
            graph.addEdge(new Edge(ownerId, memberId, EdgeType.CONTAINS));

            // return type reference
            if (!isCtor && tree.getReturnType() != null) {
                TypeMirror rt = typeMirrorFor(tree.getReturnType());
                if (rt != null) {
                    addReferenceEdge(memberId, rt);
                    addReturnsEdge(memberId, rt);
                }
            }
            // throws references
            for (Tree thr : tree.getThrows()) {
                TypeMirror tm = typeMirrorFor(thr);
                if (tm != null) {
                    addThrowsEdge(memberId, tm);
                }
            }
            // parameters
            for (VariableTree param : tree.getParameters()) {
                TypeMirror pt = typeMirrorFor(param.getType());
                if (pt != null) {
                    addReferenceEdge(memberId, pt);
                }
            }
            // body - method calls and references
            super.visitMethod(tree, p);
            return null;
        }

        @Override
        public Void visitVariable(VariableTree tree, Void p) {
            Element el = elementFor(tree);
            if (el == null || el.getEnclosingElement() == null) {
                return super.visitVariable(tree, p);
            }
            // Only field-level variables (skip local vars and parameters - those are visited via MethodTree).
            if (el.getEnclosingElement().getKind() != ElementKind.CLASS
                && el.getEnclosingElement().getKind() != ElementKind.INTERFACE
                && el.getEnclosingElement().getKind() != ElementKind.ENUM
                && el.getEnclosingElement().getKind() != ElementKind.RECORD) {
                return super.visitVariable(tree, p);
            }
            String ownerId = ownerTypeId(el);
            if (ownerId == null) {
                return super.visitVariable(tree, p);
            }
            String fieldId = ownerId + "#field:" + el.getSimpleName();
            Node fieldNode = new Node(fieldId, NodeType.FIELD);
            fieldNode.setSimpleName(el.getSimpleName().toString());
            fieldNode.setQualifiedName(ownerQualifiedName(el) + "." + el.getSimpleName());
            fieldNode.setContainingModule(moduleName);
            fieldNode.setContainingPackage(packageName);
            fieldNode.setSourceFile(sourceFile());
            fieldNode.putProperty("modifiers", modifiersOf(el));
            graph.addNode(fieldNode);
            graph.addEdge(new Edge(ownerId, fieldId, EdgeType.CONTAINS));

            TypeMirror ft = typeMirrorFor(tree.getType());
            if (ft != null) {
                addReferenceEdge(fieldId, ft);
                addUsesEdge(fieldId, ft);
            }
            return super.visitVariable(tree, p);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
            Element el = elementFor(node);
            String callerId = currentMethodId();
            if (el != null && callerId != null) {
                Element owner = el.getEnclosingElement();
                if (owner instanceof TypeElement ownerType) {
                    String calleeTypeId = externalTypeId(ownerType);
                    ensureExternalTypeNode(ownerType, calleeTypeId);
                    String calleeId = calleeTypeId + "#" + el.getSimpleName() + signature(el);
                    if (!graph.hasNode(calleeId)) {
                        Node callee = new Node(calleeId, NodeType.METHOD);
                        callee.setSimpleName(el.getSimpleName().toString());
                        callee.setQualifiedName(ownerType.getQualifiedName().toString() + "." + el.getSimpleName());
                        callee.setContainingModule(externalModule(ownerType));
                        callee.setContainingPackage(packageOf(ownerType));
                        callee.putProperty("external", true);
                        graph.addNode(callee);
                    }
                    graph.addEdge(new Edge(callerId, calleeId, EdgeType.CALLS));
                }
            }
            return super.visitMethodInvocation(node, p);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, Void p) {
            Element el = elementFor(node);
            String fromId = currentMethodId();
            if (el != null && fromId != null && el.getKind() == ElementKind.FIELD) {
                Element owner = el.getEnclosingElement();
                if (owner instanceof TypeElement ownerType) {
                    String refTypeId = externalTypeId(ownerType);
                    ensureExternalTypeNode(ownerType, refTypeId);
                    graph.addEdge(new Edge(fromId, refTypeId, EdgeType.REFERENCES));
                }
            }
            return super.visitMemberSelect(node, p);
        }

        // ---- helpers -----------------------------------------------------

        private final List<String> methodIdStack = new ArrayList<>();

        private String currentMethodId() {
            return methodIdStack.isEmpty() ? null : methodIdStack.get(methodIdStack.size() - 1);
        }

        @Override
        public Void scan(Tree tree, Void p) {
            // Track method context for nested scans (method invocations inside a method body).
            if (tree instanceof MethodTree mt) {
                Element el = elementFor(mt);
                String ownerId = ownerTypeId(el);
                String id = ownerId == null ? null
                    : (mt.getReturnType() == null ? ownerId + "#ctor" : ownerId + "#" + el.getSimpleName() + signature(el));
                if (id != null) {
                    methodIdStack.add(id);
                }
                try {
                    return super.scan(tree, p);
                } finally {
                    methodIdStack.remove(methodIdStack.size() - 1);
                }
            }
            return super.scan(tree, p);
        }

        private Element elementFor(Tree tree) {
            try {
                return trees.getElement(trees.getPath(unit, tree));
            } catch (Exception ignored) {
                return null;
            }
        }

        private TypeMirror typeMirrorFor(Tree tree) {
            try {
                TreePath path = trees.getPath(unit, tree);
                if (path == null) return null;
                return trees.getTypeMirror(path);
            } catch (Exception ignored) {
                return null;
            }
        }

        private String sourceFile() {
            return unit.getSourceFile() == null ? null : unit.getSourceFile().getName();
        }

        private String ensurePackageNode(String pkg) {
            String id = "package:" + pkg;
            if (!graph.hasNode(id)) {
                Node node = new Node(id, NodeType.PACKAGE);
                String simple = pkg.isEmpty() ? "(default)" : pkg.substring(pkg.lastIndexOf('.') + 1);
                node.setSimpleName(simple);
                node.setQualifiedName(pkg.isEmpty() ? "(default package)" : pkg);
                node.setContainingModule(moduleName);
                graph.addNode(node);
                graph.addEdge(new Edge(sourceSetId, id, EdgeType.CONTAINS));
            }
            return id;
        }

        private Node buildTypeNode(String typeId, TypeElement el, ClassTree tree) {
            NodeType type = nodeTypeFor(el);
            Node node = new Node(typeId, type);
            node.setSimpleName(el.getSimpleName().toString());
            node.setQualifiedName(el.getQualifiedName().toString());
            node.setContainingModule(moduleName);
            node.setContainingPackage(packageName);
            node.setSourceFile(sourceFile());
            node.putProperty("modifiers", modifiersOf(el));
            return node;
        }

        private static NodeType nodeTypeFor(TypeElement el) {
            return switch (el.getKind()) {
                case ENUM -> NodeType.ENUM;
                case INTERFACE -> NodeType.INTERFACE;
                case ANNOTATION_TYPE -> NodeType.ANNOTATION;
                case RECORD -> NodeType.RECORD;
                default -> NodeType.CLASS;
            };
        }

        private void ensureExternalTypeNode(TypeElement el, String id) {
            if (graph.hasNode(id)) {
                return;
            }
            Node node = new Node(id, nodeTypeFor(el));
            node.setSimpleName(el.getSimpleName().toString());
            node.setQualifiedName(el.getQualifiedName().toString());
            node.setContainingModule(externalModule(el));
            node.setContainingPackage(packageOf(el));
            node.putProperty("external", true);
            graph.addNode(node);
        }

        private void addReferenceEdge(String fromId, TypeMirror type) {
            TypeElement target = typeElementOf(type);
            if (target == null) {
                return;
            }
            String targetId = externalTypeId(target);
            ensureExternalTypeNode(target, targetId);
            graph.addEdge(new Edge(fromId, targetId, EdgeType.REFERENCES));
        }

        private void addReturnsEdge(String memberId, TypeMirror type) {
            TypeElement target = typeElementOf(type);
            if (target == null) {
                return;
            }
            String targetId = externalTypeId(target);
            ensureExternalTypeNode(target, targetId);
            graph.addEdge(new Edge(memberId, targetId, EdgeType.RETURNS));
        }

        private void addThrowsEdge(String memberId, TypeMirror type) {
            TypeElement target = typeElementOf(type);
            if (target == null) {
                return;
            }
            String targetId = externalTypeId(target);
            ensureExternalTypeNode(target, targetId);
            graph.addEdge(new Edge(memberId, targetId, EdgeType.THROWS));
        }

        private void addUsesEdge(String fieldId, TypeMirror type) {
            TypeElement target = typeElementOf(type);
            if (target == null) {
                return;
            }
            String targetId = externalTypeId(target);
            ensureExternalTypeNode(target, targetId);
            graph.addEdge(new Edge(fieldId, targetId, EdgeType.USES));
        }

        private static TypeElement typeElementOf(TypeMirror type) {
            if (type == null || type.getKind() != javax.lang.model.type.TypeKind.DECLARED) {
                return null;
            }
            Element el = ((DeclaredType) type).asElement();
            return el instanceof TypeElement te ? te : null;
        }

        private static String externalTypeId(TypeElement el) {
            return "type:" + el.getQualifiedName().toString();
        }

        private static String externalModule(TypeElement el) {
            // We don't know which Gradle module an external type lives in; flag as external.
            return null;
        }

        private static String packageOf(TypeElement el) {
            Element enc = el.getEnclosingElement();
            while (enc != null && enc.getKind() != ElementKind.PACKAGE) {
                enc = enc.getEnclosingElement();
            }
            if (enc == null) {
                return "";
            }
            return enc.toString();
        }

        private static String ownerQualifiedName(Element el) {
            Element enc = el.getEnclosingElement();
            if (enc instanceof TypeElement te) {
                return te.getQualifiedName().toString();
            }
            return enc == null ? "" : enc.toString();
        }

        private static String ownerTypeId(Element el) {
            Element enc = el.getEnclosingElement();
            if (enc instanceof TypeElement te) {
                return externalTypeId(te);
            }
            return null;
        }

        private static List<String> modifiersOf(Element el) {
            List<String> out = new ArrayList<>();
            for (Modifier m : el.getModifiers()) {
                out.add(m.toString().toLowerCase());
            }
            Collections.sort(out);
            return out;
        }

        private static String signature(Element methodOrCtor) {
            // Best-effort signature string used to disambiguate overloads.
            // Uses erased return type + simple parameter type names.
            StringBuilder sb = new StringBuilder("(");
            javax.lang.model.element.ExecutableElement ee = (javax.lang.model.element.ExecutableElement) methodOrCtor;
            for (var p : ee.getParameters()) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                sb.append(p.asType().toString());
            }
            sb.append(")");
            return sb.toString();
        }
    }
}
