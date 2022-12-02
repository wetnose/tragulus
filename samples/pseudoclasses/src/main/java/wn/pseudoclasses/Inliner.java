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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
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

    private static final int A = 0, B = 1, C = 2, D = 3, V = 4; // asm vars


    final Pseudos pseudos;
    final Trees   trees;
    final Types   types;

    final TreeAssembler asm;


    public Inliner(Pseudos pseudos) {
        ProcessingHelper helper = pseudos.helper;
        this.pseudos   = pseudos;
        this.trees     = pseudos.trees;
        this.types     = pseudos.types;
        this.asm       = helper.newAssembler(V+1);
    }


    void inline(Collection<PseudoType> pseudotypes) {
        UnitProcessor processor = new UnitProcessor(pseudotypes);
        pseudotypes.stream()
                .flatMap(t -> t.units.stream())
                .distinct()
                .forEach(processor::process);
    }



    private class UnitProcessor {

        final ProcessingHelper helper = pseudos.helper;
        final Map<TypeMirror,Extension> extensions;

        UnitProcessor(Collection<PseudoType> pseudotypes) {
            extensions = pseudotypes.stream()
                    .filter(t -> t instanceof Extension)
                    .collect(Collectors.toMap(t -> t.elem.asType(), t -> (Extension) t));
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Unit processing

        void process(CompilationUnitTree unit) {

            Names names = new Names(unit);
            maskErroneousCasts(new TreePath(unit));

            new TreePathScanner<Extract, Void>() {

                @Override
                public Extract reduce(Extract r1, Extract r2) {
                    if (r1 != null || r2 != null) {
                        helper.printError("internal error", getCurrentPath());
                    }
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
//                        } else if (expr.getKind() != Tree.Kind.IDENTIFIER) {
//                            throw new AssertionError();
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
                public Extract visitMemberSelect(MemberSelectTree node, Void unused) {
                    Extract extr = scan(node.getExpression(), null);
                    if (extr == null) return null;
                    return new Extract(extr.stmts, asm.at(node).set(extr.expr).select(node.getIdentifier()).asExpr());
                }


                @Override
                public Extract visitMethodInvocation(MethodInvocationTree node, Void unused) {
                    TreePath path = getCurrentPath();
                    Extract selExtr = scan(node.getMethodSelect(), null);
                    ExpressionTree select = node.getMethodSelect();
                    ExecutableElement elem = (ExecutableElement) trees.getElement(new TreePath(path, select));
                    Extension ext = extensions.get(elem.getEnclosingElement().asType());
                    if (ext != null && !(ext instanceof Pseudos.Wrapper)) {
                        ext = null;
                        helper.printError("not supported yet", elem);
                    }
                    List<? extends Tree> typeArgs = node.getTypeArguments();
                    //todo scan(typeArgs, null);
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
                    if (selExtr == null && extrCount == 0 && ext == null) return null;
                    Name self = null;
                    ExpressionTree tgt = null;
                    MethodInjector mthd;
                    VariableElement[] params;
                    if (ext != null) {
                        mthd = new MethodInjector(ext, elem);
                        params = mthd.params;
                    } else {
                        mthd = null;
                        params = elem.getParameters().toArray(new VariableElement[0]);
                    }
                    Statements stmts = new Statements();
                    if (selExtr != null) {
                        stmts.addAll(selExtr.stmts);
                        if (mthd != null) {
                            select = ((MemberSelectTree) selExtr.expr).getExpression();
                            self = stmts.addDecl(select, ext.wrappedType, names, "self", select);
                            if (!mthd.isConst()) {
                                //todo check the position
                                helper.printError("not assignable", new TreePath(path, select));
                            }
                        } else {
                            asm.set(A, selExtr.expr);
                        }
                    } else
                    if (mthd != null) {
                        if (select instanceof IdentifierTree) {
                            //todo support this correctly
                            self = pseudos.wrapperValue.getSimpleName();
                            if (!mthd.isConst()) tgt = asm.identOf(self);
                        } else {
                            select = ((MemberSelectTree) select).getExpression();
                            if (select instanceof IdentifierTree && JavacUtils.isLocal(JavacUtils.asElement(select))) {
                                self = ((IdentifierTree) select).getName();
                            } else {
                                self = stmts.addDecl(select, ext.wrappedType, names, "self", select);
                                if (!mthd.isConst()) {
                                    select = peelExpr(select);
                                    if (isAssignable(select)) {
                                        tgt = select;
                                    } else {
                                        //todo check the position
                                        helper.printError("not assignable", new TreePath(path, select));
                                    }
                                }
                            }
                        }
                    } else {
                        asm.set(A, select);
                    }
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
                                TypeMirror type = params[i].asType();
                                if (expr == null) expr = asm.copyOf(arg);
                                Name var = stmts.addDecl(arg, type, names, "var", expr);
                                args[i] = asm.identOf(var);
                            }
                        } else {
                            args[i] = expr;
                        }
                    }
                    ExpressionTree ret;
                    if (mthd != null) {
                        assert self != null;
                        ret = mthd.inline(node, asm.identOf(self), args, stmts);
                        if (tgt != null) stmts.addAssign(node, tgt, asm.at(node).identOf(self));
                    } else {
                        ret = asm.at(node).invoke(A, asm.copyOf(typeArgs), Arrays.asList(args)).get(A);
                    }
                    return new Extract(stmts, ret);
                }


                /**
                 * Inject the body of a pseudo method
                 */
                class MethodInjector {

                    final Extension         ext;
                    final ExecutableElement elem;
                    final VariableElement[] params;

                     MethodInjector(Extension ext, ExecutableElement elem) {
                        this.ext = ext;
                        this.elem = elem;
                        this.params = elem.getParameters().toArray(new VariableElement[0]);
                    }

                    boolean isConst() {
                         return ext.isConstant(elem);
                    }

                    ExpressionTree inline(Tree pos, ExpressionTree self, ExpressionTree[] args, Statements stmts) {
                        assert args.length == params.length;
                        final HashMap<Name,Tree> repl = new HashMap<>();
                        final HashSet<Name> trueVars = new HashSet<>();
                        BlockTree body = ((MethodTree) trees.getPath(elem).getLeaf()).getBody();
                        JavacUtils.scan(body, t -> {
                            ExpressionTree var = t instanceof UnaryTree ? null : JavacUtils.getAssignableExpression(t);
                            if (var != null) {
                                if (t instanceof CompoundAssignmentTree) {
                                    ExpressionTree expr = ((CompoundAssignmentTree) t).getExpression();
                                    if (expr instanceof LiteralTree) var = null; // c += 2 -> c + 2
                                }
                            } else
                            if (t instanceof MethodInvocationTree)
                                var = ((MethodInvocationTree) t).getMethodSelect(); // c.inv()
                            if (var instanceof IdentifierTree) {
                                trueVars.add(((IdentifierTree) var).getName());
                            }
                        });
                        for (int i=0, argCount=args.length; i < argCount; i++) {
                            VariableElement param = params[i];
                            Name name = param.getSimpleName();
                            ExpressionTree arg = args[i];
                            if (trueVars.contains(name)) {
                                Name r = names.generate(name);
                                repl.put(name, asm.at(arg).identOf(stmts.addDecl(null, param.asType(), r, arg)));
                            } else
                            if (arg instanceof LiteralTree) {
                                repl.put(name, arg);
                            } else {
                                repl.put(name, asm.identOf(((IdentifierTree) arg).getName()));
                            }
                        }
                        Name label = names.generate(elem.getSimpleName());
                        Name var = elem.getReturnType() == pseudos.voidType ? null : names.generate("var");
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
                                        if ((init instanceof LiteralTree || init instanceof IdentifierTree)
                                                && !trueVars.contains(n)) {
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
                                        if (ext.isSelf(JavacUtils.asElement(t))) return asm.copyOf(self);
                                        return copier.copy(t);
                                    }
                                    case RETURN: {
                                        ReturnTree ret = (ReturnTree) t;
                                        ExpressionTree expr = copy(ret.getExpression(), copier);
                                        if (var != null && expr != null) {
                                            Statements s = new Statements(2);
                                            s.addAssign(null, var, expr);
                                            s.add(asm.brk(label).get());
                                            return asm.block(s).get();
                                        } else {
                                            return asm.brk(label).get();
                                        }
                                    }
                                    case EXPRESSION_STATEMENT: {
                                        ExpressionTree expr = copy(((ExpressionStatementTree) t).getExpression(), copier);
                                        if (JavacUtils.isStatementExpression(expr)
                                                && !(JavacUtils.getAssignableExpression(expr) instanceof LiteralTree)) {
                                            return asm.set(expr).exec().get();
                                        } else {
                                            return asm.empty().get();
                                        }
                                    }
                                    case PARENTHESIZED: {
                                        ExpressionTree e = copy(((ParenthesizedTree) t).getExpression(), copier);
                                        if (e instanceof LiteralTree || e instanceof IdentifierTree) {
                                            return e;
                                        }
                                        return asm.par(e).get();
                                    }
                                    default:
                                        if (t instanceof UnaryTree) {
                                            UnaryTree u = (UnaryTree) t;
                                            Name n = JavacUtils.getAssignableVariable(t);
                                            ExpressionTree a = u.getExpression();
                                            a = copy(a, copier);
                                            if (a instanceof LiteralTree) {
                                                Object res = Expressions.eval(kind, ((LiteralTree) a).getValue());
                                                if (res != null) {
                                                    t = asm.literal(res).get();
                                                    if (n != null) repl.replace(n, t);
                                                    return t;
                                                }
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
                                        } else
                                        if (t instanceof CompoundAssignmentTree) {
                                            CompoundAssignmentTree c = (CompoundAssignmentTree) t;
                                            Name n = JavacUtils.getAssignableVariable(t);
                                            ExpressionTree l = copy(c.getVariable(), copier);
                                            ExpressionTree r = copy(c.getExpression(), copier);
                                            if (l instanceof LiteralTree && r instanceof LiteralTree) {
                                                Object res = Expressions.eval(
                                                        kind, ((LiteralTree) l).getValue(), ((LiteralTree) r).getValue());
                                                if (res != null) {
                                                    t = asm.literal(res).get();
                                                    if (n != null) repl.replace(n, t);
                                                    return t;
                                                }
                                            }
                                            return asm.set(A, l).assign(kind, A, r).get(A);
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
                        if (var != null) stmts.addDecl(pos, elem.getReturnType(), var, null);
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
                        ExpressionTree arg;
                        if (JavacUtils.getAssignableVariable(node) == null
                                && (arg = node.getExpression()) instanceof LiteralTree) {
                            Object res = Expressions.eval(kind, ((LiteralTree) arg).getValue());
                            if (res != null) {
                                Editors.replaceTree(getCurrentPath(), asm.literal(res).get());
                            }
                        }
                        return null;
                    } else {
                        if (JavacUtils.isAssignment(kind)) {
                            helper.printError("not assignable",
                                    new TreePath(getCurrentPath(), node.getExpression()));
                        }
                        return new Extract(extr.stmts, uno(kind, extr.expr));
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
                        if (rExtr == null) return new Extract(lExtr.stmts, bin(kind, lExtr.expr, right));
                        Statements stmts = lExtr.stmts;
                        switch (kind) {
                            case CONDITIONAL_AND:
                            case CONDITIONAL_OR:
                                Name var;
                                if (lExtr.expr instanceof IdentifierTree) {
                                    var = ((IdentifierTree) lExtr.expr).getName();
                                } else {
                                    var = stmts.addDecl(node.getLeftOperand(),
                                            pseudos.booleanType, names, "var", lExtr.expr);
                                }
                                Statements b = rExtr.stmts;
                                b.addAssign(right, var, rExtr.expr);
                                asm.block(B, b);
                                asm.at(node).set(A, asm.identOf(var));
                                if (kind == Tree.Kind.CONDITIONAL_OR) asm.not(A);
                                stmts.add(asm.ifThen(A, B).get(A));
                                return new Extract(stmts, var);
                            default:
                                stmts.addAll(rExtr.stmts);
                                return new Extract(stmts, bin(kind, lExtr.expr, rExtr.expr));
                        }
                    } else {
                        ExpressionTree left = node.getLeftOperand();
                        TypeMirror type = trees.getTypeMirror(getCurrentPath());
                        Statements stmts = new Statements();
                        Name var = stmts.addDecl(left, type, names, "var", left);
                        switch (kind) {
                            case CONDITIONAL_AND:
                            case CONDITIONAL_OR:
                                Statements b = rExtr.stmts;
                                b.addAssign(node.getRightOperand(), var, rExtr.expr);
                                asm.block(B, b);
                                asm.at(node).set(A, asm.identOf(var));
                                if (kind == Tree.Kind.CONDITIONAL_OR) asm.not(A);
                                stmts.add(asm.ifThen(A, B).get(A));
                                return new Extract(stmts, var);
                            default:
                                stmts.addAll(rExtr.stmts);
                                return new Extract(stmts, bin(kind, asm.identOf(var), rExtr.expr));
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
                    TreePath path = getCurrentPath();
                    scan(node.getType(), null);
                    Tree type = node.getType();
                    Extension ext = extensions.get(helper.attributeType(new TreePath(path, type)));
                    if (ext != null) {
                        ExpressionTree expr = node.getExpression();
                        pseudos.suppressDiagnostics(CANNOT_CAST, expr);
                        scan(unmaskErroneousCasts(new TreePath(path, expr)), null);
                        TypeMirror exprType = helper.attributeExpr(new TreePath(path, expr = node.getExpression()));
                        TypeMirror replace = ext.wrappedType;
                        if (exprType == replace) {
                            Editors.replaceTree(path, expr);
                            return scan(expr, null);
                        } else {
                            Editors.setType(node, asm.at(type).type(replace).asExpr());
                        }
                    } else {
                        scan(node.getExpression(), null);
                    }
                    return null;
                }


                @Override
                public Extract visitReturn(ReturnTree node, Void unused) {
                    Extract ext = scan(node.getExpression(), null);
                    if (ext != null) {
                        Statements stmts = ext.stmts;
                        stmts.add(asm.at(node).ret(ext.expr).asStat());
                        Editors.replaceTree(getCurrentPath(), asm.block(stmts).get());
                    }
                    return null;
                }


                @Override
                public Extract visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
                    Extract conExtr = scan(node.getCondition(), null);
                    Extract posExtr = scan(node.getTrueExpression(), null);
                    Extract negExtr = scan(node.getFalseExpression(), null);
                    if (conExtr == null && posExtr == null && negExtr == null) return null;
                    ExpressionTree con = node.getCondition();
                    ExpressionTree pos = node.getTrueExpression();
                    ExpressionTree neg = node.getFalseExpression();
                    Statements stmts = new Statements();
                    Name cv = names.generate("con");
                    asm.at(con).ident(D, cv);
                    if (conExtr == null) {
                        stmts.addDecl(con, pseudos.booleanType, cv, asm.copyOf(con));
                    } else {
                        stmts.addAll(conExtr.stmts);
                        stmts.addDecl(con, pseudos.booleanType, cv, conExtr.expr);
                    }
                    if (posExtr == null && negExtr == null) {
                        ConditionalExpressionTree expr = asm.copyOf(node);
                        Editors.setCondition(expr, asm.asExpr(D));
                        return new Extract(stmts, expr);
                    } else {
                        Name rv = stmts.addDecl(node, helper.attributeExpr(getCurrentPath()), names.generate("var"), null);
                        if (posExtr == null) {
                            asm.set(B, asm.copyOf(pos));
                        } else {
                            Statements branch = posExtr.stmts;
                            branch.addAssign(pos, rv, posExtr.expr);
                            asm.block(B, branch);
                        }
                        if (negExtr == null) {
                            asm.set(C, asm.copyOf(neg));
                        } else {
                            Statements branch = negExtr.stmts;
                            branch.addAssign(neg, rv, negExtr.expr);
                            asm.block(C, branch);
                        }
                        stmts.add(asm.at(node).ifThenElse(D, B, C).asStat(D));
                        return new Extract(stmts, asm.at(node).identOf(rv));
                    }
                }

            }.scan(unit, null);

            System.out.println(unit);
            System.out.println(names);
        }


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Routines


        ExpressionTree peelExpr(ExpressionTree expr) {
            return expr instanceof ParenthesizedTree ? peelExpr(((ParenthesizedTree) expr).getExpression()) : expr;
        }

        boolean isAssignable(ExpressionTree expr) {
            expr = peelExpr(expr);
            switch (peelExpr(expr).getKind()) {
                case IDENTIFIER:
                case MEMBER_SELECT:
                case ARRAY_ACCESS:
                    return true;
                default:
                    return false;
            }
        }


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Fraud


        void maskErroneousCasts(TreePath path) {
            new TreePathScanner<Void,Void>() {
                @Override
                public Void visitTypeCast(TypeCastTree node, Void unused) {
                    TypeMirror type = helper.attributeType(new TreePath(getCurrentPath(), node.getType()));
                    ExpressionTree expr = node.getExpression();
                    Extension ext = extensions.get(type);
                    if (ext != null)
                        Editors.replaceTree(node, expr, expr = asm.at(expr).set(expr).cast(pseudos.objectType).get());
                    scan(expr, null);
                    return null;
                }
            }.scan(path, null);
        }

        ExpressionTree unmaskErroneousCasts(TreePath path) {
            ExpressionTree node = (ExpressionTree) path.getLeaf();
            if (!(node instanceof TypeCastTree)) return node;
            TypeMirror type = helper.attributeType(new TreePath(path, ((TypeCastTree) node).getType()));
            if (type == pseudos.objectType) Editors.replaceTree(path, node = ((TypeCastTree) node).getExpression());
            return node;
        }
    }



    class Statements extends ArrayList<StatementTree> {

        Statements() {
        }

        Statements(int initialCapacity) {
            super(initialCapacity);
        }

        Name addDecl(Tree pos, TypeMirror type, Names names, String prefix, ExpressionTree init) {
            Name var = names.generate(prefix);
            return addDecl(pos, type, var, init);
        }

        Name addDecl(Tree pos, TypeMirror type, Name var, ExpressionTree init) {
            if (pos != null) asm.at(pos);
            asm.declareVar(V, type, var);
            add((init == null ? asm : asm.assign(V, init)).get(V));
            return var;
        }

        void addAssign(Tree pos, Name var, ExpressionTree expr) {
            if (pos != null) asm.at(pos);
            add(asm.ident(V, var).assign(V, expr).asStat(V));
        }

        void addAssign(Tree pos, ExpressionTree lhs, ExpressionTree rhs) {
            if (pos != null) asm.at(pos);
            add(asm.set(V, lhs).assign(V, rhs).asStat(V));
        }
    }


    class Extract {

        final Statements     stmts;
        final ExpressionTree expr;

        Extract(Statements stmts, ExpressionTree expr) {
            this.expr = expr;
            this.stmts = stmts;
        }

        Extract(Statements stmts, Name var) {
            this(stmts, asm.identOf(var));
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
