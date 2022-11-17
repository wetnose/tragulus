package wn.pseudoclasses;

import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 12:45 AM
 */
interface Plugin {

    boolean validate(ProcessingHelper helper, TypeElement type);

    void process(ProcessingHelper helper, List<TypeUsages> usages);
}
