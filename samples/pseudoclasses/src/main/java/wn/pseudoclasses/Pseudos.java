package wn.pseudoclasses;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree;
import wn.tragulus.JavacUtils;
import wn.tragulus.ProcessingHelper;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.sun.tools.javac.code.Flags.FINAL;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.util.Elements.Origin.MANDATED;
import static wn.tragulus.JavacUtils.isPublic;
import static wn.tragulus.JavacUtils.isStatic;
import static wn.tragulus.JavacUtils.walkOver;

/**
 * Alexander A. Solovioff
 * Date: 17.11.2022
 * Time: 3:58 AM
 */
class Pseudos {

    enum Err {

        INHERIT_FROM_FINAL   ("compiler.err.cant.inherit.from.final"),
        PRIM_TYPE_ARG        ("compiler.err.type.found.req"),
        OVERRIDES_OBJ_MEMBER ("compiler.err.default.overrides.object.member"),
        CANNOT_CAST          ("compiler.err.prob.found.req"),
        CONST_EXPR_REQUIRED  ("compiler.err.const.expr.req"),

        ;

        final String code;

        Err(String code) {
            this.code = code;
        }
    }


    final Map<TypeMirror,PseudoType> pseudotypes = new HashMap<>();

    final ProcessingHelper helper;
    final Trees            trees;
    final Types            types;
    final Elements         elements;

    final TypeMirror voidType;
    final TypeMirror booleanType;
    final TypeMirror objectType;
    final TypeMirror wrapperType;
    final TypeMirror pseudoType;
    final TypeMirror overrideType;

    final Element wrapperValue;
    final Name    thisName;


    public Pseudos(ProcessingHelper helper) {
        this.helper   = helper;
        this.trees    = helper.getTreeUtils();
        this.types    = helper.getTypeUtils();
        this.elements = helper.getElementUtils();

        voidType      = helper.asType(void.class);
        booleanType   = helper.asType(boolean.class);
        objectType    = helper.asType(Object.class);
        wrapperType   = helper.asType(wn.pseudoclasses.Wrapper.class);
        pseudoType    = helper.asType(wn.pseudoclasses.Pseudo.class);
        overrideType  = helper.asType(Override.class);

        thisName = helper.getName("this");

        TypeElement wrapper = helper.asElement(wrapperType);
        Name value = helper.getName("value");
        for (Element member : wrapper.getEnclosedElements()) {
            if (member.getKind() == ElementKind.FIELD && member.getSimpleName() == value) {
                wrapperValue = member;
                return;
            }
        }
        throw new AssertionError();
    }


    static boolean isMarkedAsPseudo(TypeElement type) {
        return type != null && type.getAnnotation(Pseudo.class) != null;
    }


    TypeElement getBaseType(TypeElement type) {
        return helper.asElement(type.getSuperclass());
    }


    TypeElement getBaseType(TypeMirror type) {
        return getBaseType((TypeElement) helper.asElement(type));
    }


    public TypeElement getSupertype(TreePath typePath) {
        if (typePath.getLeaf().getKind() != Kind.CLASS) return null;
        ClassTree classTree = (ClassTree) typePath.getLeaf();
        Tree extendsClause= classTree.getExtendsClause();
        return extendsClause == null ? null : helper.asElement(TreePath.getPath(typePath, extendsClause));
    }


    void suppressDiagnostics(Err err, TreePath path) {
        suppressDiagnostics(err, path.getCompilationUnit(), path.getLeaf());
    }


    void suppressDiagnostics(Err err, CompilationUnitTree unit, Tree tree) {
        helper.filterDiagnostics(diag ->
                diag.getCode().equals(err.code)
                        && diag.getSource() == unit.getSourceFile()
                        && diag.getPosition() == ((JCTree) tree).pos);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // PSEUDO TYPES                                                                                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    PseudoType pseudoTypeOf(Element element) {
        return element instanceof TypeElement ? pseudoTypeOf(trees.getPath(element)) : null;
    }


    PseudoType pseudoTypeOf(TreePath path) {
        return pseudotypes.computeIfAbsent(helper.asType(path), t -> {
            PseudoType type = detectPseudotype(path);
            if (type != null) {
                if (type instanceof Extension) ((Extension) type).decompose();
                validate(type);
            }
            return type;
        });
    }


    Collection<? extends PseudoType> all() {
        return pseudotypes.values();
    }


    Extension getExtension(TypeMirror type) {
        PseudoType pseudotype = pseudotypes.get(type);
        return pseudotype instanceof Extension ? (Extension) pseudotype : null;
    }


