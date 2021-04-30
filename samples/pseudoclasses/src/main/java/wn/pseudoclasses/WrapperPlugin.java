package wn.pseudoclasses;

import wn.tragulus.ProcessingHelper;

import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 12:42 AM
 */
class WrapperPlugin implements SpecialPlugin {

    @Override
    public Class<?> basicType() {
        return Wrapper.class;
    }


    @Override
    public void process(ProcessingHelper helper, List<TypeElement> types) {

    }
}
