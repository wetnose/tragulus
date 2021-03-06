package wn.tragulus;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import static wn.tragulus.JCUtils.toJCList;


/**
 * Created by Alexander A. Solovioff
 * 05.08.2018
 */
public class TreeAssembler {
    
    public static final int DEFAULT_MEM_SIZE = 10;
    public static final int NA = -1;

    private final com.sun.tools.javac.tree.TreeMaker M;
    private final com.sun.tools.javac.tree.TreeCopier C;
    private final Names N;

    private Object[] mem;

    TreeAssembler(Context context) {
        this(context, DEFAULT_MEM_SIZE);
    }

    TreeAssembler(Context context, int initialMemSize) {
        M = TreeMaker.instance(context);
        N = Names.instance(context);
        C = new TreeCopier(M);
        mem = new Object[initialMemSize];
        reset();
    }
    
    
    public TreeAssembler alloc(int count) {
        if (count < 1) throw new IllegalArgumentException();
        if (mem == null) {
            mem = new Object[count];
        } else 
        if (mem.length < count) {
            mem = Arrays.copyOf(mem, count);
        }
        return this;
    }
    
    
    public TreeAssembler set(Tree val) {
        return set(0, val);
    }


    public TreeAssembler set(int reg, Tree val) {
        mem[reg] = val;
        return this;
    }


    public TreeAssembler set(Collection<? extends Tree> val) {
        return set(0, val);
    }


    public TreeAssembler set(int reg, Collection<? extends Tree> val) {
        mem[reg] = val;
        return this;
    }


    public TreeAssembler set(javax.lang.model.element.Name name) {
        return set(0, name);
    }


    public TreeAssembler set(int reg, javax.lang.model.element.Name name) {
        mem[reg] = name;
        return this;
    }


    public <T> T get() {
        return get(0);
    }


    public <T> T get(int reg) {
        //noinspection unchecked
        return reg < 0 ? null : (T) mem[reg];
    }


    public <T> T get(Class<T> type) {
        return get(0, type);
    }
    
    
    public <T> T get(int reg, Class<T> type) {
        Object val = mem[reg];
        if (val == null) return null;
        if (type == null || type.isInstance(val))
            //noinspection unchecked
            return (T) val;
        throw new IllegalStateException(
                String.format(
                        Locale.US, "Cannot cast mem[%d] of %s to %s",
                        reg, val.getClass().getName(), type.getName()
                ));
    }


    public Name asName() {
        return asName(0);
    }


    public Name asName(int reg) {
        Object v = mem[reg];
        if (v == null) return null;
        if (v instanceof Name) return (Name) v;
        if (v instanceof JCTree) {
            switch (((JCTree) v).getKind()) {
                case IDENTIFIER     : return ((JCIdent) v).name;
                case METHOD         : return ((JCMethodDecl) v).name;
                case TYPE_PARAMETER : return ((JCTypeParameter) v).name;
                case VARIABLE       : return ((JCVariableDecl) v).name;
            }
        }
        throw new IllegalStateException(
                String.format(
                        Locale.US, "Cannot extract Name from mem[%d] of %s",
                        reg, v.getClass().getName()
                ));
    }


    public JCExpression asExpr() {
        return asExpr(0);
    }


    public JCExpression asExpr(int reg) {
        Object v = mem[reg];
        if (v == null) return null;
        if (v instanceof JCExpression) return (JCExpression) v;
        throw new IllegalStateException(
                String.format(
                        Locale.US, "Cannot extract Expression from mem[%d] of %s",
                        reg, v.getClass().getName()
                ));
    }


    public JCStatement asStat() {
        return asStat(0);
    }


    public JCStatement asStat(int reg) {
        Object v = mem[reg];
        if (v == null) return null;
        if (v instanceof JCStatement) return (JCStatement) v;
        if (v instanceof JCExpression) {
            return M.Exec((JCExpression) v);
        }
        throw new IllegalStateException(
                String.format(
                        Locale.US, "Cannot extract Statement from mem[%d] of %s",
                        reg, v.getClass().getName()
                ));
    }


    public TreeAssembler mov(int src, int dst) {
        mem[dst] = mem[src];
        return this;
    }
    
    
    public TreeAssembler clr() {
        return clr(0);
    }
    
    
    public TreeAssembler clr(int reg) {
        mem[reg] = null;
        return this;
    }


