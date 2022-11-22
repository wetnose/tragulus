package wn.pseudoclasses;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import wn.pseudoclasses.ProcessingHelper.PseudoType;
import wn.tragulus.BasicProcessor;
import wn.tragulus.Editors;
import wn.tragulus.JavacUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:15 PM
 */
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class Processor extends BasicProcessor {

    final Inliner inliner = new Inliner();

    ProcessingHelper helper;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        helper = new ProcessingHelper(processingEnv);
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        if (env.getRootElements().isEmpty()) return false;

        Trees trees = helper.getTreeUtils();

        ArrayList<PseudoType> pseudotypes = new ArrayList<>();

        for (Element element : env.getRootElements()) {
            switch (element.getKind()) {
                case CLASS:
                case INTERFACE:
                    PseudoType pt = helper.pseudoTypeOf(trees.getPath(element));
                    if (pt != null) pseudotypes.add(pt);
            }
        }

        pseudotypes.forEach(System.out::println);

        Map<CompilationUnitTree,List<PseudoType>> usages = new HashMap<>();

        boolean valid = helper.noErrorReports();

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
            pseudotypes.forEach(inliner::inline);
        }

        pseudotypes.forEach(type -> {
            Tree tree = type.path.getLeaf();
            if (tree == null) throw new AssertionError();
            CompilationUnitTree unit = type.path.getCompilationUnit();
            Editors.filterTree(unit, true, node -> node != tree);
        });

        return false;
    }
}
