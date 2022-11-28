package wn.pseudoclasses;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import wn.pseudoclasses.Pseudos.Extension;
import wn.pseudoclasses.Pseudos.PseudoType;
import wn.tragulus.Editors;
import wn.tragulus.JavacUtils;
import wn.tragulus.ProcessingHelper;
import wn.tragulus.TreeAssembler;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static wn.pseudoclasses.Pseudos.Err.CANNOT_CAST;

/**
 * Alexander A. Solovioff
 * Date: 19.11.2022
 * Time: 2:00 AM
 */
class Inliner {

    private static final int A = 0, B = 1, C = 2, V = 3; // asm vars


    final Pseudos pseudos;
    final Trees   trees;
    final Types   types;

    final TreeAssembler asm;

    final Element wrapperValue;


    public Inliner(Pseudos pseudos) {
        ProcessingHelper helper = pseudos.helper;
        this.pseudos   = pseudos;
        this.trees     = pseudos.trees;
        this.types     = pseudos.types;
        this.asm       = helper.newAssembler(4);

        TypeElement wrapper = helper.asElement(pseudos.wrapperType);
        Name value = asm.toName("value");
        for (Element member : wrapper.getEnclosedElements()) {
            if (member.getKind() == ElementKind.FIELD && member.getSimpleName() == value) {
                wrapperValue = member;
                return;
            }
        }
        throw new AssertionError();
    }


    void inline(Collection<PseudoType> pseudotypes) {
        UnitProcessor processor = new UnitProcessor(pseudotypes);
        pseudotypes.stream()
                .flatMap(t -> t.units.stream())
                .distinct()
                .forEach(processor::process);
    }



    private class UnitProcessor {

        final Map<TypeMirror,Extension> extensions;

        UnitProcessor(Collection<PseudoType> pseudotypes) {
            extensions = pseudotypes.stream()
                    .filter(t -> t instanceof Extension)
                    .collect(Collectors.toMap(t -> t.elem.asType(), t -> (Extension) t));
        }


