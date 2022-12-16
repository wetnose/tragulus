package wn.pseudoclasses;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import wn.pseudoclasses.Pseudos.Extension;
import wn.pseudoclasses.Pseudos.PseudoType;
import wn.tragulus.BasicProcessor;
import wn.tragulus.Editors;
import wn.tragulus.JavacUtils;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:15 PM
 */
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class Processor extends BasicProcessor {


    final Listener listener;


    public Processor() {
        this(null);
    }


    Processor(Listener listener) {
        this.listener = listener;
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        if (env.getRootElements().isEmpty()) return false;

        Pseudos pseudos = new Pseudos(helper);
        ArrayList<PseudoType> pseudotypes = new ArrayList<>();

        Trees trees = helper.getTreeUtils();
        env.getRootElements().stream().map(trees::getPath).forEach(new Consumer<>() {
            @Override
            public void accept(TreePath path) {
                Tree node = path.getLeaf();
                switch (node.getKind()) {
                    case CLASS:
                    case INTERFACE:
                        PseudoType pt = pseudos.pseudoTypeOf(path);
                        if (pt != null) pseudotypes.add(pt);
                        ((ClassTree) node).getMembers().forEach(member -> accept(new TreePath(path, member)));
                }
            }
        });

        pseudotypes.forEach(System.out::println);

        Map<CompilationUnitTree,List<PseudoType>> usages = new HashMap<>();

        boolean valid = pseudos.validate();

        distribution: {

            if (!valid) break distribution;

            Map<TypeMirror,PseudoType> mirrors = pseudotypes.stream()
                    .collect(Collectors.toMap(t -> t.elem.asType(), t -> t));

            BiConsumer<PseudoType,CompilationUnitTree> distribute = (ref, unit) -> {
                ref.add(unit);
                usages.compute(unit, (u, list) -> list != null ? list : new ArrayList<>()).add(ref);
            };

            collectCompilationUnits(env, unit -> true).forEach(unit -> {
                unit.getImports().forEach(imp -> {
                    Tree id = imp.getQualifiedIdentifier();
                    TypeMirror ref = JavacUtils.typeOf(imp.isStatic() ? ((MemberSelectTree) id).getExpression() : id);
                    PseudoType pt = mirrors.get(ref);
                    if (pt != null) {
                        distribute.accept(pt, unit);
                    }
                });

                ExpressionTree unitPkg = unit.getPackageName();
                pseudotypes.forEach(type -> {
                    ExpressionTree refPkg = type.path.getCompilationUnit().getPackageName();
                    if (Objects.equals(refPkg, unitPkg)) {
                        distribute.accept(type, unit);
                    }
                });

            });
        }

        if (valid && helper.noErrorReports()) {
            Inliner inliner = new Inliner(pseudos);
            preprocessExtensions(pseudos, inliner);
            pseudos.all().stream()
                    .flatMap(t -> t.units.stream())
                    .distinct()
                    .forEach(unit -> {
                        inliner.inline(new TreePath(unit));
                        if (listener != null) {
                            JavacUtils.scan(unit, t -> {
                                if (t.getKind() == Tree.Kind.CLASS) {
                                    ClassTree clazz = (ClassTree) t;
                                    listener.onInlined(clazz.getSimpleName().toString(), clazz.toString());
                                }
                            });
                        }
                    });
        }

        pseudotypes.forEach(type -> {
            Tree tree = type.path.getLeaf();
            if (tree == null) throw new AssertionError();
            CompilationUnitTree unit = type.path.getCompilationUnit();
            Editors.filterTree(unit, true, node -> node != tree);
        });

        return false;
    }


    void preprocessExtensions(Pseudos pseudos, Inliner inliner) {
        Trees trees = pseudos.trees;

        Map<TypeMirror, Extension> extensions;
        extensions = pseudos.all().stream()
                .filter(t -> t instanceof Extension)
                .collect(Collectors.toMap(t -> t.elem.asType(), t -> (Extension) t));

        HashMap<Element,MethodDesc> methods = new HashMap<>();
        extensions.values().stream()
                .flatMap(e -> Stream.concat(e.constructors.stream(), e.methods.stream()))
                .forEach(m -> methods.put(m.elem, new MethodDesc(m)));

        TreePathScanner<Void,MethodDesc> dependencyCollector = new TreePathScanner<>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, MethodDesc desc) {
                TreePath path = new TreePath(getCurrentPath(), node.getMethodSelect());
                Element elem = trees.getElement(path);
                MethodDesc dep = methods.get(elem);
                if (dep != null) desc.deps.add(new Dep(dep, path));
                return super.visitMethodInvocation(node, desc);
            }
        };

        methods.values().forEach(desc -> dependencyCollector.scan(desc.mthd.path, desc));

        LinkedHashSet<MethodDesc> ordered = new LinkedHashSet<>(methods.size());
        LinkedList<MethodDesc> queue = new LinkedList<>();
        HashSet<MethodDesc> passed = new HashSet<>();
        methods.values().forEach(d -> {
            if (!passed.add(d)) return;
            queue.add(d);
            MethodDesc desc;
            while ((desc = queue.peek()) != null) {
                boolean added = false;
                for (Dep dep : desc.deps) {
                    MethodDesc dd = dep.desc;
                    if (ordered.contains(dd)) continue;
                    if (passed.add(dd)) {
                        queue.addFirst(dd);
                        added = true;
                    } else {
                        helper.printError("prohibited recursion", dep.path);
                    }
                }
                if (added) continue;
                ordered.add(queue.poll());
            }
        });

        for (MethodDesc desc : ordered) {
            if (desc.deps.isEmpty()) continue;
            inliner.inline(desc.mthd.path);
        }
    }


    private static class MethodDesc {
        final Pseudos.Method mthd;
        final HashSet<Dep> deps = new HashSet<>();
        MethodDesc(Pseudos.Method mthd) {
            this.mthd = mthd;
        }
        @Override
        public String toString() {
            return mthd.elem.getEnclosingElement() + "." + mthd.elem;
        }
    }

    private static class Dep {
        final MethodDesc desc;
        final TreePath path;
        Dep(MethodDesc desc, TreePath path) {
            this.desc = desc;
            this.path = path;
        }
    }


    interface Listener {
        void onInlined(String className, String patchedSrc);
    }
}
