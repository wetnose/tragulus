package wn.pseudoclasses;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import wn.tragulus.JavacUtils;
import wn.tragulus.ProcessingHelper;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Flags.ABSTRACT;
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
                            if ((m.flags() & ABSTRACT) == 0 || !Processor.isMarkedAsPseudo(m.owner)) {
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
    public void process(ProcessingHelper helper, Map<TypeMirror, Set<CompilationUnitTree>> usages) {

        Set<Name> classNames = usages.keySet().stream().map(helper::getName).collect(Collectors.toSet());
        System.out.println(classNames);
//        usages.keySet().forEach(t -> {
//            CompilationUnitTree unit = helper.getUnit(t);
//            ((JCTree) unit).accept(new TreeTranslator() {
//                @Override
//                public void visitTopLevel(JCTree.JCCompilationUnit cu) {
//                    cu.defs = List.nil();
//                    result = cu;
//                }
//            });
//            System.out.println(unit.getSourceFile());
//            System.out.println(unit);
//        });
//        usages.keySet().forEach(t -> {
//            TypeMirror basic = helper.getSupertype(t);
//            TypeElement element = (TypeElement) helper.asElement(basic);
//            if (element instanceof DelegatedSymbol && (((Symbol) helper.asElement(basic)).flags() & FINAL) != 0) {
//                ((Symbol) element).flags_field &= ~FINAL;
//            }
//        });
    }
}
