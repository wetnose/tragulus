package wn.pseudoclasses;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import wn.tragulus.JavacUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Alexander A. Solovioff
 * Date: 17.11.2022
 * Time: 3:58 AM
 */
class ProcessingHelper extends wn.tragulus.ProcessingHelper {

    enum Err {

        INHERIT_FROM_FINAL   ("compiler.err.cant.inherit.from.final"),
        PRIM_TYPE_ARG        ("compiler.err.type.found.req"),
        OVERRIDES_OBJ_MEMBER ("compiler.err.default.overrides.object.member"),

        ;

        final String code;

        Err(String code) {
            this.code = code;
        }
    }


    TypeMirror objectType;
    TypeMirror wrapperType;
    Set<TypeMirror> specialTypes;


    public ProcessingHelper(ProcessingEnvironment processingEnv) {
        super(processingEnv);
        objectType = asType(Object.class);
        wrapperType = asType(Wrapper.class);
        specialTypes = Set.of(wrapperType);
    }


    static boolean isMarkedAsPseudo(TypeElement type) {
        return type != null && type.getAnnotation(Pseudo.class) != null;
    }


    boolean isPseudoclass(TypeElement type) {
        if (type == null) return false;
        if (specialTypes.contains(type.asType())) return true;
        return isMarkedAsPseudo(type) || isPseudoclass(asElement(type.getSuperclass()));
    }


    boolean isSpecial(Element type) {
        return type != null && type.getKind() == ElementKind.CLASS && specialTypes.contains(type.asType());
    }


    boolean isSpecial(TypeMirror type) {
        return type != null && specialTypes.contains(type);
    }


    TypeElement getBaseType(TypeElement type) {
        return asElement(type.getSuperclass());
    }


    TypeElement getBaseType(TypeMirror type) {
        return getBaseType((TypeElement) asElement(type));
    }


    @Override
    public TypeMirror getSupertype(TypeMirror type) {
        TypeMirror superclass = super.getSupertype(type);
        if (superclass.getKind() == TypeKind.ERROR) {
            return ((Type.ErrorType) superclass).getOriginalType();
        }
        return superclass;
    }


    public TypeElement getSupertype(TreePath typePath) {
        if (typePath.getLeaf().getKind() != Tree.Kind.CLASS) return null;
        ClassTree classTree = (ClassTree) typePath.getLeaf();
        Tree extendsClause= classTree.getExtendsClause();
        return extendsClause == null ? null : asElement(TreePath.getPath(typePath, extendsClause));
    }


    void suppressDiagnostics(Err err, Predicate<Diagnostic<JavaFileObject>> filter) {
        filterDiagnostics(diag -> diag.getCode().equals(err.code) && filter.test(diag));
    }


    void suppressDiagnostics(Err err, JavaFileObject src) {
        filterDiagnostics(diag -> diag.getCode().equals(err.code) && diag.getSource() == src);
    }


    void suppressDiagnostics(Err err, Tree tree) {
        filterDiagnostics(diag -> diag.getCode().equals(err.code) && JavacUtils.getTree(diag) == tree);
    }
}
