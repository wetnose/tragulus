package wn.pseudoclasses;

import wn.tragulus.JavacUtils;

import javax.lang.model.element.TypeElement;

/**
 * Alexander A. Solovioff
 * Date: 07.05.2021
 * Time: 5:01 PM
 */
class Utils {


    static boolean isMarkedAsPseudo(TypeElement type) {
        return type.getAnnotation(Pseudo.class) != null;
    }


    static boolean isPseudoclass(TypeElement type) {
        return type != null && (isMarkedAsPseudo(type) || isPseudoclass(JavacUtils.asElement(type.getSuperclass())));
    }
}
