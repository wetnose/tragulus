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

import javax.annotation.processing.ProcessingEnvironment;
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
import java.util.stream.Stream;

import static wn.pseudoclasses.ProcessingHelper.isMarkedAsPseudo;
import static wn.pseudoclasses.ProcessingHelper.isPseudoclass;


/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:15 PM
 */
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class Processor extends BasicProcessor {

    private static final Plugin DEFAULT_PLUGIN = new DefaultPlugin();
    private static final SpecialPlugin[] SPECIAL_PLUGINS = {new WrapperPlugin()};

    ProcessingHelper helper;
    Map<TypeMirror,SpecialPlugin> specialPlugins;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        helper = new ProcessingHelper(processingEnv);
        specialPlugins = Stream.of(SPECIAL_PLUGINS)
                .collect(Collectors.toMap(p -> p.basicType(helper), p -> p));
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (roundEnv.getRootElements().isEmpty()) return false;

        Trees trees = helper.getTreeUtils();

//        Map<TypeMirror, Plugin> plugins = new IdentityHashMap<>(SPECIAL_PLUGINS.length);
//        for (SpecialPlugin plugin : SPECIAL_PLUGINS) {
//            plugins.put(helper.asType(plugin.basicType()), plugin);
//        }

        List<TypeElement> classes = collectClasses(roundEnv);
        LinkedHashSet<TypeElement> pseudoclasses = new LinkedHashSet<>();

        // find pseudoclasses
        classes.forEach(type -> {
            helper.printNote(type + ": isPseudo = " + isPseudoclass(type));
            TypeMirror supertype = helper.getSupertype(type.asType());
            TypeElement superclass = helper.asElement(supertype);
            boolean markedAsPseudo = isMarkedAsPseudo(type);
            boolean extendsPseudotype = isMarkedAsPseudo(superclass);
            boolean extendsPrimitive = supertype != null && supertype.getKind().isPrimitive();
            if (!markedAsPseudo) {
                if (extendsPseudotype || extendsPrimitive) {
                    helper.printError("Missing @Pseudo annotation", type);
                } else {
                    return;
                }
            } else {
                if (!extendsPseudotype) {
                    CompilationUnitTree unit = helper.getUnit(type);
                    JavaFileObject src = unit.getSourceFile();
                    if (superclass != null && helper.isFinal(superclass)) {
                        helper.suppressCantInheritFromFinal(diag -> diag.getSource() == src);
                    } else
                    if (extendsPrimitive) {
                        helper.suppressPrimTypeArg(diag -> diag.getSource() == src);
                    }
                }
            }
//            distribution.compute(DEFAULT_PLUGIN, (plugin, list) -> list != null ? list : new ArrayList<>()).add(type);
//            if (type.getSuperclass() == helper.wrapperType) {
//                distribution.compute(WRAPPER_PLUGIN, (plugin, list) -> list != null ? list : new ArrayList<>()).add(type);
//            }
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

            helper.suppressPrimTypeArg(diag -> {
                Tree tree = JavacUtils.getTree(diag);
                return parametrizedTypePos.contains(tree);
            });
        }

        helper.printNote(pseudoclasses.toString());

        Map<Plugin,List<TypeUsages>> usages = new HashMap<>();

        Set<TypeMirror> pseudoTypes = pseudoclasses.stream().map(Element::asType).collect(Collectors.toSet());
        Set<TypeMirror> validated = new LinkedHashSet<>(pseudoclasses.size());

        distribution: {

            class Validator implements Consumer<TypeMirror> {
                boolean valid = true;
                @Override
                public void accept(TypeMirror t) {
                    if (validated.contains(t)) return;
                    TypeMirror supertype = helper.getSupertype(t);
                    if (pseudoTypes.contains(supertype)) accept(supertype);
                    TypeElement element = helper.asElement(t);
                    if (!DEFAULT_PLUGIN.validate(helper, element)) {
                        SpecialPlugin plugin = specialPlugins.get(helper.getBaseType(element).asType());
                        valid = plugin == null || plugin.validate(helper, element);
                    }
                    validated.add(t);
                }
            }

            Validator validator = new Validator();
            pseudoTypes.forEach(validator);

            if (!validator.valid) break distribution;

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

            usages.put(DEFAULT_PLUGIN, new ArrayList<>(typeUsages.values()));
            typeUsages.forEach((type, using) -> {
                SpecialPlugin plugin = specialPlugins.get(helper.getBaseType(type).asType());
                if (plugin == null) return;
                usages.compute(plugin, (p, use) -> use != null ? use : new ArrayList<>()).add(using);
            });
        }

        if (helper.getDiagnosticQ().stream().noneMatch(diag -> diag.getKind() == Diagnostic.Kind.ERROR)) {
            usages.forEach((plugin, use) -> plugin.process(helper, use));
        }


        pseudoclasses.forEach(type -> {
            Tree tree = helper.getTreeUtils().getTree(type);
            if (tree == null) throw new AssertionError();
            CompilationUnitTree unit = helper.getUnit(type);
            Editors.filterTree(unit, true, node -> node != tree);
//            try {
//                System.out.println("--------" + unit.getSourceFile());
//                new com.sun.tools.javac.tree.Pretty(new OutputStreamWriter(System.out), true).print(unit);
//            } catch (IOException e) {
//                throw new AssertionError(e);
//            }
        });

        return false;
    }
}
