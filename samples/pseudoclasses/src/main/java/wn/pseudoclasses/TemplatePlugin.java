package wn.pseudoclasses;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * Alexander A. Solovioff
 * Date: 18.11.2022
 * Time: 2:00 AM
 */
public class TemplatePlugin implements SpecialPlugin {


    @Override
    public boolean accept(ProcessingHelper helper, TypeElement type) {
        return type.getKind() == ElementKind.INTERFACE;
    }


    @Override
    public boolean validate(ProcessingHelper helper, TypeElement type) {
        return true;
    }


    @Override
    public void process(ProcessingHelper helper, List<TypeUsages> usages) {

    }
}
