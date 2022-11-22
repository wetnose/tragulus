package wn.pseudoclasses;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import wn.pseudoclasses.Pseudos.Extension;
import wn.pseudoclasses.Pseudos.PseudoType;
import wn.tragulus.Editors;
import wn.tragulus.JavacUtils.TreeWalker;
import wn.tragulus.ProcessingHelper;
import wn.tragulus.TreeAssembler;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.function.Consumer;

import static wn.tragulus.JavacUtils.walkOver;

/**
 * Alexander A. Solovioff
 * Date: 19.11.2022
 * Time: 2:00 AM
 */
class Inliner {

    void inline(PseudoType type) {

        if (type instanceof Extension) {
            //System.out.println("---- test " + type.elem);
            Pseudos pseudo = type.helper();
            ProcessingHelper helper = pseudo.helper;
            TreeAssembler asm = helper.newAssembler();

            TypeMirror replace = ((Extension) type).wrappedType;
            class Walker implements Consumer<TreeWalker> {
                @Override
                public void accept(TreeWalker walker) {
                    TreePath path = walker.path();
                    Tree node = path.getLeaf();
                    System.out.println(node + " " + node.getKind());
                    switch (node.getKind()) {
                        case VARIABLE:
                            VariableTree var = (VariableTree) node;
                            Tree tt = var.getType();
                            if (helper.typeOf(path, var.getType()) == type.elem.asType()) {
                                Editors.setType(var, asm.type(replace).asExpr());
                            }
                            break;
//                        case IDENTIFIER:
//                            Element element = helper.asElement(path);
//                            System.out.print(node);
//                            if (element != null) {
//                                Element enc = helper.asElement(path.getParentPath());
//                                if (enc != null) System.out.print(" " + enc + " " + enc.getKind());
//                            }
//                            System.out.println();
//                            break;
                    }
                }
            }

            Walker walker = new Walker();
            type.units.forEach(unit -> {
                walkOver(unit, walker);

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
