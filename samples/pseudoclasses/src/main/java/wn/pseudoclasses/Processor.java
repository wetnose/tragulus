package wn.pseudoclasses;

import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import wn.tragulus.BasicProcessor;
import wn.tragulus.JavacUtils;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.lang.model.element.ElementKind.CLASS;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:15 PM
 */
public class Processor extends BasicProcessor {

    private static final Class<?>[] PSEUDOTYPES = {Wrapper.class};


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (roundEnv.getRootElements().isEmpty()) return false;

        Name pseudoclassesPkgName = helper.getName("wn.pseudoclasses");
        TypeMirror object = helper.asType(Object.class);

        List<TypeElement> pseudoClasses = collectClasses(roundEnv);

        Set<TypeMirror> pseudotypes = Stream.of(PSEUDOTYPES).map(helper::asType).collect(Collectors.toSet());

        pseudoClasses.forEach(type -> {
            TypeMirror superclass = type.getSuperclass();
            boolean markedAsPseudo = type.getAnnotation(Pseudo.class) != null;
            boolean extendsPseudotype = superclass != object
                    && (pseudotypes.contains(superclass) || superclass.getAnnotation(Pseudo.class) != null);
            if (extendsPseudotype != markedAsPseudo && superclass != object) {
                if (markedAsPseudo) {
                    helper.printError("Pseudo superclass expected", type);
                } else {
                    helper.printError("Missing @Pseudo annotation", type);
                }
            }
//            Name superclassPkgName = helper.getName(superclass == null ? null : helper.asElement(superclass).getEnclosingElement());
//            System.out.println(superclassPkgName);
//            System.out.println("pseudoclass " + type + ": " + superclass + " (" + (superclassPkgName == pseudoclassesPkgName) + ')');
        });


//        Name pseudoclassesPkgName = helper.getName("wn.pseudoclasses");
//        HashMap<Name,TypeElement> pseudoclassTree = new HashMap<>();
//
//        List<TypeElement> classes = collectClasses(roundEnv);
//
//
//
//
//
//        Predicate<ImportTree> pseudoImport = imp -> {
//            Tree id = imp.getQualifiedIdentifier();
//            if (imp.isStatic()) {
//                MemberSelectTree ref = (MemberSelectTree) id;
//                ref.getIdentifier().
//                return ref.getIdentifier() == optName && JavacUtils.typeOf(ref.getExpression()) == operatorsType;
//            } else {
//                return JavacUtils.typeOf(id) == operatorsType;
//            }
//        };

        return false;
    }
}