        void process(CompilationUnitTree unit) {

            ProcessingHelper helper = pseudos.helper;
            Names names = new Names(unit);

            new TreePathScanner<Extract, Container>() {

                @Override
                public Extract visitExpressionStatement(ExpressionStatementTree node, Container container) {
                    Extract extr = scan(node.getExpression(), null);
                    if (extr == null) return null;
                    Statements stmts = extr.stmts;
                    ExpressionTree expr = extr.expr;
                    asm.at(node);
                    if (expr != null) {
                        if (JavacUtils.isStatementExpression(expr)) {
                            stmts.add(asm.set(extr.expr).exec().get());
                        } else if (expr.getKind() != Tree.Kind.IDENTIFIER) {
                            throw new AssertionError();
                        }
                    }
                    if (stmts.size() == 1) {
                        Editors.replaceTree(getCurrentPath(), stmts.get(0));
                    } else {
                        Editors.replaceTree(getCurrentPath(), asm.block(stmts).get());
                    }
                    return null;
                }

                @Override
                public Extract visitMethodInvocation(MethodInvocationTree node, Container container) {
                    TreePath path = getCurrentPath();
                    ExecutableElement elem = (ExecutableElement) trees
                            .getElement(new TreePath(path, node.getMethodSelect()));
                    Extension ext = extensions.get(elem.getEnclosingElement().asType());
                    if (ext != null && !(ext instanceof Pseudos.Wrapper)) {
                        helper.printError("not supported yet", elem);
                        return super.visitMethodInvocation(node, null);
                    }
                    List<? extends Tree> typeArgs = node.getTypeArguments();
                    //todo scan(typeArgs, null);
                    ExpressionTree sel = node.getMethodSelect();
                    Extract selExtr = scan(sel, null);
                    ExpressionTree[] args = node.getArguments().toArray(new ExpressionTree[0]);
                    int argCount = args.length, extrCount = 0;
                    Extract[] argExtr = new Extract[argCount];
                    for (int i=0; i < argCount; i++) {
                        ExpressionTree arg = args[i];
                        Extract extr = scan(arg, null);
                        if (extr != null) {
                            extrCount++;
                            argExtr[i] = extr;
                        }
                    }
                    if (selExtr != null || extrCount != 0 || ext != null) {
                        //TreeAssembler asm = Inliner.this.asm;
                        Statements stmts = new Statements();
                        if (selExtr != null) {
                            stmts.addAll(selExtr.stmts);
                            asm.set(A, selExtr.expr);
                        } else {
                            asm.set(A, sel);
                        }
                        MethodInjector mthd = extrCount == 0 && ext == null ? null : new MethodInjector(elem);
                        for (int i=0; i < argCount; i++) {
                            ExpressionTree arg = args[i];
                            if (extrCount == 0 && ext == null) {
                                args[i] = asm.copyOf(arg);
                                continue;
                            }
                            Extract extr = argExtr[i];
                            ExpressionTree expr;
                            if (extr != null) {
                                extrCount--;
                                stmts.addAll(extr.stmts);
                                expr = extr.expr;
                            } else {
                                expr = null;
                            }
                            if (!(expr instanceof IdentifierTree)) {
                                TypeMirror type = mthd.params[i].asType();
                                asm.at(arg);
                                if (expr == null) expr = asm.copyOf(arg);
                                Name var = stmts.addDecl(type, names, "var", expr);
                                args[i] = asm.identOf(var);
                            } else {
                                args[i] = expr;
                            }
                        }
                        ExpressionTree ret;
                        if (mthd != null) {
                            ret = mthd.inject(((MemberSelectTree) asm.get(A)).getExpression(), args, stmts);
                        } else {
                            ret = asm.at(node).invoke(A, asm.copyOf(typeArgs), Arrays.asList(args)).get(A);
                        }
                        return new Extract(ret, stmts);
                    } else {
                        return super.visitMethodInvocation(node, container);
                    }
                }

                class MethodInjector {

                    final ExecutableElement elem;
                    final VariableElement[] params;

                     MethodInjector(ExecutableElement elem) {
                        this.elem = elem;
                        this.params = elem.getParameters().toArray(new VariableElement[0]);
                    }

                    ExpressionTree inject(ExpressionTree self, ExpressionTree[] args, Statements stmts) {
                        assert args.length == params.length;
                        final HashMap<Name,Name> repl = new HashMap<>();
                        for (int i=0, argCount=args.length; i < argCount; i++) {
                            VariableElement param = params[i];
                            Name name = param.getSimpleName();
                            ExpressionTree arg = args[i];
                            repl.put(name, ((IdentifierTree) arg).getName());
                        }
                        Name label = names.generate(elem.getSimpleName());
                        Name var = elem.getReturnType() == pseudos.voidType ? null : names.generate("var");
                        BlockTree body = ((MethodTree) trees.getPath(elem).getLeaf()).getBody();
                        body = asm.reset().copyOf(body, new Function<>() {
                            @Override
                            public Tree apply(Tree t) {
                                switch (t.getKind()) {
                                    case IDENTIFIER:
                                        Name n = ((IdentifierTree) t).getName();
                                        Name r = repl.get(n);
                                        if (r == null && names.contains(n)) {
                                            r = names.generate(n);
                                        }
                                        if (r != null) return asm.identOf(r);
                                        break;
                                    case MEMBER_SELECT:
                                        if (JavacUtils.asElement(t) == wrapperValue) return asm.copyOf(self);
                                        break;
                                    case RETURN:
                                        ReturnTree ret = (ReturnTree) t;
                                        ExpressionTree expr = asm.copyOf(ret.getExpression(), this);
                                        if (var == null && expr != null) {
                                            Statements s = new Statements(2);
                                            s.addAssign(var, expr);
                                            s.add(asm.brk(label).get());
                                            return asm.block(s).get();
                                        }
                                }
                                return null;
                            }
                        });
                        if (var != null) stmts.addDecl(elem.getReturnType(), var, null);
                        stmts.add(asm.set(body).labeled(label).get());
                        return var == null ? null : asm.identOf(var);
                    }
                }

                @Override
                public Extract visitVariable(VariableTree node, Container stmt) {
                    super.visitVariable(node, stmt);
                    TreePath path = getCurrentPath();
                    Tree type = node.getType();
                    Extension ext = extensions.get(helper.typeOf(path, type));
                    if (ext != null) {
                        Editors.setType(node, asm.at(type).type(ext.wrappedType).asExpr());
                    }
                    return null;
                }

                @Override
                public Extract visitTypeCast(TypeCastTree node, Container stmt) {
                    super.visitTypeCast(node, stmt);
                    TreePath path = getCurrentPath();
                    Tree type = node.getType();
                    Extension ext = extensions.get(helper.typeOf(path, type));
                    if (ext != null) {
                        ExpressionTree expr = node.getExpression();
                        TypeMirror replace = ext.wrappedType;
                        if (types.isAssignable(helper.typeOf(path, expr), replace)) {
                            pseudos.suppressDiagnostics(CANNOT_CAST, expr);
                            Editors.setType(node, asm.at(type).type(replace).asExpr());
                        }
                    }
                    return null;
                }

            }.scan(unit, null);

            System.out.println(unit);
            System.out.println(names);
        }
    }



//    private class Decomposer extends TreePathScanner<Extract, Container> {
//
//        final Names names;
//
//        Decomposer(Names names) {
//            this.names = names;
//        }
//
//        @Override
//        public Extract scan(Tree tree, Container container) {
//            if (tree instanceof StatementTree)
//            return super.scan(tree, container);
//        }
//
//        @Override
//        public Extract visitConditionalExpression(ConditionalExpressionTree node, Container container) {
//            ExpressionTree cond = node.getCondition();
//            ExpressionTree pos  = node.getTrueExpression();
//            ExpressionTree neg  = node.getFalseExpression();
//            Extract conExtr = scan(cond, container);
//            Extract posExtr = scan(node.getTrueExpression(), container);
//            Extract negExtr = scan(node.getFalseExpression(), container);
//            if (conExtr == null && posExtr == null && negExtr == null) return null;
//            String var = names.generate("var");
//            ArrayList<StatementTree> stmts = new ArrayList<>(2);
//            if (conExtr == null) {
//                asm.set(D, cond).cpy(D);
//            } else {
//                stmts.addAll(conExtr.stmts);
//                asm.set(D, conExtr.expr);
//            }
//            if (posExtr == null) {
//                asm.set(B, pos).cpy(B);
//            } else {
//                ArrayList<StatementTree> branch = posExtr.stmts;
//                asm.ident(B, var);
//                asm.assign(B, posExtr.expr);
//                branch.add(asm.get(B));
//                asm.at(pos).block(B, branch);
//            }
//            if (negExtr == null) {
//                asm.set(C, neg).cpy(C);
//            } else {
//                ArrayList<StatementTree> branch = negExtr.stmts;
//                asm.ident(C, var);
//                asm.assign(C, negExtr.expr);
//                branch.add(asm.get(C));
//                asm.at(neg).block(C, branch);
//            }
//            asm.at(node).declareVar(A, JavacUtils.typeOf(node), var);
//            if (posExtr == null && negExtr == null) {
//                asm.at(cond).cond(D, B, C);
//                asm.assign(A, D);
//            } else {
//                asm.at(node).ifThenElse(D, B, C);
//            }
//            stmts.add(asm.get());
//            Extract extract = new Extract(asm.ident(var).get(), stmts);
//            asm.reset();
//            return extract;
//        }
//
//        @Override
//        public Extract visitMemberSelect(MemberSelectTree node, Container container) {
//            Extract upd = scan(node.getExpression(), container);
//            if (upd == null) return null;
//            MemberSelectTree expr = asm.set(upd.expr).select(node.getIdentifier()).get();
//            return new Extract(expr, upd.stmts);
//        }
//
//        @Override
//        public Extract visitMethodInvocation(MethodInvocationTree node, Container container) {
//            requireNonNull(container);
//            Invocation inv = container.invocations;
//            if (inv.expr == node) {
//                container.next();
//                //todo
//            } else {
//                ExpressionTree sel = node.getMethodSelect();
//                Extract selExtr = scan(sel, container);
//            }
//            //if (invocs.isEmpty()) return super.visitMethodInvocation(node, container);
////            if ((inv = invocs.head).expr == node) {
////
////            } else {
////                TypeMirror type = JavacUtils.typeOf(node);
////                stmts.add(asm.at(node).declareVar(type, ""))
////            }
////
////            TreePath path = getCurrentPath();
////            ExecutableElement elem = (ExecutableElement) trees
////                    .getElement(new TreePath(path, node.getMethodSelect()));
////            Extension ext = extensions.get(elem.getEnclosingElement().asType());
////            if (ext != null) {
////                return List.of(new Invocation(container, node, ext));
////            }
//            return super.visitMethodInvocation(node, container);
//        }
//    }


//    static class Invocation extends Ring<Invocation> {
//
//        final Container container; //todo is it actually required?
//        final MethodInvocationTree expr;
//        final Extension ext;
//
//        Invocation(Container container, MethodInvocationTree expr, Extension ext) {
//            assert container != null;
//            this.container = container;
//            this.expr = expr;
//            this.ext = ext;
//        }
//
//        @Override
//        protected void appendTo(StringBuilder buf) {
//            buf.append(expr);
//        }
//    }


