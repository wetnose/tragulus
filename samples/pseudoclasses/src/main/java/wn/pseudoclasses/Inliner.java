package wn.pseudoclasses;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
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
import javax.lang.model.util.Types;
import java.util.function.Consumer;

import static wn.pseudoclasses.Pseudos.Err.CANNOT_CAST;
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
            Pseudos pseudos = type.pseudos();
            Types types = pseudos.types;
            ProcessingHelper helper = pseudos.helper;
            TreeAssembler asm = helper.newAssembler();

            TypeMirror find = type.elem.asType();
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
                            if (helper.typeOf(path, var.getType()) == find) {
                                Editors.setType(var, asm.type(replace).asExpr());
                            }
                            break;
                        case TYPE_CAST:
                            TypeCastTree cast = (TypeCastTree) node;
                            if (helper.typeOf(path, cast.getType()) == find) {
                                ExpressionTree expr = cast.getExpression();
                                if (types.isAssignable(helper.typeOf(path, expr), replace)) {
                                    pseudos.suppressDiagnostics(CANNOT_CAST, expr);
                                    Editors.setType(cast, asm.type(replace).asExpr());
                                }
                            }
                            System.out.println(cast);
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