    public TreeAssembler cpy() {
        return cpy(0);
    }


    public TreeAssembler cpy(int reg) {
        return cpy(reg, reg);
    }


    public TreeAssembler cpy(int src, int dst) {
        Object v = mem[src];
        if (v == null) {
            mem[dst] = null;
        } else
        if (v instanceof Collection) {
            //noinspection unchecked
            Collection<JCTree> sc = (Collection<JCTree>) v; 
            v = sc.stream().map(C::copy).collect(Collectors.toCollection(() -> new ArrayList<>(sc.size())));
            mem[dst] = v;
        } else
        if (v instanceof JCTree) {
            mem[dst] = C.copy((JCTree) v);
        } else {
            mem[dst] = v;
        }
        return this;
    }


    public TreeAssembler reset() {
        M.at(-1);
        Arrays.fill(mem, null);
        return this;
    }


    public TreeAssembler at(Tree tree) {
        if (tree != null)
            M.pos = ((JCTree) tree).pos;
        return this;
    }


    public TreeAssembler swap(int reg1, int reg2) {
        Object tmp = mem[reg1];
        mem[reg1] = mem[reg2];
        mem[reg2] = tmp;
        return this;
    }


    public TreeAssembler list() {
        return list(0);
    }


    public TreeAssembler list(int reg) {
        Object v = mem[reg];
        if (v != null && !(v instanceof Collection)) {
            ArrayList<Object> list = new ArrayList<>(1);
            list.add(v);
            mem[reg] = list;
        }
        return this;
    }


    public TreeAssembler nil() {
        return nil(0);
    }


    public TreeAssembler nil(int reg) {
        set(reg, M.Literal(TypeTag.BOT, null));
        return this;
    }


    public TreeAssembler literal(Object lit) {
        return literal(0, lit);
    }


    public TreeAssembler literal(int reg, Object lit) {
        set(reg, M.Literal(lit));
        return this;
    }


    public TreeAssembler select(String selector) {
        return select(0, selector);
    }


    public TreeAssembler select(int reg, String selector) {
        JCExpression expr = get(reg, JCExpression.class);
        int i=0, j;
        while ((j = selector.indexOf('.', i)) >= 0) {
            expr = M.Select(expr, N.fromString(selector.substring(i, j)));
            i = j+1;
        }
        set(reg, M.Select(expr, N.fromString(i == 0 ? selector : selector.substring(i))));
        return this;
    }


    public TreeAssembler select(javax.lang.model.element.Name name) {
        return select(0, name);
    }


    public TreeAssembler select(int reg, javax.lang.model.element.Name name) {
        JCExpression expr = get(reg, JCExpression.class);
        set(reg, M.Select(expr, (Name) name));
        return this;
    }


    public TreeAssembler select(MemberSelectTree member) {
        return select(0, member);
    }

    
    public TreeAssembler select(int reg, MemberSelectTree member) {
        JCExpression expr = get(reg, JCExpression.class);
        set(reg, M.Select(expr, (Name) member.getIdentifier()));
        return this;
    }


    public TreeAssembler select(NewClassTree clazz) {
        return select(0, clazz);
    }
    

    public TreeAssembler select(int reg, NewClassTree clazz) {
        JCExpression expr = get(reg, JCExpression.class);
        set(reg, M.Select(expr, (Name) clazz.getIdentifier()));
        return this;
    }


    public TreeAssembler exec() {
        return exec(0);
    }


    public TreeAssembler exec(int reg) {
        JCExpression expr = get(reg, JCExpression.class);
        set(reg, M.Exec(expr));
        return this;
    }


    public TreeAssembler type(TypeMirror type) {
        return type(0, type);
    }


    public TreeAssembler type(int reg, TypeMirror type) {
        set(reg, M.Type((Type) type));
        return this;
    }


    public TreeAssembler declareVar(TypeMirror type, String name) {
        return declareVar(0, type, name);
    }


    public TreeAssembler declareVar(int reg, TypeMirror type, String name) {
        return declareVar(reg, 0, type, name);
    }


    public TreeAssembler declareVar(int reg, long mods, TypeMirror type, String name) {
        return declareVar(reg, mods, type, N.fromString(name));
    }


