package wn.pseudoclasses;

import com.sun.tools.javac.code.Type;
import wn.tragulus.JavacUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.function.Predicate;

/**
 * Alexander A. Solovioff
 * Date: 17.11.2022
 * Time: 3:58 AM
 */
class ProcessingHelper extends wn.tragulus.ProcessingHelper {

    private static final String ERR_INHERIT_FROM_FINAL = "compiler.err.cant.inherit.from.final";
    private static final String ERR_PRIM_TYPE_ARG      = "compiler.err.type.found.req";

    TypeMirror wrapperType;


    public ProcessingHelper(ProcessingEnvironment processingEnv) {
        super(processingEnv);
        wrapperType = asType(Wrapper.class);
    }


    static boolean isMarkedAsPseudo(TypeElement type) {
        return type != null && type.getAnnotation(Pseudo.class) != null;
    }


    static boolean isPseudoclass(TypeElement type) {
        return type != null && (isMarkedAsPseudo(type) || isPseudoclass(JavacUtils.asElement(type.getSuperclass())));
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


    void suppressCantInheritFromFinal(Predicate<Diagnostic<JavaFileObject>> filter) {
        filterDiagnostics(diag -> diag.getCode().equals(ERR_INHERIT_FROM_FINAL) && filter.test(diag));
    }


    void suppressPrimTypeArg(Predicate<Diagnostic<JavaFileObject>> filter) {
        filterDiagnostics(diag -> diag.getCode().equals(ERR_PRIM_TYPE_ARG) && filter.test(diag));
    }
}
