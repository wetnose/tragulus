package wn.tragulus;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.lang.model.util.Elements;
import java.io.IOException;
import java.io.StringWriter;


class MacroProcessor extends Visitor {

    private final TreeMaker M;
    private final Names N;
    
    private final Name of;
    private final Name nil;
    private final Name str;

    private final Symbol lst;
    private final Symbol tag;
    private final Symbol ttg;
    private final Symbol bkd;
    private final Symbol mod;

    private Name m;
    private Name n;

    public MacroProcessor(Context context) {
        Elements E = JavacElements.instance(context);
        M   = TreeMaker.instance(context);
        N   = Names.instance(context);
        m   = N.fromString("M");
        n   = N.fromString("N");
        of  = N.fromString("of");
        nil = N.fromString("nil");
        str = N.fromString("fromString");
        lst = (Symbol) E.getTypeElement(List.class.getCanonicalName());
        tag = (Symbol) E.getTypeElement(Tag.class.getCanonicalName());
        ttg = (Symbol) E.getTypeElement(TypeTag.class.getCanonicalName());
        bkd = (Symbol) E.getTypeElement(BoundKind.class.getCanonicalName());
        mod = (Symbol) E.getTypeElement(ReferenceMode.class.getCanonicalName());
        M.Binary(
                com.sun.tools.javac.tree.JCTree.Tag.NE,
                M.Parens(M.Assign(M.Ident(N.fromString("$B")), M.Ident(N.fromString("$A")))),
                M.Literal(com.sun.tools.javac.code.TypeTag.BOT, null)
        );
    }


    public void setScope(String M, String N) {
        setScope(
                N == null ? null : this.N.fromString(N),
                M == null ? null : this.N.fromString(M)
                );
    }


    public void setScope(javax.lang.model.element.Name M, javax.lang.model.element.Name N) {
        if (M != null) m = (Name) M;
        if (N != null) n = (Name) N;
    }


    public ExpressionTree process(Tree node) {
        return process(node, null);
    }


    public ExpressionTree process(Tree node, Filter filter) {
        return new Disassembler(filter).process((JCTree) node);
    }


    public interface Filter {
        ExpressionTree filterNode(Tree node);
        ExpressionTree filterName(javax.lang.model.element.Name name);
    }


    private class Disassembler extends Visitor {

        JCExpression result;
        Filter filter;

        Disassembler(Filter filter) {
            this.filter = filter;
        }

        private JCExpression nil() {
            return M.Literal(TypeTag.BOT, null);
        }


        private JCExpression listOf() {
            return M.Select(M.QualIdent(lst), of);
        }


        private JCExpression listNil() {
            return M.Select(M.QualIdent(lst), nil);
        }


        private List<JCExpression> make(JCExpression ... args) {
            return args == null || args.length == 0 ? List.nil() : List.from(args);
        }


        private JCExpression make(String method, JCExpression ... args) {
            return M.Apply(null, M.Select(M.Ident(m), N.fromString(method)), make(args));
        }


        public JCExpression process(Name name) {
            if (name == null) return nil();
            if (filter != null) {
                JCExpression expr = (JCExpression) filter.filterName(name);
                if (expr != null) return expr;
            }
            return M.Apply(null, M.Select(M.Ident(n), str), make(M.Literal(name.toString())));
        }


        public JCExpression process(Tag t) {
            return M.Select(M.QualIdent(tag), N.fromString(t.name()));
        }


        public JCExpression process(TypeTag t) {
            return M.Select(M.QualIdent(ttg), N.fromString(t.name()));
        }


        public JCExpression process(BoundKind k) {
            return M.Select(M.QualIdent(bkd), N.fromString(k.name()));
        }


        public JCExpression process(ReferenceMode m) {
            return M.Select(M.QualIdent(mod), N.fromString(m.name()));
        }


        public JCExpression process(JCTree tree) {
            if (tree == null) {
                return nil();
            } else {
                int bak = M.pos;
                M.at(tree.pos);
                try {
                    if (filter != null) {
                        JCExpression expr = (JCExpression) filter.filterNode(tree);
                        if (expr != null) return expr;
                    }
                    tree.accept(this);
                } finally {
                    M.at(bak);
                }
                JCExpression res = this.result;
                this.result = null;
                return res;
            }
        }

        public JCExpression process(List<? extends JCTree> list) {
            if (list == null) {
                return null;
            } else {
                List<JCExpression> res = List.nil();
                for(List item = list; item.nonEmpty(); item = item.tail) {
                    res = res.prepend(this.process((JCTree) item.head));
                }
                res = res.reverse();
                return M.Apply(null, res.isEmpty() ? listNil() : listOf(), res);
            }
        }


        @Override
        public void visitTopLevel(JCCompilationUnit unit) {
            JCExpression anno = process(unit.packageAnnotations);
            JCExpression pid  = process(unit.pid);
            JCExpression defs = process(unit.defs);
            result = make("TopLevel", anno, pid, defs);
        }


        @Override
        public void visitImport(JCImport imp) {
            result = make("Import",
                    process(imp.qualid),
                    M.Literal(imp.staticImport)
            );
        }


