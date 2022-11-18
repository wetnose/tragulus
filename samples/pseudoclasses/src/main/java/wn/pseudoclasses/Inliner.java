package wn.pseudoclasses;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import wn.pseudoclasses.ProcessingHelper.PseudoType;
import wn.pseudoclasses.ProcessingHelper.TypeWrapper;
import wn.tragulus.Editors;
import wn.tragulus.TreeAssembler;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Alexander A. Solovioff
 * Date: 19.11.2022
 * Time: 2:00 AM
 */
class Inliner {

    void inline(PseudoType type) {

        if (type instanceof TypeWrapper) {
            ProcessingHelper helper = type.helper();
            TreeAssembler asm = helper.newAssembler();

            TypeMirror replace = ((TypeWrapper) type).wrappedType;
            type.units.forEach(unit -> {
                unit.getTypeDecls().forEach(tree -> {
                    ((ClassTree) tree).getMembers().forEach(member -> {
                        if (member.getKind() != Tree.Kind.VARIABLE) return;
                        VariableElement var = helper.asElement(unit, member);
                        TypeElement varType = helper.asElement(var.asType());
                        if (type.elem == varType) {
                            Editors.setType((VariableTree) member, asm.type(replace).asExpr());
                        }
                    });
                });
            });
        }

    }
}
