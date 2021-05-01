package wn.pseudoclasses;

import com.sun.source.tree.CompilationUnitTree;
import wn.tragulus.ProcessingHelper;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.Set;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 12:45 AM
 */
interface Plugin {

    boolean validate(ProcessingHelper helper, TypeElement type);

    void process(ProcessingHelper helper, Map<TypeMirror, Set<CompilationUnitTree>>usages);
}
