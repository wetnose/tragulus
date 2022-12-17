package wn.pseudoclasses;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree;
import wn.tragulus.Editors;
import wn.tragulus.JavacUtils;
import wn.tragulus.ProcessingHelper;
import wn.tragulus.TreeAssembler;

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
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static com.sun.tools.javac.code.Flags.FINAL;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.util.Elements.Origin.MANDATED;
import static wn.tragulus.JavacUtils.isPublic;
import static wn.tragulus.JavacUtils.isStatic;


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
    final TreeAssembler    asm;

    final TypeMirror voidType;
    final TypeMirror booleanType;
    final TypeMirror objectType;
    final TypeMirror wrapperType;
    final TypeMirror pseudoType;
    final TypeMirror overrideType;

    final Element wrapperValue;
    final Name    superName;
    final Name    thisName;


    public Pseudos(ProcessingHelper helper) {
        this.helper   = helper;
        this.trees    = helper.getTreeUtils();
        this.types    = helper.getTypeUtils();
        this.elements = helper.getElementUtils();
        this.asm      = helper.newAssembler(5);

        voidType      = helper.asType(void.class);
        booleanType   = helper.asType(boolean.class);
        objectType    = helper.asType(Object.class);
        wrapperType   = helper.asType(wn.pseudoclasses.Wrapper.class);
        pseudoType    = helper.asType(wn.pseudoclasses.Pseudo.class);
        overrideType  = helper.asType(Override.class);

        superName = helper.getName("super");
        thisName  = helper.getName("this");

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


    PseudoType pseudoTypeOf(TreePath path) {
        return pseudotypes.computeIfAbsent(helper.asType(path), t -> {
            PseudoType type = detectPseudotype(path);
            if (type != null) {
                if (type instanceof Extension) ((Extension) type).decompose();
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


    Collection<CompilationUnitTree> collectUsages(Set<? extends Element> roots) {
        roots.stream().map(trees::getPath).forEach(new Consumer<>() {
            @Override
            public void accept(TreePath path) {
                Tree node = path.getLeaf();
                switch (node.getKind()) {
                    case CLASS:
                    case INTERFACE:
                        pseudoTypeOf(path);
                        ((ClassTree) node).getMembers().forEach(member -> accept(new TreePath(path, member)));
                }
            }
        });

        pseudotypes.values().forEach(this::validatePseudotype);
        if (!helper.noErrorReports()) return null;

        Set<CompilationUnitTree> units = new HashSet<>();
        roots.stream().map(trees::getPath).map(TreePath::getCompilationUnit).distinct().forEach(unit -> {
            unit.getImports().forEach(imp -> {
                Tree id = imp.getQualifiedIdentifier();
                TypeMirror ref = JavacUtils.typeOf(imp.isStatic() ? ((MemberSelectTree) id).getExpression() : id);
                PseudoType pt = pseudotypes.get(ref);
                if (pt != null) units.add(unit);
            });

            ExpressionTree unitPkg = unit.getPackageName();
            pseudotypes.values().forEach(type -> {
                ExpressionTree typePkg = type.path.getCompilationUnit().getPackageName();
                if (Objects.equals(typePkg, unitPkg)) {
                    units.add(unit);
                }
            });
        });

        units.forEach(unit -> {
            JavacUtils.walkOver(unit, walker -> {
                TreePath path = walker.path();
                Tree t = path.getLeaf();
                if (t.getKind() == Tree.Kind.CLASS && getExtension(JavacUtils.typeOf(t)) == null) {
                    //System.out.println("CLASS " + ((ClassTree) t).getSimpleName());
                    validateUsages(path);
                }
            });
        });

        boolean valid = helper.getDiagnosticQ().stream().noneMatch(diag ->
                diag.getKind() == Diagnostic.Kind.ERROR &&
                !diag.getCode().equals(Err.CONST_EXPR_REQUIRED.code)); // keep for switch/case

        if (!valid) return null;

        return units;
    }


    private void validatePseudotype(PseudoType type) {
        TreePath root = type.path;
        {
            TreePath par;
            while ((par = root.getParentPath()).getLeaf() instanceof ClassTree)
                root = par;
            if (par != root) {
                maskErroneousCasts(root);
            }
        }
        boolean pub = isPublic(type.elem);
        Scope scope = trees.getScope(type.path);
        TypeMirror tm = type.elem.asType();
//        if (JavacUtils.hasOuterInstance(type.elem)) {
//            helper.printError("unsupported nested pseudo class", type.path);
//        }

        new TreePathScanner<Void,PseudoType>() {
            @Override
            public Void visitClass(ClassTree node, PseudoType pt) {
                TypeMirror type = helper.asType(getCurrentPath());
                return super.visitClass(node, pseudotypes.get(type));
            }
            @Override
            public Void visitAnnotation(AnnotationTree node, PseudoType pt) {
                if (pt == null) return super.visitAnnotation(node, pt);
                if (pt == type) {
                    TypeMirror anno = JavacUtils.typeOf(node);
                    if (anno != overrideType && anno != pseudoType) {
                        helper.printError("unexpected annotation type", getCurrentPath());
                    }
                }
                return null;
            }
            @Override
            public Void visitIdentifier(IdentifierTree node, PseudoType pt) {
                TreePath path = getCurrentPath();
                helper.attribute(path);
                if (pt != type) return null;
                Element element = trees.getElement(path);
                if (element == null) return null;
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
                        if (!member) return null;
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
                return null;
            }
        }.scan(root, null);
        //System.out.println(type.path.getParentPath().getLeaf());
    }


    private void validateUsages(TreePath path) {
        maskErroneousCasts(path);
        new TreePathScanner<Void,Void>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                TreePath path = getCurrentPath();
                TreePath select = new TreePath(path, node.getMethodSelect());
                Element mth = trees.getElement(select);
                if (mth != null) {
                    for (ExpressionTree arg : node.getArguments()) {
                        helper.attributeExpr(new TreePath(path, arg));
                    }
                    return null;
                } else {
                    helper.printWarning("not found", select);
                    return super.visitMethodInvocation(node, unused);
                }
            }
        }.scan(path, null);
        JavaFileObject src = path.getCompilationUnit().getSourceFile();
        helper.filterDiagnostics(diag -> {
            if (diag.getSource() != src) return false;
            return diag.getCode().equals(Err.CANNOT_CAST.code);
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
                                    status = ST_INVALID;
                                }
                                constructors.add(new Method(this, path));
                            } else {
                                if (!helper.getOverriddenMethods(elem).isEmpty()) {
                                    helper.printError("method overriding not supported", path);
                                    status = ST_INVALID;
                                }
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
                //todo
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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Fraud


    void maskErroneousCasts(TreePath path) {
        new TreePathScanner<Void,Tree>() {
            @Override
            public Void visitClass(ClassTree node, Tree unused) {
                TypeMirror type = JavacUtils.typeOf(node);
                if (pseudotypes.containsKey(type)) return null;
                return super.visitClass(node, null);
            }
            @Override
            public Void visitTypeCast(TypeCastTree node, Tree unused) {
                TypeMirror type = helper.attributeType(new TreePath(getCurrentPath(), node.getType()));
                ExpressionTree expr = node.getExpression();
                Extension ext = getExtension(type);
                if (ext != null && !isMasked(new TreePath(getCurrentPath(), expr)))
                    Editors.replaceTree(node, expr, expr = asm.at(expr).set(expr).cast(objectType).get());
                scan(expr, null);
                return null;
            }
        }.scan(path, null);
    }

    private boolean isMasked(TreePath path) {
        Tree node = path.getLeaf();
        if (!(node instanceof TypeCastTree)) return false;
        TypeMirror type = helper.attributeType(new TreePath(path, ((TypeCastTree) node).getType()));
        return type == objectType;
    }

    ExpressionTree unmaskErroneousCasts(TreePath path) {
        ExpressionTree node = (ExpressionTree) path.getLeaf();
        if (!isMasked(path)) return node;
        Editors.replaceTree(path, node = ((TypeCastTree) node).getExpression());
        return node;
    }
}