    public TreeAssembler declareVar(TypeMirror type, javax.lang.model.element.Name name) {
        return declareVar(0, type, name);
    }


    public TreeAssembler declareVar(int reg, TypeMirror type, javax.lang.model.element.Name name) {
        return declareVar(reg, 0, type, name);
    }


    public TreeAssembler declareVar(int reg, long mods, TypeMirror type, javax.lang.model.element.Name name) {
        set(reg, M.VarDef(M.Modifiers(mods), (Name) name, M.Type((Type) type), null));
        return this;
    }


    public TreeAssembler accessByIndex(int indexReg) {
        return accessByIndex(0, indexReg);
    }


    public TreeAssembler accessByIndex(int reg, int indexReg) {
        JCExpression expr = get(reg, JCExpression.class);
        JCExpression index = get(indexReg, JCExpression.class);
        set(reg, M.Indexed(expr, index));
        return this;
    }


    public TreeAssembler accessByIndex(ExpressionTree index) {
        return accessByIndex(0, index);
    }


    public TreeAssembler accessByIndex(int reg, ExpressionTree index) {
        JCExpression expr = get(reg, JCExpression.class);
        set(reg, M.Indexed(expr, (JCExpression) index));
        return this;
    }


    public TreeAssembler invoke(int methReg, int ... argReg) {
        List<JCExpression> args = List.nil();
        if (argReg != null && argReg.length > 0) {
            for (int r : argReg) args = args.prepend(get(r, JCExpression.class));
            args = args.reverse();
        }
        set(methReg, M.Apply(null, get(methReg, JCExpression.class), args));
        return this;
    }


    public TreeAssembler invoke(Iterable<? extends ExpressionTree> args) {
        return invoke(0, args);
    }


    public TreeAssembler invoke(int reg, Iterable<? extends ExpressionTree> args) {
        JCExpression method = get(reg, JCExpression.class);
        set(reg, M.Apply(null, method, toJCList(args)));
        return this;
    }


    public TreeAssembler invoke(Iterable<? extends Tree> typeArgs, Iterable<? extends ExpressionTree> args) {
        return invoke(typeArgs, args);
    }


    public TreeAssembler invoke(int reg, Iterable<? extends Tree> typeArgs, Iterable<? extends ExpressionTree> args) {
        JCExpression method = get(reg, JCExpression.class);
        set(reg, M.Apply(toJCList(typeArgs), method, toJCList(args)));
        return this;
    }


    public TreeAssembler newClass(int reg, int identReg, int argsReg) {
        return newClass(reg, NA, NA, identReg, argsReg, NA);
    }


    public TreeAssembler newClass(int reg, int typeArgsReg, int identReg, int argsReg) {
        return newClass(reg, NA, typeArgsReg, identReg, argsReg, NA);
    }


    public TreeAssembler newClass(int reg, int typeArgsReg, int identReg, int argsReg, int bodyReg) {
        return newClass(reg, NA, typeArgsReg, identReg, argsReg, bodyReg);
    }


    public TreeAssembler newClass(int reg, int enclReg, int typeArgsReg, int identReg, int argsReg, int bodyReg) {
        JCExpression encl = get(enclReg, JCExpression.class);
        //noinspection unchecked
        Collection<JCExpression> typeArgs = get(typeArgsReg, Collection.class);
        JCExpression ident = get(identReg, JCExpression.class);
        //noinspection unchecked
        Collection<JCExpression> args = get(argsReg, Collection.class);
        JCClassDecl body = get(bodyReg, JCClassDecl.class);
        set(reg, M.NewClass(encl, toJCList(typeArgs), ident, toJCList(args), body));
        return this;
    }


    public TreeAssembler ident(String name) {
        return ident(0, name);
    }


    public TreeAssembler ident(int reg, String name) {
        set(reg, M.Ident(N.fromString(name)));
        return this;
    }


    public TreeAssembler ident(javax.lang.model.element.Name name) {
        return ident(0, name);
    }


    public TreeAssembler ident(int reg, javax.lang.model.element.Name name) {
        set(reg, M.Ident((Name) name));
        return this;
    }


    public TreeAssembler ident(IdentifierTree ident) {
        return ident(0, ident);
    }


