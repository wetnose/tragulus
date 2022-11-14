package wn.pseudoclasses;

import com.sun.tools.javac.code.Type;
import wn.tragulus.JavacUtils;
import wn.tragulus.ProcessingHelper;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Alexander A. Solovioff
 * Date: 07.05.2021
 * Time: 5:01 PM
 */
class Utils {


    static boolean isMarkedAsPseudo(TypeElement type) {
        return type != null && type.getAnnotation(Pseudo.class) != null;
    }


    static boolean isPseudoclass(TypeElement type) {
        return type != null && (isMarkedAsPseudo(type) || isPseudoclass(JavacUtils.asElement(type.getSuperclass())));
    }


    static TypeMirror supertypeOf(ProcessingHelper helper, TypeElement type) {
        TypeMirror superclass = helper.getSupertype(type.asType());
        if (superclass.getKind() == TypeKind.ERROR) {
            return ((Type.ErrorType) superclass).getOriginalType();
        }
        return superclass;
    }
}
