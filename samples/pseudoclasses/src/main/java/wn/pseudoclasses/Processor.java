package wn.pseudoclasses;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import wn.tragulus.BasicProcessor;
import wn.tragulus.Editors;
import wn.tragulus.JavacUtils;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static wn.pseudoclasses.Utils.isMarkedAsPseudo;
import static wn.pseudoclasses.Utils.isPseudoclass;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:15 PM
 */
@SupportedSourceVersion(SourceVersion.RELEASE_9)
public class Processor extends BasicProcessor {

    private static final Plugin DEFAULT_PLUGIN = new DefaultPlugin();
//    private static final SpecialPlugin[] SPECIAL_PLUGINS = {new WrapperPlugin()};

    private static final String ERR_INHERIT_FROM_FINAL = "compiler.err.cant.inherit.from.final";
    private static final String ERR_PRIM_TYPE_ARG      = "compiler.err.type.found.req";


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (roundEnv.getRootElements().isEmpty()) return false;

        Trees trees = helper.getTreeUtils();

//        Map<TypeMirror, Plugin> plugins = new IdentityHashMap<>(SPECIAL_PLUGINS.length);
//        for (SpecialPlugin plugin : SPECIAL_PLUGINS) {
//            plugins.put(helper.asType(plugin.basicType()), plugin);
//        }

        List<TypeElement> classes = collectClasses(roundEnv);
        Map<Plugin,List<TypeElement>> distribution = new HashMap<>();
        Set<TypeElement> pseudoclasses = new HashSet<>();

        // find pseudoclasses
        classes.forEach(type -> {
            System.out.println(type + ": isPseudo = " + isPseudoclass(type));
            TypeElement superclass = helper.asElement(type.getSuperclass());
            boolean markedAsPseudo = isMarkedAsPseudo(type);
            boolean extendsPseudotype = isMarkedAsPseudo(superclass);
            if (!markedAsPseudo) {
                if (extendsPseudotype) {
                    helper.printError("Missing @Pseudo annotation", type);
                } else {
                    return;
                }
            } else {
                if (!extendsPseudotype) {
                    if (helper.isFinal(superclass)) {
                        CompilationUnitTree unit = helper.getUnit(type);
                        JavaFileObject src = unit.getSourceFile();
                        helper.filterDiagnostics(d -> d.getSource() == src && d.getCode().equals(ERR_INHERIT_FROM_FINAL));
                    }
                }
            }
            distribution.compute(DEFAULT_PLUGIN, (plugin, list) -> list != null ? list : new ArrayList<>()).add(type);
            pseudoclasses.add(type);
        });

        { // remove errors for expressions like "Type<int>"
            Set<Tree> parametrizedTypePos = new HashSet<>();

            classes.forEach(type -> {
                JavacUtils.scan(trees.getTree(type), node -> {
                    if (node.getKind() != Tree.Kind.PARAMETERIZED_TYPE) return;
                    ParameterizedTypeTree parType = (ParameterizedTypeTree) node;
                    if (!pseudoclasses.contains(helper.<TypeElement>asElement(JavacUtils.typeOf(parType)))) return;
                    parType.getTypeArguments().forEach(arg -> {
                        if (arg.getKind() == Tree.Kind.PRIMITIVE_TYPE) parametrizedTypePos.add(arg);
                    });
                });
            });

            helper.filterDiagnostics(diag -> {
                Tree tree = JavacUtils.getTree(diag);
                return diag.getCode().equals(ERR_PRIM_TYPE_ARG) && parametrizedTypePos.contains(tree);
            });
        }

        System.out.println(distribution);

        Map<Plugin,List<TypeUsages>> usages = new HashMap<>();

        distribution.forEach((plugin, types) -> {

            Set<TypeMirror> pseudoTypes = types.stream().map(Element::asType).collect(Collectors.toSet());
            Set<TypeMirror> validated = new LinkedHashSet<>(pseudoTypes.size());

            class Validator implements Consumer<TypeMirror> {
                boolean valid = true;
                @Override
                public void accept(TypeMirror t) {
                    if (validated.contains(t)) return;
                    TypeMirror supertype = helper.getSupertype(t);
                    if (pseudoTypes.contains(supertype)) accept(supertype);
                    if (!plugin.validate(helper, helper.asElement(t))) valid = false;
                    validated.add(t);
                }
            }

            Validator validator = new Validator();
            pseudoTypes.forEach(validator);

            if (!validator.valid) return;

            Map<TypeMirror,TypeUsages> typeUsages = new HashMap<>();
            BiConsumer<TypeMirror,CompilationUnitTree> distribute = (ref, unit) ->
                    typeUsages.compute(ref, (type, using) -> using != null ? using : new TypeUsages(type)).add(unit);

            collectCompilationUnits(roundEnv, unit -> true).forEach(unit -> {
                unit.getImports().forEach(imp -> {
                    Tree id = imp.getQualifiedIdentifier();
                    TypeMirror ref = JavacUtils.typeOf(imp.isStatic() ? ((MemberSelectTree) id).getExpression() : id);
                    if (pseudoTypes.contains(ref)) {
                        distribute.accept(ref, unit);
                    }
                });

                ExpressionTree unitPkg = unit.getPackageName();
                pseudoTypes.forEach(ref -> {
                    ExpressionTree refPkg = helper.getUnit(ref).getPackageName();
                    if (Objects.equals(refPkg, unitPkg)) {
                        distribute.accept(ref, unit);
                    }
                });

            });

            usages.put(plugin, new ArrayList<>(typeUsages.values()));
        });

        if (helper.getDiagnosticQ().stream().noneMatch(diag -> diag.getKind() == Diagnostic.Kind.ERROR)) {
            usages.forEach((plugin, use) -> plugin.process(helper, use));
        }


        distribution.forEach((plugin, types) -> types.forEach(t -> {
            Tree tree = helper.getTreeUtils().getTree(t);
            if (tree == null) throw new AssertionError();
            CompilationUnitTree unit = helper.getUnit(t);
            Editors.filterTree(unit, true, node -> node != tree);
//            try {
//                System.out.println("--------" + unit.getSourceFile());
//                new com.sun.tools.javac.tree.Pretty(new OutputStreamWriter(System.out), true).print(unit);
//            } catch (IOException e) {
//                throw new AssertionError(e);
//            }
        }));

        return false;
    }
}