    public TreeAssembler ident(int reg, IdentifierTree ident) {
        set(reg, M.Ident((Name) ident.getName()));
        return this;
    }


    public TreeAssembler ident(MemberReferenceTree member) {
        return ident(0, member);
    }


    public TreeAssembler ident(int reg, MemberReferenceTree member) {
        set(reg, (M.Ident((Name) member.getName())));
        return this;
    }


    public TreeAssembler ident(MethodTree method) {
        return ident(0, method);
    }


    public TreeAssembler ident(int reg, MethodTree method) {
        set(reg, M.Ident((Name) method.getName()));
        return this;
    }


    public TreeAssembler ident(TypeParameterTree param) {
        return ident(0, param);
    }


    public TreeAssembler ident(int reg, TypeParameterTree param) {
        set(reg, M.Ident((Name) param.getName()));
        return this;
    }


    public TreeAssembler ident(VariableTree var) {
        return ident(0, var);
    }


    public TreeAssembler ident(int reg, VariableTree var) {
        set(reg, M.Ident((Name) var.getName()));
        return this;
    }


    public TreeAssembler ident(Element element) {
        return ident(0, element);
    }


    public TreeAssembler ident(int reg, Element element) {
        set(reg, M.QualIdent((Symbol) element));
        return this;
    }


    public TreeAssembler ident(TypeMirror type) {
        return ident(0, type);
    }


    public TreeAssembler ident(int reg, TypeMirror type) {
        ident(reg, ((DeclaredType) type).asElement());
        return this;
    }


    public TreeAssembler assign(int exprReg) {
        return assign(0, exprReg);
    }


    public TreeAssembler assign(int reg, int exprReg) {
        return assign(reg, get(exprReg, JCExpression.class));
    }


    public TreeAssembler assign(ExpressionTree expr) {
        return assign(0, expr);
    }


    public TreeAssembler assign(int reg, ExpressionTree expr) {
        JCTree target = get(reg, JCTree.class);
        switch (target.getKind()) {
            case VARIABLE:
                JCVariableDecl decl = (JCVariableDecl) target;
                decl.init = (JCExpression) expr;
                set(reg, decl);
                break;
            default:
                set(reg, M.Assign((JCExpression) target, (JCExpression) expr));
                break;
        }
        return this;
    }


    public TreeAssembler cast(Tree type) {
        return cast(0, type);
    }


    public TreeAssembler cast(int reg, Tree type) {
        JCExpression expr = get(reg, JCExpression.class);
        set(reg, M.TypeCast((JCTree) type, expr));
        return this;
    }


    public TreeAssembler cast(TypeMirror type) {
        return cast(0, type);
    }

    
    public TreeAssembler cast(int reg, TypeMirror type) {
        JCExpression expr = get(reg, JCExpression.class);
        set(reg, M.TypeCast((Type) type, expr));
        return this;
    }


    public TreeAssembler ret() {
        return ret(0);
    }


    public TreeAssembler ret(int reg) {
        JCExpression expr = get(reg, JCExpression.class);
        set(reg, M.Return(expr));
        return this;
    }


    public TreeAssembler cond(int condReg, int trueReg, int falseReg) {
        JCExpression cond = get(condReg, JCExpression.class);
        JCExpression truePart = get(trueReg, JCExpression.class);
        JCExpression falsePart = get(falseReg, JCExpression.class);
        set(condReg, M.Conditional(cond, truePart, falsePart));
        return this;
    }


    public TreeAssembler ifThen(int condReg, int thenReg) {
        JCStatement then = get(thenReg, JCStatement.class);
        JCExpression cond = get(condReg, JCExpression.class);
        set(condReg, M.If(cond, then, null));
        return this;
    }


    public TreeAssembler ifThenElse(int condReg, int thenReg, int elseReg) {
        JCExpression cond = get(condReg, JCExpression.class);
        JCStatement thenPart = get(thenReg, JCStatement.class);
        JCStatement elsePart = get(elseReg, JCStatement.class);
        set(condReg, M.If(cond, thenPart, elsePart));
        return this;
    }


