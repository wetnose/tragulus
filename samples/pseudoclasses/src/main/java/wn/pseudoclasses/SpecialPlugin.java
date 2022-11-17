package wn.pseudoclasses;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 12:45 AM
 */
interface SpecialPlugin extends Plugin {

    boolean accept(ProcessingHelper helper, TypeElement type);

    default boolean accept(ProcessingHelper helper, TypeMirror type) {
        return accept(helper, (TypeElement) helper.asElement(type));
    }
}
