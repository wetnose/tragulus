package wn.pseudoclasses;

import com.sun.source.tree.CompilationUnitTree;

import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.Set;

/**
 * Alexander A. Solovioff
 * Date: 07.05.2021
 * Time: 3:33 AM
 */
class TypeUsages {

    final TypeMirror type;
    final Set<CompilationUnitTree> units = new HashSet<>();


    TypeUsages(TypeMirror type) {
        this.type = type;
    }

    void add(CompilationUnitTree unit) {
        units.add(unit);
    }
}
