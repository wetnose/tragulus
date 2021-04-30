package wn.pseudoclasses;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeTranslator;
import wn.tragulus.BasicProcessor;
import wn.tragulus.Editors;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:15 PM
 */
public class Processor extends BasicProcessor {

    private static final Plugin DEFAULT_PLUGIN = new DefaultPlugin();
    private static final SpecialPlugin[] SPECIAL_PLUGINS = {new WrapperPlugin()};


    static boolean isMarkedAsPseudo(AnnotatedConstruct type) {
        return type.getAnnotation(Pseudo.class) != null;
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (roundEnv.getRootElements().isEmpty()) return false;

      //Name pseudoclassesPkgName = helper.getName("wn.pseudoclasses");
        TypeMirror object = helper.asType(Object.class);

        Map<TypeMirror, Plugin> plugins = new IdentityHashMap<>(SPECIAL_PLUGINS.length);
        for (SpecialPlugin plugin : SPECIAL_PLUGINS) {
            plugins.put(helper.asType(plugin.basicType()), plugin);
        }

        List<TypeElement> classes = collectClasses(roundEnv);
        Map<Plugin,List<Symbol>> distribution = new HashMap<>();


        classes.forEach(type -> {
            TypeMirror superclass = type.getSuperclass();
            boolean markedAsPseudo = isMarkedAsPseudo(type);
            boolean extendsPseudotype = isMarkedAsPseudo(superclass);
            if (extendsPseudotype && !markedAsPseudo) {
                helper.printError("Missing @Pseudo annotation", type);
                return;
            }
            if (!markedAsPseudo) return;
            if (!extendsPseudotype) {
                int fieldCount = (int) type.getEnclosedElements().stream()
                        .filter(member -> member.getKind() == ElementKind.FIELD)
                        .peek(filed -> helper.printError("Filed declaration is not allowed here", filed))
                        .count();
                if (fieldCount != 0) return;
            }
            if (!(type instanceof Symbol)) throw new AssertionError();
            distribution.compute(DEFAULT_PLUGIN, (plugin, list) -> list != null ? list : new ArrayList<>())
                    .add((Symbol) type);
        });

        System.out.println(distribution);

        distribution.forEach((plugin, types) -> types.forEach(t -> {
            Tree tree = helper.getTreeUtils().getTree(t);
            if (tree == null) throw new AssertionError();
            CompilationUnitTree unit = helper.getUnit((TypeElement) t);
            Editors.filterTree(unit, true, node -> node != tree);
        }));

        return false;
    }
}
