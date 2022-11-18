package wn.pseudoclasses;

import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import wn.pseudoclasses.ProcessingHelper.PseudoType;
import wn.pseudoclasses.ProcessingHelper.Template;
import wn.tragulus.JavacUtils.TreeWalker;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.function.Consumer;

import static com.sun.tools.javac.code.Flags.ABSTRACT;
import static wn.pseudoclasses.ProcessingHelper.Err.OVERRIDES_OBJ_MEMBER;
import static wn.pseudoclasses.ProcessingHelper.isMarkedAsPseudo;
import static wn.tragulus.JavacUtils.walkOver;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 4:59 PM
 */
class Validator {


    boolean validate(PseudoType type) {
        ProcessingHelper helper = type.helper();
        int fieldCount = (int) type.elem.getEnclosedElements().stream()
                .filter(member -> member.getKind() == ElementKind.FIELD)
                .peek(filed -> helper.printError("filed declaration in a regular pseudoclass", filed))
                .count();

        Trees trees = helper.getTreeUtils();
        Scope scope = trees.getScope(type.path);
        class Walker implements Consumer<TreeWalker> {
            boolean valid = true;
            @Override
            public void accept(TreeWalker walker) {
                Tree node = walker.node();
                //System.out.println(node + ": " + node.getKind());
                switch (node.getKind()) {
                    case IDENTIFIER: {
                        TreePath path = walker.path();
                        helper.attribute(path);
                        Element element = trees.getElement(path);
                        if (element == null) break;
                        accessCheck: {
                            if (element instanceof TypeElement) {
                                if (trees.isAccessible(scope, (TypeElement) element))
                                    break accessCheck;
                            } else {
                                TypeMirror enclosing = element.getEnclosingElement().asType();
                                if (!(enclosing instanceof DeclaredType)) break accessCheck;
                                if (trees.isAccessible(scope, element, (DeclaredType) enclosing))
                                    break accessCheck;
                            }
                            helper.printError(
                                    "no access to " + element.getKind().toString().toLowerCase() + " " + element, path);
                            valid = false;
                        }
                        //System.out.println(node + ", " + id + ", " + element.getEnclosingElement());
                        break;
                    }
                    case METHOD: {
                        TreePath path = walker.path();
                        MethodSymbol method = (MethodSymbol) trees.getElement(path);
                        for (MethodSymbol m : helper.getOverriddenMethods(method)) {
                            if ((m.flags() & ABSTRACT) == 0 || !isMarkedAsPseudo((TypeElement) m.owner)) {
                                if (type instanceof Template) {
                                    if (m.getEnclosingElement().asType() == helper.objectType) {
                                        helper.suppressDiagnostics(OVERRIDES_OBJ_MEMBER, path.getLeaf());
                                    }
                                } else {
                                    helper.printError("method overriding not supported", method);
                                    valid = false;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        Walker walker = new Walker();
        walkOver(type.path, walker);
        return fieldCount == 0 && walker.valid;
    }
}