    private void validate(PseudoType type) {
        boolean pub = isPublic(type.elem);
        Scope scope = trees.getScope(type.path);
        TypeMirror tm = type.elem.asType();
//        if (JavacUtils.hasOuterInstance(type.elem)) {
//            helper.printError("unsupported nested pseudo class", type.path);
//        }
        walkOver(type.path, walker -> {
            TreePath path = walker.path();
            Tree node = path.getLeaf();
            //System.out.println(node.getKind() + ": " + node);
            switch (node.getKind()) {
                case ANNOTATION:
                    TypeMirror anno = JavacUtils.typeOf(node);
                    if (anno != overrideType && anno != pseudoType) {
                        helper.printError("unexpected annotation type", path);
                    }
                    break;
                case IDENTIFIER: {
                    helper.attribute(path);
                    Element element = trees.getElement(path);
                    if (element == null) break;
                    accessCheck: {
                        boolean member;
                        switch (element.getKind()) {
                            case ENUM:
                            case CLASS:
                            case INTERFACE:
                            case ANNOTATION_TYPE:
                                member = false;
                                break;
                            case FIELD:
                            case METHOD:
                            case CONSTRUCTOR:
                                member = true;
                                break;
                            default:
                                break accessCheck;
                        }
                        if (member) {
                            if (isStatic(element)) {
                                helper.printError("static members not supported", path);
                                break accessCheck;
                            }
                            if (type instanceof Wrapper && element == wrapperValue) {
                                break accessCheck;
                            }
                        }
                        if (pub) {
                            if (isPublic(element)) break accessCheck;
                            if (!member) break;
                            TypeMirror enclosing = element.getEnclosingElement().asType();
                            if (enclosing == tm) break accessCheck;
                        } else {
                            if (member) {
                                TypeMirror enclosing = element.getEnclosingElement().asType();
                                if (!(enclosing instanceof DeclaredType)) break accessCheck;
                                if (trees.isAccessible(scope, element, (DeclaredType) enclosing))
                                    break accessCheck;
                            } else {
                                if (trees.isAccessible(scope, (TypeElement) element)) break accessCheck;
                            }
                        }
                        helper.printError(
                                "no access to " + element.getKind().toString().toLowerCase() + " " + element, path);
                    }
                    break;
                }
            }
        });
    }


    private PseudoType detectPseudotype(TreePath path) {
        if (path == null) return null;

        Tree leaf = path.getLeaf();
        switch (leaf.getKind()) {
            case CLASS:
            case INTERFACE:
                break;
            default:
                return null;
        }

        ClassSymbol type = helper.asElement(path);
        JavacUtils.scan(leaf, tree -> {
            ClassSymbol t;
            TreePath tp;
            if (tree.getKind().asInterface() != ClassTree.class) return;
            if (isMarkedAsPseudo(t = helper.asElement(tp = TreePath.getPath(path, tree)))) {
                if (t.isLocal()) {
                    helper.printError("local pseudoclasses not supported", tp);
                } else
                //if (JavacUtils.hasOuterInstance(t)) {
                if (!JavacUtils.isStatic(t)) {
                    helper.printError("'static' modifier expected", tp);
                }
            }
        });

        if (!isMarkedAsPseudo(type)) {
            return null;
        }

        if (type.getKind() == ElementKind.INTERFACE) {
            return new Template(path);
        }

        final long flags = type.flags();
        if ((flags & FINAL) == 0) {
            helper.printError("missing final modifier", path);
        }

//        if (type.isInner() && (flags & STATIC) == 0) {
//            printError("missing static modifier", path);
//        }

        ClassTree classTree = (ClassTree) leaf;
        Tree extendsClause = classTree.getExtendsClause();
        if (extendsClause == null) {
            if (isMarkedAsPseudo(type)) helper.printError("missing base type", path);
            return null;
        }

        Tree baseTree = extendsClause;
        boolean wrapper = false;
        if (extendsClause.getKind() == Kind.PARAMETERIZED_TYPE) {
            TypeMirror baseType = helper.asType(TreePath.getPath(path, extendsClause));
            if (baseType != wrapperType) {
                if (!isMarkedAsPseudo(type)) return null;
            } else {
                wrapper = true;
                baseTree = ((ParameterizedTypeTree) extendsClause).getTypeArguments().get(0);
            }
        }

        CompilationUnitTree unit = path.getCompilationUnit();

        if (baseTree.getKind() == Kind.PRIMITIVE_TYPE)
            suppressDiagnostics(Err.PRIM_TYPE_ARG, unit, baseTree);
        TypeMirror baseType = trees.getTypeMirror(TreePath.getPath(path, baseTree));
        if (baseType == null) return null;

        if (!baseType.getKind().isPrimitive()) {
            ClassSymbol baseElem = helper.asElement(baseType);
            if ((baseElem.flags() & FINAL) != 0)
                suppressDiagnostics(Err.INHERIT_FROM_FINAL, unit, extendsClause);
            if (isMarkedAsPseudo(baseElem)) {
                helper.printError("prohibited pseudoclass inheritance", type);
                return null;
            }
        }
        return wrapper ? new Wrapper(path, baseType) : new Subtype(path, baseType);
    }


    abstract class PseudoType {

        final TreePath path;
        final TypeElement elem;

