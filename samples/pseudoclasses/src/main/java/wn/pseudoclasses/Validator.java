package wn.pseudoclasses;

import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import wn.pseudoclasses.ProcessingHelper.Extension;
import wn.pseudoclasses.ProcessingHelper.PseudoType;
import wn.tragulus.JavacUtils.TreeWalker;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.function.Consumer;

import static com.sun.tools.javac.code.Flags.PUBLIC;
import static wn.tragulus.JavacUtils.walkOver;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 4:59 PM
 */
class Validator {


    boolean validate(PseudoType type) {
        boolean declValid = !(type instanceof Extension) || ((Extension) type).decompose();
        ProcessingHelper helper = type.helper();
        Trees trees = helper.getTreeUtils();
        Scope scope = trees.getScope(type.path);
        PackageTree pkg = (PackageTree) type.path.getCompilationUnit().getPackageName();
        boolean pub = (((ClassSymbol) type.elem).flags() & PUBLIC) != 0;
        class Walker implements Consumer<TreeWalker> {
            boolean valid = true;
            @Override
            public void accept(TreeWalker walker) {
                TreePath path = walker.path();
                Tree node = path.getLeaf();
                //System.out.println(node + ": " + node.getKind());
                switch (node.getKind()) {
                    case IDENTIFIER: {
                        helper.attribute(path);
                        Element element = trees.getElement(path);
                        if (element == null) break;
                        accessCheck: {
                            if (pub) {
                                Element enclosing = element.getEnclosingElement();
                                if (enclosing == type.elem) break accessCheck;
                                if (enclosing.getKind() == ElementKind.METHOD) break accessCheck;
                                if (helper.isSpecial(enclosing)) break accessCheck;
                                //if ( isPublic(element)) break accessCheck;
                            } else {
                                if (element instanceof TypeElement) {
                                    if (trees.isAccessible(scope, (TypeElement) element))
                                        break accessCheck;
                                } else {
                                    TypeMirror enclosing = element.getEnclosingElement().asType();
                                    if (!(enclosing instanceof DeclaredType)) break accessCheck;
                                    if (trees.isAccessible(scope, element, (DeclaredType) enclosing))
                                        break accessCheck;
                                }
                            }
//                            if (element instanceof TypeElement) {
//                                if (trees.isAccessible(scope, (TypeElement) element))
//                                    break accessCheck;
//                            } else {
//                                TypeMirror enclosing = element.getEnclosingElement().asType();
//                                if (!(enclosing instanceof DeclaredType)) break accessCheck;
//                                if (trees.isAccessible(scope, element, (DeclaredType) enclosing))
//                                    break accessCheck;
//                            }
                            helper.printError(
                                    "no access to " + element.getKind().toString().toLowerCase() + " " + element, path);
                            valid = false;
                        }
                        //System.out.println(node + ", " + id + ", " + element.getEnclosingElement());
                        break;
                    }
                    case METHOD: {
                        MethodSymbol method = (MethodSymbol) trees.getElement(path);
                        if (!helper.getOverriddenMethods(method).isEmpty()) {
                            helper.printError("method overriding not supported", method);
                            valid = false;
                        }
                        break;
                    }
                }
            }
        }
        Walker walker = new Walker();
        walkOver(type.path, walker);
        return declValid && walker.valid;
    }
}
