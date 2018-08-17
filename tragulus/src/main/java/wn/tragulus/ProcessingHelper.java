package wn.tragulus;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Scope;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by Alexander A. Solovioff
 * 05.08.2018
 */
public class ProcessingHelper {

    ProcessingEnvironment env;
    Messager log;
    Trees trees;
    Types types;
    Elements elements;


    public ProcessingHelper(ProcessingEnvironment processingEnv) {
        env = processingEnv;
        log = processingEnv.getMessager();
        trees = Trees.instance(processingEnv);
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
    }


    Context context() {
        return ((JavacProcessingEnvironment) env).getContext();
    }


    public Messager getMessager() {
        return log;
    }


    public Trees getTreeUtils() {
        return trees;
    }


    public Types getTypeUtils() {
        return types;
    }


    public Elements getElementUtils() {
        return elements;
    }


    public TreeAssembler newAssembler() {
        return new TreeAssembler(context());
    }


    public TreeAssembler newAssembler(int memorySize) {
        return new TreeAssembler(context(), memorySize);
    }


    public void printError(CharSequence msg, TreePath path) {
        printMessage(Diagnostic.Kind.ERROR, msg, path);
    }


    public void printError(CharSequence msg, Element element) {
        printMessage(Diagnostic.Kind.ERROR, msg, element);
    }


    public void printMandatoryWarning(CharSequence msg, TreePath path) {
        printMessage(Diagnostic.Kind.MANDATORY_WARNING, msg, path);
    }


    public void printMandatoryWarning(CharSequence msg, Element element) {
        printMessage(Diagnostic.Kind.MANDATORY_WARNING, msg, element);
    }


    public void printWarning(CharSequence msg, TreePath path) {
        printMessage(Diagnostic.Kind.WARNING, msg, path);
    }


    public void printWarning(CharSequence msg, Element element) {
        printMessage(Diagnostic.Kind.WARNING, msg, element);
    }


    public void printNote(CharSequence msg, TreePath path) {
        printMessage(Diagnostic.Kind.NOTE, msg, path);
    }


    public void printNote(CharSequence msg, Element element) {
        printMessage(Diagnostic.Kind.NOTE, msg, element);
    }


    public void printMessage(Diagnostic.Kind kind, CharSequence msg, TreePath path) {
        trees.printMessage(kind, msg, path.getLeaf(), path.getCompilationUnit());
    }


    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element) {
        log.printMessage(kind, msg, element);
    }


    public Name getName(CharSequence s) {
        return elements.getName(s);
    }


    public Name getName(TypeMirror type) {
        if (type == null) return null;
        switch (type.getKind()) {
            case VOID     : return getName("void");
            case BOOLEAN  : return getName("boolean");
            case BYTE     : return getName("byte");
            case SHORT    : return getName("short");
            case INT      : return getName("int");
            case LONG     : return getName("long");
            case CHAR     : return getName("char");
            case FLOAT    : return getName("float");
            case DOUBLE   : return getName("double");
            case DECLARED : return ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName();
            default       : return null;
        }
    }


    public TypeElement getTypeElement(Class type) {
        return elements.getTypeElement(type.getCanonicalName());
    }


    public Element asElement(TypeMirror type) {
        return types.asElement(type);
    }


    public TypeMirror asType(Class clazz) {
        if (clazz == null) return null;
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "void"   : return types.getNoType(TypeKind.VOID);
                case "boolean": return types.getPrimitiveType(TypeKind.BOOLEAN);
                case "byte"   : return types.getPrimitiveType(TypeKind.BYTE);
                case "short"  : return types.getPrimitiveType(TypeKind.SHORT);
                case "int"    : return types.getPrimitiveType(TypeKind.INT);
                case "long"   : return types.getPrimitiveType(TypeKind.LONG);
                case "char"   : return types.getPrimitiveType(TypeKind.CHAR);
                case "float"  : return types.getPrimitiveType(TypeKind.FLOAT);
                case "double" : return types.getPrimitiveType(TypeKind.DOUBLE);
            }
        }
        if (clazz.isArray()) {
            TypeMirror compType = asType(clazz.getComponentType());
            return types.getArrayType(compType);
        } else {
            TypeElement element = getTypeElement(clazz);
            return element == null ? null : element.asType();
        }
    }


    public TypeMirror getSupertype(TypeMirror type) {
        return com.sun.tools.javac.code.Types.instance(context()).supertype((Type) type);
//        for (TypeMirror t : types.directSupertypes(type)) {
//            if (((DeclaredType) t).asElement().getKind() == ElementKind.CLASS) return t;
//        }
//        return null;
    }


    public CompilationUnitTree getUnit(TypeElement type) {
        return trees.getPath(type).getCompilationUnit();
    }


    public Collection<? extends Element> getLocalElements(TreePath path) {
        return getLocalElements(trees.getScope(path));
    }


    private static Collection<? extends Element> getLocalElements(Scope scope) {
        if (scope == null) return null;
        ArrayList<Element> locals = new ArrayList<>();
        scope.getLocalElements().forEach(locals::add);
        return locals;
    }


//    public Element resolve1(TreePath path) {
//        if (path == null) return null;
//        Tree node = path.getLeaf();
//        switch (node.getKind()) {
//            case IDENTIFIER: {
//                Name name = ((IdentifierTree) node).getName();
//                Scope scope = trees.getScope(path);
//                for (Element e : scope.getLocalElements()) {
//                    if (e.getSimpleName() == name) return e;
//                }
//                break;
//            }
//            case MEMBER_SELECT: {
//                MemberSelectTree select = (MemberSelectTree) node;
//                return resolve1(new TreePath(path, select.getExpression()));
//            }
//            case METHOD_INVOCATION: {
//                MethodInvocationTree invokation = (MethodInvocationTree) node;
//                ExpressionTree select = invokation.getMethodSelect();
//                Element elem = resolve1(new TreePath(path, select));
//                switch (select.getKind()) {
//                    case IDENTIFIER:
//                        return elem;
//                    case MEMBER_SELECT:
//                        Name name = ((MemberSelectTree) select).getIdentifier();
//
//                }
//                //TypeMirror type = resolve(new TreePath(path, select.getExpression()));
//                //if (type == null) return null;
//            }
//        }
//        return null;
//    }


    public TypeMirror attribute(CompilationUnitTree unit) {
        return attribute(new TreePath(unit));
    }


    public TypeMirror attribute(TreePath path) {
        JCTree tree = (JCTree) path.getLeaf();
        TypeMirror type = tree.type;
        if (type != null) return type;
        Attr attr = Attr.instance(context());
        attr.attrib(getEnv(path));
        return tree.type;
    }


    public TypeMirror attributeType(TreePath path) {
        JCTree tree = (JCTree) path.getLeaf();
        TypeMirror type = tree.type;
        if (type != null) return type;
        Attr attr = Attr.instance(context());
        return attr.attribType(tree, getEnv(path));
    }


    public TypeMirror attributeExpr(TreePath exprPath) {
        ExpressionTree expr = (ExpressionTree) exprPath.getLeaf();
        Attr attr = Attr.instance(context());
        return attr.attribExpr((JCTree) expr, getEnv(exprPath));
    }


    private Env<AttrContext> getEnv(Scope scope) {
        return ((JavacScope) scope).getEnv();
    }


    private Env<AttrContext> getEnv(TreePath path) {
        return ((JavacScope) trees.getScope(path)).getEnv();
    }
}