        final Set<CompilationUnitTree> units = new HashSet<>();

        PseudoType(TreePath path) {
            this.path = path;
            this.elem = helper.asElement(path);
        }

        Pseudos pseudos() {
            return Pseudos.this;
        }

        void add(CompilationUnitTree unit) {
            units.add(unit);
        }
    }


    class Template extends PseudoType {

        Template(TreePath path) {
            super(path);
        }

        @Override
        public String toString() {
            return elem + " (template)";
        }
    }


    abstract class Extension extends PseudoType {

        static final int ST_CREATED = 0;
        static final int ST_VALID   = 1;
        static final int ST_INVALID = 2;

        final TypeMirror wrappedType;
        final ArrayList<Method> constructors = new ArrayList<>();
        final ArrayList<Method> methods = new ArrayList<>();
        final Set<ExecutableElement> constant = new HashSet<>();

        int status = ST_CREATED;

        Extension(TreePath path, TypeMirror wrappedType) {
            super(path);
            this.wrappedType = wrappedType;
        }

        boolean decompose() {
            if (status == ST_CREATED) {
                status = ST_VALID;
                boolean prim = wrappedType.getKind().isPrimitive();
                ClassTree classTree = (ClassTree) path.getLeaf();
                for (Tree member : classTree.getMembers()) {
                    TreePath path = TreePath.getPath(this.path, member);
                    switch (member.getKind()) {
                        case METHOD: {
                            ExecutableElement elem = helper.asElement(path);
                            if (elem.getKind() == CONSTRUCTOR) {
                                if (elements.getOrigin(elem) == MANDATED) break;
                                if (prim) {
                                    helper.printError("prohibited constructor declaration", path);
                                } else {
                                    constructors.add(new Method(this, path));
                                }
                            } else {
                                methods.add(new Method(this, path));
                            }
                            if (isConst(elem)) constant.add(elem); //todo inline invocations of other pseudo methods
                            break;
                        }
                        case CLASS: {
                            TypeElement elem = helper.asElement(path);
                            if (isMarkedAsPseudo(elem) && !JavacUtils.hasOuterInstance(elem)) break;
                        }
                        default:
                            helper.printError("unsupported declaration", path);
                            status = ST_INVALID;
                            break;
                    }
                }
//                TypeElement elem = this.elem;
//                for (Element member : elem.getEnclosedElements()) {
//                    System.out.println(trees.getTree(member));
//                    switch (member.getKind()) {
//                        case CONSTRUCTOR:
//                            constructors.add(new Method(trees.getPath(member)));
//                            break;
//                        case METHOD:
//                            methods.add(new Method(trees.getPath(member)));
//                            break;
//                        default:
//                            printError("unsupported declaration", member);
//                            status = ST_INVALID;
//                            break;
//                    }
//                }

            }
            return status == ST_VALID;
        }

        private boolean isConst(ExecutableElement elem) {
            TreePath path = trees.getPath(elem);
            BlockTree body = ((MethodTree) path.getLeaf()).getBody();
            return null == JavacUtils.findFirst(body, t -> {
                Tree var = JavacUtils.getAssignableExpression(t);
                Element el;
                return var != null && (el = trees.getElement(TreePath.getPath(path, var))) != null && isSelf(el);
            });
        }

        boolean isConstant(ExecutableElement elem) {
            return constant.contains(elem);
        }

        abstract boolean isSelf(Element element);

        @Override
        public String toString() {
            return elem + " (wrapper for " + wrappedType + ')';
        }
    }


    class Subtype extends Extension {

        Subtype(TreePath path, TypeMirror wrappedType) {
            super(path, wrappedType);
        }

        @Override
        boolean decompose() {
            if (status == ST_CREATED) {
                super.decompose();
                for (Method m : methods) {
                    Element elem = trees.getElement(m.path);
                    if (!helper.getOverriddenMethods(elem).isEmpty()) {
                        helper.printError("method overriding not supported", m.path);
                        status = ST_INVALID;
                    }
                }
            }
            return status == ST_INVALID;
        }

        @Override
        boolean isSelf(Element element) {
            return element.getKind() == FIELD && element.getEnclosingElement() == elem
                    && element.getSimpleName() == thisName;
        }
    }


    class Wrapper extends Extension {

        Wrapper(TreePath path, TypeMirror wrappedType) {
            super(path, wrappedType);
        }

        @Override
        boolean decompose() {
            if (status == ST_CREATED) {
                super.decompose();
                //todo
            }
            return status == ST_VALID;
        }

        @Override
        boolean isSelf(Element element) {
            return element == wrapperValue;
        }
    }


    class Method {

        final Extension ext;
        final TreePath path;
        final ExecutableElement elem;

        Method(Extension ext, TreePath path) {
            this.ext  = ext;
            this.path = path;
            this.elem = helper.asElement(path);
        }

        @Override
        public String toString() {
            return elem.getEnclosingElement() + "." + elem;
        }
    }
}