        @Override
        public void visitClassDef(JCClassDecl decl) {
            result = make("ClassDef",
                    process(decl.mods),
                    process(decl.name),
                    process(decl.typarams),
                    process(decl.extending),
                    process(decl.implementing),
                    process(decl.defs)
            );

        }


        @Override
        public void visitMethodDef(JCMethodDecl decl) {
            result = make("M.MethodDef",
                    process(decl.mods),
                    process(decl.name),
                    process(decl.restype),
                    process(decl.typarams),
                    process(decl.recvparam),
                    process(decl.params),
                    process(decl.thrown),
                    process(decl.body),
                    process(decl.defaultValue)
            );
        }


        @Override
        public void visitVarDef(JCVariableDecl var) {
            result = make("VarDef",
                    process(var.mods),
                    process(var.name),
                    process(var.vartype),
                    process(var.init)
            );
        }


        @Override
        public void visitSkip(JCSkip skip) {
            result = make("Skip");
        }


        @Override
        public void visitBlock(JCBlock block) {
            result = make("Block",
                    M.Literal(block.flags),
                    process(block.stats)
            );
        }


        @Override
        public void visitDoLoop(JCDoWhileLoop loop) {
            result = make("DoLoop",
                    process(loop.body),
                    process(loop.cond)
            );
        }


        @Override
        public void visitWhileLoop(JCWhileLoop loop) {
            result = make("WhileLoop",
                    process(loop.cond),
                    process(loop.body)
            );
        }


        @Override
        public void visitForLoop(JCForLoop loop) {
            result = make("ForLoop",
                    process(loop.init),
                    process(loop.cond),
                    process(loop.step),
                    process(loop.body)
            );
        }


        @Override
        public void visitForeachLoop(JCEnhancedForLoop loop) {
            result = make("ForeachLoop",
                    process(loop.var),
                    process(loop.expr),
                    process(loop.body)
            );
        }


        @Override
        public void visitLabelled(JCLabeledStatement stmt) {
            result = make("Labelled",
                    process(stmt.label),
                    process(stmt.body)
            );
        }


        @Override
        public void visitSwitch(JCSwitch swch) {
            result = make("Switch",
                    process(swch.selector),
                    process(swch.cases)
            );
        }


        @Override
        public void visitCase(JCCase cas) {
            result = make("Case",
                    process(cas.pat),
                    process(cas.stats)
            );
        }


        @Override
        public void visitSynchronized(JCSynchronized sync) {
            result = make("Synchronized",
                    process(sync.lock),
                    process(sync.body)
            );
        }


        @Override
        public void visitTry(JCTry tr) {
            result = make("Try",
                    process(tr.resources),
                    process(tr.body),
                    process(tr.catchers),
                    process(tr.finalizer)
            );
        }


        @Override
        public void visitCatch(JCCatch ctch) {
            result = make("Catch",
                    process(ctch.param),
                    process(ctch.body)
            );
        }


        @Override
        public void visitConditional(JCConditional cond) {
            result = make("Conditional",
                    process(cond.cond),
                    process(cond.truepart),
                    process(cond.falsepart)
            );
        }


        @Override
        public void visitIf(JCIf iif) {
            result = make("If",
                    process(iif.cond),
                    process(iif.thenpart),
                    process(iif.elsepart)
            );
        }


        @Override
        public void visitExec(JCExpressionStatement stmt) {
            result = make("Exec",
                    process(stmt.expr)
            );
        }


        @Override
        public void visitBreak(JCBreak brk) {
            result = make("Break",
                    process(brk.label)
            );
        }


        @Override
        public void visitContinue(JCContinue cont) {
            result = make("Continue",
                    process(cont.label)
            );
        }


        @Override
        public void visitReturn(JCReturn ret) {
            result = make("Return",
                    process(ret.expr)
            );
        }


        @Override
        public void visitThrow(JCThrow thrw) {
            result = make("Throw",
                    process(thrw.expr)
            );
        }


        @Override
        public void visitAssert(JCAssert asrt) {
            result = make("Assert",
                    process(asrt.cond),
                    process(asrt.detail)
            );
        }


        @Override
        public void visitApply(JCMethodInvocation inv) {
            result = make("Apply",
                    process(inv.typeargs),
                    process(inv.meth),
                    process(inv.args)
            );
        }


        @Override
        public void visitNewClass(JCNewClass newc) {
            result = make("NewClass",
                    process(newc.encl),
                    process(newc.typeargs),
                    process(newc.clazz),
                    process(newc.args),
                    process(newc.def)
            );
        }


        @Override
        public void visitNewArray(JCNewArray newa) {
            result = make("NewArray",
                    process(newa.elemtype),
                    process(newa.dims),
                    process(newa.elems)
            );
        }


        @Override
        public void visitLambda(JCLambda lmbd) {
            result = make("Lambda",
                    process(lmbd.params),
                    process(lmbd.body)
            );
        }


        @Override
        public void visitParens(JCParens par) {
            result = make("Parens",
                    process(par.expr)
            );
        }


