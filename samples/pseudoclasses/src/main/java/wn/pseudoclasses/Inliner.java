package wn.pseudoclasses;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import wn.pseudoclasses.Pseudos.Extension;
import wn.pseudoclasses.Pseudos.Method;
import wn.pseudoclasses.Pseudos.PseudoType;
import wn.tragulus.Editors;
import wn.tragulus.ProcessingHelper;
import wn.tragulus.TreeAssembler;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;

import static wn.pseudoclasses.Pseudos.Err.CANNOT_CAST;

/**
 * Alexander A. Solovioff
 * Date: 19.11.2022
 * Time: 2:00 AM
 */
class Inliner {

    void inline(PseudoType type) {

        if (type instanceof Extension) {

            Pseudos pseudos = type.pseudos();
            Types types = pseudos.types;
            ProcessingHelper helper = pseudos.helper;
            TreeAssembler asm = helper.newAssembler();

            TypeMirror find = type.elem.asType();
            TypeMirror replace = ((Extension) type).wrappedType;

            TreePathScanner<Invocations,Void> scanner = new TreePathScanner<>() {

                @Override
                public Invocations visitVariable(VariableTree node, Void unused) {
                    super.visitVariable(node, unused);
                    TreePath path = getCurrentPath();
                    if (helper.typeOf(path, node.getType()) == find) {
                        Editors.setType(node, asm.type(replace).asExpr());
                    }
                    return null;
                }

                @Override
                public Invocations visitTypeCast(TypeCastTree node, Void unused) {
                    super.visitTypeCast(node, unused);
                    TreePath path = getCurrentPath();
                    if (helper.typeOf(path, node.getType()) == find) {
                        ExpressionTree expr = node.getExpression();
                        if (types.isAssignable(helper.typeOf(path, expr), replace)) {
                            pseudos.suppressDiagnostics(CANNOT_CAST, expr);
                            Editors.setType(node, asm.type(replace).asExpr());
                        }
                    }
                    return null;
                }
            };

            type.units.forEach(unit -> {
                scanner.scan(unit, null);
            });
        }
    }


    private static class Invocation {
        final MethodInvocationTree tree;
        final Method method;

        Invocation(MethodInvocationTree tree, Method method) {
            this.tree = tree;
            this.method = method;
        }
    }


    private static class Invocations extends ArrayList<Invocation> {

    }
}
