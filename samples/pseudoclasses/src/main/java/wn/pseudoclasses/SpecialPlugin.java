package wn.pseudoclasses;

import javax.lang.model.type.TypeMirror;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 12:45 AM
 */
interface SpecialPlugin extends Plugin {

    TypeMirror basicType(ProcessingHelper helper);
}