        @Override
        public void visitAssign(JCAssign asgn) {
            result = make("Assign",
                    process(asgn.lhs),
                    process(asgn.rhs)
            );
        }


        @Override
        public void visitAssignop(JCAssignOp asgn) {
            result = make("Assignop",
                    process(asgn.getTag()),
                    process(asgn.lhs),
                    process(asgn.rhs)
            );
        }


        @Override
        public void visitUnary(JCUnary unar) {
            result = make("Unary",
                    process(unar.getTag()),
                    process(unar.arg)
            );
        }


        @Override
        public void visitBinary(JCBinary bin) {
            result = make("Binary",
                    process(bin.getTag()),
                    process(bin.lhs),
                    process(bin.rhs)
            );
        }


        @Override
        public void visitTypeCast(JCTypeCast cast) {
            result = make("TypeCast",
                    process(cast.clazz),
                    process(cast.expr)
            );
        }


        @Override
        public void visitTypeTest(JCInstanceOf test) {
            result = make("TypeTest",
                    process(test.expr),
                    process(test.clazz)
            );
        }


        @Override
        public void visitIndexed(JCArrayAccess arr) {
            result = make("Indexed",
                    process(arr.indexed),
                    process(arr.index)
            );
        }


        @Override
        public void visitSelect(JCFieldAccess acc) {
            result = make("Select",
                    process(acc.selected),
                    process(acc.name)
            );
        }


        @Override
        public void visitReference(JCMemberReference ref) {
            result = make("Reference",
                    process(ref.mode),
                    process(ref.name),
                    process(ref.expr),
                    process(ref.typeargs)
            );
        }


        @Override
        public void visitIdent(JCIdent id) {
            result = make("Ident",
                    process(id.name)
            );

        }


        @Override
        public void visitLiteral(JCLiteral lit) {
            TypeTag tt = lit.typetag;
            result = make("Literal",
                    process(tt),
                    tt == TypeTag.CHAR ? M.Literal(TypeTag.INT, (int) lit.value) : M.Literal(tt, lit.value)
            );
        }


        @Override
        public void visitTypeIdent(JCPrimitiveTypeTree id) {
            result = make("TypeIdent",
                    process(id.typetag)
            );
        }


        @Override
        public void visitTypeArray(JCArrayTypeTree tarr) {
            result = make("TypeArray",
                    process(tarr.elemtype)
            );
        }


        @Override
        public void visitTypeApply(JCTypeApply app) {
            result = make("TypeApply",
                    process(app.clazz),
                    process(app.arguments)
            );
        }


        @Override
        public void visitTypeUnion(JCTypeUnion union) {
            result = make("TypeUnion",
                    process(union.alternatives)
            );
        }


        @Override
        public void visitTypeIntersection(JCTypeIntersection inter) {
            result = make("TypeIntersection",
                    process(inter.bounds)
            );
        }


        @Override
        public void visitTypeParameter(JCTypeParameter param) {
            result = make("TypeParameter",
                    process(param.name),
                    process(param.bounds),
                    process(param.annotations)
            );
        }


        @Override
        public void visitWildcard(JCWildcard wcrd) {
            result = make("Wildcard",
                    process(wcrd.kind),
                    process(wcrd.inner)
            );
        }


        @Override
        public void visitTypeBoundKind(TypeBoundKind tbk) {
            result = make("TypeBoundKind",
                    process(tbk.kind)
            );
        }


        @Override
        public void visitAnnotation(JCAnnotation anno) {
            switch (anno.getTag()) {
                case ANNOTATION:
                    result = make("Annotation",
                            process(anno.annotationType),
                            process(anno.args)
                    );
                    break;
                case TYPE_ANNOTATION:
                    result = make("TypeAnnotation",
                            process(anno.annotationType),
                            process(anno.args)
                    );
                    break;
            }
        }


        @Override
        public void visitModifiers(JCModifiers mods) {
            result = make("Modifiers",
                    M.Literal(mods.flags),
                    process(mods.annotations)
            );
        }


        @Override
        public void visitAnnotatedType(JCAnnotatedType type) {
            result = make("AnnotatedType",
                    process(type.annotations),
                    process(type.underlyingType)
            );
        }


        @Override
        public void visitErroneous(JCErroneous err) {
            result = make("M.Erroneous()");
        }


        @Override
        public void visitLetExpr(LetExpr expr) {
            result = make("LetExpr",
                    process(expr.defs),
                    process(expr.expr)
            );
        }
    }


    public static String toString(Tree tree) {
        StringWriter out = new StringWriter();
        try {
            new Pretty(out, false) {
                @Override
                public void visitSelect(JCFieldAccess select) {
                    String name = select.name.toString();
                    switch (name) {
                        default:
                            if (!name.startsWith("JC")) {
                                super.visitSelect(select);
                                break;
                            }
                        case "TreeAssembler":
                        case "TreeMaker":
                        case "TypeTag":
                        case "Names":
                        case "List":
                            try {
                                this.print(name);
                            } catch (IOException e) {
                                throw new AssertionError(e);
                            }
                            break;
                    }
                }
            }.printExpr((JCTree) tree);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return out.toString();
    }
}
