package wn.tragulus;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.type.TypeKind.DECLARED;


/**
 * Created by Alexander A. Solovioff
 * 31.07.2018
 */
public abstract class BasicProcessor extends AbstractProcessor {

    protected ProcessingHelper helper;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        helper = new ProcessingHelper(processingEnv);
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        SupportedAnnotationTypes sat = this.getClass().getAnnotation(SupportedAnnotationTypes.class);
        return sat == null ? Collections.singleton("*") : super.getSupportedAnnotationTypes();
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        SupportedSourceVersion ssv = this.getClass().getAnnotation(SupportedSourceVersion.class);
        return ssv == null ? SourceVersion.RELEASE_8 : super.getSupportedSourceVersion();
    }


    protected final java.util.List<TypeElement> collectClasses(RoundEnvironment env) {
        return env.getRootElements().stream()
                .filter(e -> e.getKind() == CLASS)
                .map(e -> (TypeElement) e)
                .collect(Collectors.toList());
    }


    protected final java.util.List<TypeElement> collectClassesAndInterfaces(RoundEnvironment env) {
        return env.getRootElements().stream()
                .filter(e -> {
                    ElementKind kind = e.getKind();
                    return kind == CLASS || kind == INTERFACE;
                })
                .map(e -> (TypeElement) e)
                .collect(Collectors.toList());
    }


    protected final java.util.List<CompilationUnitTree> collectCompilationUnits(RoundEnvironment env, Predicate<CompilationUnitTree> filter) {
        return collectClasses(env).stream().map(helper::getUnit)
                .distinct()
                .filter(filter)
                .collect(Collectors.toList());
    }


    protected static Predicate<CompilationUnitTree> importFilter(TypeMirror type) {
        Name pkg = type.getKind() == DECLARED
                ? ((PackageElement) ((DeclaredType) type).asElement().getEnclosingElement()).getQualifiedName()
                : null;
        return u -> {
            if (pkg != null && pkg == JavacUtils.packageOf(u).getQualifiedName()) return true;
            return u.getImports().stream().anyMatch(imp -> {
                Tree id = imp.getQualifiedIdentifier();
                if (imp.isStatic()) {
                    id = ((MemberSelectTree) id).getExpression();
                }
                return JavacUtils.typeOf(id) == type;
            });
        };
    }


    protected static Predicate<CompilationUnitTree> importFilter(Predicate<ImportTree> filter) {
        return u -> u.getImports().stream().anyMatch(filter);
    }
}