    class Statements extends ArrayList<StatementTree> {

        Statements() {
        }

        Statements(int initialCapacity) {
            super(initialCapacity);
        }

        Name addDecl(TypeMirror type, Names names, String prefix, ExpressionTree init) {
            Name var = names.generate(prefix);
            return addDecl(type, var, init);
        }

        Name addDecl(TypeMirror type, Name var, ExpressionTree init) {
            asm.declareVar(V, type, var);
            add((init == null ? asm : asm.assign(V, init)).get(V));
            return var;
        }

        void addAssign(Name var, ExpressionTree expr) {
            add(asm.ident(V, var).assign(V, expr).get(V));
        }
    }


    static class Extract extends Ring<Extract> {

        final ExpressionTree expr;
        final Statements stmts;

        Extract(ExpressionTree expr, Statements stmts) {
            this.expr = expr;
            this.stmts = stmts;
        }

        @Override
        protected void appendTo(StringBuilder buf) {
            buf.append(stmts);
        }
    }


    class Names {

        final Set<String> names = new HashSet<>();


        public Names(Tree root) {
            JavacUtils.scan(root, t -> {
                if (t instanceof IdentifierTree) {
                    add(((IdentifierTree) t).getName().toString());
                }
            });
        }


        void add(String name) {
            names.add(name);
        }

        boolean contains(Object name) {
            return names.contains(name.toString());
        }

        int vi = 0;

        Name generate(Name prefix) {
            return generate(prefix.toString());
        }

        Name generate(String prefix) {
            String varName;
            while (!names.add(varName = prefix + vi++));
            return asm.toName(varName);
        }

        @Override
        public String toString() {
            return names.toString();
        }
    }


    static class Container {

//        final TreePath path;
//
//        Invocation invocations;
//
//        Container(TreePath path) {
//            this.path = path;
//        }
//
//        void add(Invocation inv) {
//            Invocation ring = invocations;
//            if (ring == null) {
//                invocations = inv;
//            } else {
//                ring.add(inv);
//            }
//        }
//
//        void next() {
//            invocations = invocations.next();
//        }
    }
}
