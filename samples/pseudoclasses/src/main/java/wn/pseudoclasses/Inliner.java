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
import wn.tragulus.TreeAssembler.Copier;

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
import java.util.function.BiFunction;
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

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Unit processing

        void process(CompilationUnitTree unit) {

            ProcessingHelper helper = pseudos.helper;
            Names names = new Names(unit);

            new TreePathScanner<Extract, Void>() {

                @Override
                public Extract reduce(Extract r1, Extract r2) {
                    if (r1 != null || r2 != null) throw new AssertionError();
                    return null;
                }


                @Override
                public Extract visitExpressionStatement(ExpressionStatementTree node, Void unused) {
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
                public Extract visitMethodInvocation(MethodInvocationTree node, Void unused) {
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
                    Extract selExtr = scan(node.getMethodSelect(), null);
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
                    args = node.getArguments().toArray(new ExpressionTree[0]); // could be evaluated
                    if (selExtr != null || extrCount != 0 || ext != null) {
                        Statements stmts = new Statements();
                        if (selExtr != null) {
                            stmts.addAll(selExtr.stmts);
                            asm.set(A, selExtr.expr);
                        } else {
                            asm.set(A, node.getMethodSelect());
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
                            if (!(expr instanceof IdentifierTree) && !(expr instanceof LiteralTree)) {
                                if (arg instanceof LiteralTree) {
                                    args[i] = arg;
                                } else {
                                    TypeMirror type = mthd.params[i].asType();
                                    asm.at(arg);
                                    if (expr == null) expr = asm.copyOf(arg);
                                    Name var = stmts.addDecl(type, names, "var", expr);
                                    args[i] = asm.identOf(var);
                                }
                            } else {
                                args[i] = expr;
                            }
                        }
                        ExpressionTree ret;
                        if (mthd != null) {
                            ret = mthd.inline(node, ((MemberSelectTree) asm.get(A)).getExpression(), args, stmts);
                        } else {
                            ret = asm.at(node).invoke(A, asm.copyOf(typeArgs), Arrays.asList(args)).get(A);
                        }
                        return new Extract(ret, stmts);
                    } else {
                        return super.visitMethodInvocation(node, null);
                    }
                }


                class MethodInjector {

                    final ExecutableElement elem;
                    final VariableElement[] params;

                     MethodInjector(ExecutableElement elem) {
                        this.elem = elem;
                        this.params = elem.getParameters().toArray(new VariableElement[0]);
                    }

                    ExpressionTree inline(Tree pos, ExpressionTree self, ExpressionTree[] args, Statements stmts) {
                        assert args.length == params.length;
                        final HashMap<Name,Tree> repl = new HashMap<>();
                        for (int i=0, argCount=args.length; i < argCount; i++) {
                            VariableElement param = params[i];
                            Name name = param.getSimpleName();
                            ExpressionTree arg = args[i];
                            if (arg instanceof LiteralTree) {
                                repl.put(name, arg);
                            } else {
                                repl.put(name, asm.identOf(((IdentifierTree) arg).getName()));
                            }
                        }
                        Name label = names.generate(elem.getSimpleName());
                        Name var = elem.getReturnType() == pseudos.voidType ? null : names.generate("var");
                        BlockTree body = ((MethodTree) trees.getPath(elem).getLeaf()).getBody();
                        body = asm.reset().copyOf(body, new BiFunction<Tree,Copier,Tree>() {
                            @Override
                            public Tree apply(Tree t, Copier copier) {
                                Tree.Kind kind = t.getKind();
                                switch (t.getKind()) {
                                    case VARIABLE: {
                                        VariableTree v = (VariableTree) t;
                                        Name n = v.getName();
                                        v = copier.copy(v);
                                        ExpressionTree init = v.getInitializer();
                                        if (init instanceof LiteralTree || init instanceof IdentifierTree) {
                                            repl.put(n, init);
                                            return asm.empty().get();
                                        } else {
                                            Name r = names.generate(n);
                                            repl.put(n, asm.identOf(r));
                                            Editors.setName(v, r);
                                            return v;
                                        }
                                    }
                                    case IDENTIFIER: {
                                        return asm.copyOf(repl.get(((IdentifierTree) t).getName()));
                                    }
                                    case MEMBER_SELECT: {
                                        if (JavacUtils.asElement(t) == wrapperValue) return asm.copyOf(self);
                                        return copier.copy(t);
                                    }
                                    case RETURN: {
                                        ReturnTree ret = (ReturnTree) t;
                                        ExpressionTree expr = copier.copy(ret.getExpression());
                                        if (var != null && expr != null) {
                                            Statements s = new Statements(2);
                                            s.addAssign(var, expr);
                                            s.add(asm.brk(label).get());
                                            return asm.block(s).get();
                                        } else {
                                            return asm.brk(label).get();
                                        }
                                    }
                                    case PARENTHESIZED:
                                        ExpressionTree e = copy(((ParenthesizedTree) t).getExpression(), copier);
                                        if (e instanceof LiteralTree || e instanceof IdentifierTree) {
                                            return e;
                                        }
                                        return asm.par(e).get();
                                    default:
                                        if (t instanceof UnaryTree) {
                                            UnaryTree u = (UnaryTree) t;
                                            ExpressionTree a = copy(u.getExpression(), copier);
                                            if (a instanceof LiteralTree) {
                                                Object res = Expressions.eval(kind, ((LiteralTree) a).getValue());
                                                if (res != null) return asm.literal(res).get();
                                            }
                                            return asm.uno(kind, A, a).get(A);
                                        } else
                                        if (t instanceof BinaryTree) {
                                            BinaryTree b = (BinaryTree) t;
                                            ExpressionTree l = copy(b.getLeftOperand(), copier);
                                            ExpressionTree r = copy(b.getRightOperand(), copier);
                                            if (l instanceof LiteralTree && r instanceof LiteralTree) {
                                                Object res = Expressions.eval(
                                                        kind, ((LiteralTree) l).getValue(), ((LiteralTree) r).getValue());
                                                if (res != null) return asm.literal(res).get();
                                            }
                                            return asm.set(A, l).bin(kind, A, r).get(A);
                                        } else {
                                            return copier.copy(t);
                                        }
                                }
                            }
                            <T extends Tree> T copy(T t, Copier copier) {
                                //noinspection unchecked
                                return (T) apply(t, copier);
                            }
                        });
                        JavacUtils.scan(body, t -> Editors.setPos(t, pos));
                        if (var != null) stmts.addDecl(elem.getReturnType(), var, null);
                        stmts.add(asm.set(body).labeled(label).get());
                        return var == null ? null : asm.identOf(var);
                    }
                }


                @Override
                public Extract visitVariable(VariableTree node, Void unused) {
                    super.visitVariable(node, null);
                    TreePath path = getCurrentPath();
                    Tree type = node.getType();
                    Extension ext = extensions.get(helper.typeOf(path, type));
                    if (ext != null) {
                        Editors.setType(node, asm.at(type).type(ext.wrappedType).asExpr());
                    }
                    return null;
                }


                private BinaryTree uno(Tree.Kind kind, ExpressionTree arg) {
                    return asm.uno(kind, V, asm.copyOf(arg)).get(V);
                }

                @Override
                public Extract visitUnary(UnaryTree node, Void unused) {
                    asm.at(node);
                    Tree.Kind kind = node.getKind();
                    Extract extr = scan(node.getExpression(), null);
                    if (extr == null) {
                        ExpressionTree arg = node.getExpression();
                        if (arg instanceof LiteralTree) {
                            Object res = Expressions.eval(kind, ((LiteralTree) arg).getValue());
                            if (res != null) {
                                Editors.replaceTree(getCurrentPath(), asm.literal(res).get());
                            }
                        }
                        return null;
                    } else {
                        return new Extract(uno(kind, extr.expr), extr.stmts);
                    }
                }


                private BinaryTree bin(Tree.Kind kind, ExpressionTree left, ExpressionTree right) {
                    return asm.set(V, left).bin(kind, V, asm.copyOf(right)).get(V);
                }

                @Override
                public Extract visitBinary(BinaryTree node, Void unused) {
                    asm.at(node);
                    Tree.Kind kind = node.getKind();
                    Extract lExtr = scan(node.getLeftOperand(), null);
                    Extract rExtr = scan(node.getRightOperand(), null);
                    if (lExtr == null && rExtr == null) {
                        ExpressionTree left = node.getLeftOperand();
                        ExpressionTree right = node.getRightOperand();
                        if (left instanceof LiteralTree && right instanceof LiteralTree) {
                            Object res = Expressions.eval(
                                    kind, ((LiteralTree) left).getValue(), ((LiteralTree) right).getValue());
                            if (res != null) {
                                Editors.replaceTree(getCurrentPath(), asm.literal(res).get());
                            }
                        }
                        return null;
                    }
                    if (lExtr != null) {
                        ExpressionTree right = node.getRightOperand();
                        if (rExtr == null) return new Extract(bin(kind, lExtr.expr, right), lExtr.stmts);
                        Statements stmts = lExtr.stmts;
                        switch (kind) {
                            case CONDITIONAL_AND:
                            case CONDITIONAL_OR:
                                Name var;
                                if (lExtr.expr instanceof IdentifierTree) {
                                    var = ((IdentifierTree) lExtr.expr).getName();
                                } else {
                                    var = stmts.addDecl(pseudos.booleanType, names, "var", lExtr.expr);
                                }
                                Statements b = rExtr.stmts;
                                b.addAssign(var, rExtr.expr);
                                asm.set(A, asm.identOf(var));
                                if (kind == Tree.Kind.CONDITIONAL_OR) asm.not(A);
                                stmts.add(asm.block(B, b).ifThen(A, B).get(A));
                                return new Extract(var, stmts);
                            default:
                                stmts.addAll(rExtr.stmts);
                                return new Extract(bin(kind, lExtr.expr, rExtr.expr), stmts);
                        }
                    } else {
                        ExpressionTree left = node.getLeftOperand();
                        TypeMirror type = trees.getTypeMirror(getCurrentPath());
                        Statements stmts = new Statements();
                        Name var = stmts.addDecl(type, names, "var", left);
                        switch (kind) {
                            case CONDITIONAL_AND:
                            case CONDITIONAL_OR:
                                Statements b = rExtr.stmts;
                                b.addAssign(var, rExtr.expr);
                                asm.set(A, asm.identOf(var));
                                if (kind == Tree.Kind.CONDITIONAL_OR) asm.not(A);
                                stmts.add(asm.block(B, b).ifThen(A, B).get(A));
                                return new Extract(var, stmts);
                            default:
                                stmts.addAll(rExtr.stmts);
                                return new Extract(bin(kind, asm.identOf(var), rExtr.expr), stmts);
                        }
                    }
                }


                @Override
                public Extract visitParenthesized(ParenthesizedTree node, Void unused) {
                    Extract extr = scan(node.getExpression(), null);
                    if (extr != null) return extr;
                    ExpressionTree expr = node.getExpression();
                    if (expr instanceof LiteralTree || expr instanceof IdentifierTree)
                        Editors.replaceTree(getCurrentPath(), expr);
                    return null;
                }


                @Override
                public Extract visitTypeCast(TypeCastTree node, Void unused) {
                    super.visitTypeCast(node, null);
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

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
            add(asm.ident(V, var).assign(V, expr).asStat(V));
        }
    }


    class Extract extends Ring<Extract> {

        final ExpressionTree expr;
        final Statements stmts;

        Extract(ExpressionTree expr, Statements stmts) {
            this.expr = expr;
            this.stmts = stmts;
        }

        Extract(Name var, Statements stmts) {
            this.expr = asm.identOf(var);
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
}