    public TreeAssembler lambda(int bodyReg, int ... paramReg) {
        List<JCVariableDecl> params = List.nil();
        if (paramReg != null && paramReg.length > 0) {
            for (int r : paramReg) params = params.prepend(get(r, JCVariableDecl.class));
            params = params.reverse();
        }
        set(bodyReg, M.Lambda(params, get(bodyReg, JCTree.class)));
        return this;
    }


    public TreeAssembler addCase(int switchReg, int patReg, int statReg) {
        JCExpression pat = asExpr(patReg);
        JCStatement stat = asStat(statReg);
        return addCase(switchReg, pat, stat);
    }


    public TreeAssembler addCase(int switchReg, ExpressionTree pat, StatementTree stat) {
        JCSwitch sw = get(switchReg, JCSwitch.class);
        sw.cases = sw.cases.append(M.Case((JCExpression) pat, List.of((JCStatement) stat)));
        return this;
    }


    public TreeAssembler switchStat(ExpressionTree pat) {
        return switchStat(0, pat);
    }


    public TreeAssembler switchStat(int reg, ExpressionTree pat) {
        set(reg, M.Switch((JCExpression) pat, List.nil()));
        return this;
    }


//    public <T extends Tree> Collection<T> collectRange(int startReg, int stopReg) {
//        int sz = stack.size();
//        java.util.List<JCTree> stats = stack.subList(sz-count, sz);
//        //noinspection unchecked
//        java.util.List<T> ret = stats.stream().map(s -> (T) s).collect(Collectors.toList());
//        stats.clear();
//        return ret;
//    }


//    public TreeAssembler block(int count) {
//        int sz = stack.size();
//        java.util.List<JCTree> stats = stack.subList(sz-count, sz);
//        List<JCStatement> block = toJCList(stats);
//        stats.clear();
//        set(reg, M.Block(0, block));
//        return this;
//    }


    public TreeAssembler block() {
        return block(0, 0);
    }


    public TreeAssembler block(int statsReg) {
        return block(0, statsReg);
    }


    public TreeAssembler block(int reg, int statsReg) {
        //noinspection unchecked
        return block(reg, get(statsReg, Iterable.class));
    }


    public TreeAssembler block(Iterable<? extends StatementTree> stats) {
        return block(0, stats);
    }


    public TreeAssembler block(int reg, Iterable<? extends StatementTree> stats) {
        set(reg, M.Block(0, toJCList(stats)));
        return this;
    }


    public TreeAssembler ne(int lhsReg, int rhsReg) {
        return ne(lhsReg, get(rhsReg, JCExpression.class));
    }


    public TreeAssembler ne(int lhsReg, ExpressionTree rhs) {
        JCExpression lhs = get(lhsReg, JCExpression.class);
        set(lhsReg, M.Binary(Tag.NE, lhs, (JCExpression) rhs));
        return this;
    }


    public TreeAssembler and(int lhsReg, int rhsReg) {
        return and(lhsReg, get(rhsReg, JCExpression.class));
    }


    public TreeAssembler and(int lhsReg, ExpressionTree rhs) {
        JCExpression lhs = get(lhsReg, JCExpression.class);
        set(lhsReg, M.Binary(Tag.AND, lhs, (JCExpression) rhs));
        return this;
    }


    public TreeAssembler expr(UnsafeTreeMaker maker) {
        return expr(0, maker);
    }


    public TreeAssembler expr(int reg, UnsafeTreeMaker maker) {
        set(reg, maker.make(M, N, this));
        return this;
    }


    public TreeAssembler stat(UnsafeTreeMaker maker) {
        return stat(0, maker);
    }


    public TreeAssembler stat(int reg, UnsafeTreeMaker maker) {
        set(reg, maker.make(M, N, this));
        return this;
    }


    @FunctionalInterface
    public interface UnsafeTreeMaker {
        JCTree make(TreeMaker m, Names n, TreeAssembler asm);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Operators & macros                                                                                             //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public TreeAssembler expr(String expr) {
        throw new AssertionError("expr(...) not processed");
    }


    public TreeAssembler expr(int reg, String expr) {
        throw new AssertionError("expr(...) not processed");
    }


    public TreeAssembler stat(String stat) {
        throw new AssertionError("stat(...) not processed");
    }


    public TreeAssembler stat(int reg, String stat) {
        throw new AssertionError("stat(...) not processed");
    }
}
