package wn.pseudoclasses;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import wn.tragulus.Editors;
import wn.tragulus.JavacUtils;
import wn.tragulus.TreeAssembler;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Flags.ABSTRACT;
import static wn.pseudoclasses.ProcessingHelper.isMarkedAsPseudo;
import static wn.tragulus.JavacUtils.walkOver;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 4:59 PM
 */
public class DefaultPlugin implements Plugin {


    static boolean isPublic(Element element) {
        if (!element.getModifiers().contains(Modifier.PUBLIC)) return false;
        Element enclosing = element.getEnclosingElement();
        if (enclosing == null || enclosing.getKind() == ElementKind.PACKAGE) return true;
        return isPublic(enclosing);
    }


    @Override
    public boolean validate(ProcessingHelper helper, TypeElement type) {
        int fieldCount = (int) type.getEnclosedElements().stream()
                .filter(member -> member.getKind() == ElementKind.FIELD)
                .peek(filed -> helper.printError("filed declaration in a regular pseudoclass", filed))
                .count();

        boolean pub = isPublic(type);
        Trees trees = helper.getTreeUtils();
        TreePath typePath = trees.getPath(type);
        class Walker implements Consumer<JavacUtils.TreeWalker> {
            boolean valid = true;
            @Override
            public void accept(JavacUtils.TreeWalker walker) {
                Tree node = walker.node();
                //System.out.println(node + ": " + node.getKind());
                switch (node.getKind()) {
                    case IDENTIFIER: {
                        TreePath path = walker.path();
                        helper.attribute(path);
                        Symbol element = (Symbol) trees.getElement(path);
                        if (element == null) break;
                        accessCheck: {
                            if (pub) {
                                if (isPublic(element)) break accessCheck;
                            } else {
                                if (element instanceof TypeElement) {
                                    if (trees.isAccessible(trees.getScope(typePath), (TypeElement) element))
                                        break accessCheck;
                                } else {
                                    TypeMirror enclosing = element.getEnclosingElement().asType();
                                    if (!(enclosing instanceof DeclaredType)) break accessCheck;
                                    if (trees.isAccessible(trees.getScope(typePath), element, (DeclaredType) enclosing))
                                        break accessCheck;
                                }
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
                                helper.printError("method overriding not supported", method);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
        Walker walker = new Walker();
        walkOver(trees.getPath(type), walker);
        return fieldCount == 0 && walker.valid;
    }


    @Override
    public void process(ProcessingHelper helper, List<TypeUsages> usages) {

        TreeAssembler asm = helper.newAssembler();

        Set<TypeMirror> types = usages.stream().map(usage -> usage.type).collect(Collectors.toSet());

        usages.forEach(usage -> {
            TypeElement type = helper.asElement(usage.type);
            TypeElement rep = type;
            while (isMarkedAsPseudo(rep) || types.contains(rep.asType())) {
                rep = helper.asElement(rep.getSuperclass());
            }
            TypeElement replace = rep;
            usage.units.forEach(unit -> {
                unit.getTypeDecls().forEach(tree -> {
                    ((ClassTree) tree).getMembers().forEach(member -> {
                        if (member.getKind() != Tree.Kind.VARIABLE) return;
                        VariableElement var = helper.asElement(unit, member);
                        TypeElement varType = helper.asElement(var.asType());
                        if (type == varType) {
                            Editors.setType((VariableTree) member, asm.type(replace.asType()).asExpr());
                        }
                    });
                });
            });
        });
    }


    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
