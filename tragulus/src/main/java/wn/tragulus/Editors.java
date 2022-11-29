package wn.tragulus;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;

import javax.lang.model.element.Name;
import java.util.Collection;
import java.util.function.Predicate;

import static wn.tragulus.JCUtils.append;
import static wn.tragulus.JCUtils.insert;
import static wn.tragulus.JCUtils.toJCList;


/**
 * Created by Alexander A. Solovioff
 * 05.08.2018
 */
public class Editors {

    public static void setPos(Tree node, int pos) {
        ((JCTree) node).pos = pos;
    }

    public static void setLabel(BreakTree node, Name label) {
        ((JCBreak) node).label = (com.sun.tools.javac.util.Name) label;
    }

    public static void setLabel(ContinueTree node, Name label) {
        ((JCContinue) node).label = (com.sun.tools.javac.util.Name) label;
    }

    public static void setLabel(LabeledStatementTree node, Name label) {
        ((JCLabeledStatement) node).label = (com.sun.tools.javac.util.Name) label;
    }

    public static void setName(IdentifierTree node, Name name) {
        ((JCIdent) node).name = (com.sun.tools.javac.util.Name) name;
    }

    public static void setName(MemberReferenceTree node, Name name) {
        ((JCMemberReference) node).name = (com.sun.tools.javac.util.Name) name;
    }

    public static void setName(MethodTree node, Name name) {
        ((JCMethodDecl) node).name = (com.sun.tools.javac.util.Name) name;
    }

    public static void setName(TypeParameterTree node, Name name) {
        ((JCTypeParameter) node).name = (com.sun.tools.javac.util.Name) name;
    }

    public static void setName(VariableTree node, Name name) {
        ((JCVariableDecl) node).name = (com.sun.tools.javac.util.Name) name;
    }

    public static void setSimpleName(ClassTree node, Name name) {
        ((JCClassDecl) node).name = (com.sun.tools.javac.util.Name) name;
    }

    public static void setIdentifier(MemberSelectTree node, Name identifier) {
        ((JCFieldAccess) node).name = (com.sun.tools.javac.util.Name) identifier;
    }


    public static void setExpression(ArrayAccessTree node, ExpressionTree indexed) {
        ((JCArrayAccess) node).indexed = (JCExpression) indexed;
    }

    public static void setIndex(ArrayAccessTree node, ExpressionTree index) {
        ((JCArrayAccess) node).index = (JCExpression) index;
    }

    public static void setElementType(ArrayTypeTree node, ExpressionTree type) {
        ((JCArrayTypeTree) node).elemtype = (JCExpression) type;
    }

    public static void setUnderlyingType(AnnotatedTypeTree node, ExpressionTree underlyingType) {
        ((JCAnnotatedType) node).underlyingType = (JCExpression) underlyingType;
    }

    public static void setCondition(AssertTree node, ExpressionTree condition) {
        ((JCAssert) node).cond = (JCExpression) condition;
    }

    public static void setDetail(AssertTree node, ExpressionTree detail) {
        ((JCAssert) node).detail = (JCExpression) detail;
    }

    public static void setVariable(AssignmentTree node, ExpressionTree variable) {
        ((JCAssign) node).lhs = (JCExpression) variable;
    }

    public static void setExpression(AssignmentTree node, ExpressionTree expression) {
        ((JCAssign) node).lhs = (JCExpression) expression;
    }

    public static void setVariable(CompoundAssignmentTree node, ExpressionTree variable) {
        ((JCAssignOp) node).lhs = (JCExpression) variable;
    }

    public static void setExpression(CompoundAssignmentTree node, ExpressionTree expression) {
        ((JCAssignOp) node).lhs = (JCExpression) expression;
    }

    public static void setLeftOperand(BinaryTree node, ExpressionTree leftOperand) {
        ((JCBinary) node).lhs = (JCExpression) leftOperand;
    }

    public static void setRightOperand(BinaryTree node, ExpressionTree rightOperand) {
        ((JCBinary) node).rhs = (JCExpression) rightOperand;
    }

    public static void setExpression(CaseTree node, ExpressionTree expression) {
        ((JCCase) node).pat = (JCExpression) expression;
    }

    public static void setExtendsClause(ClassTree node, ExpressionTree extendsClause) {
        ((JCClassDecl) node).extending = (JCExpression) extendsClause;
    }

//    public static void setPackageName(CompilationUnitTree node, ExpressionTree packageName) {
//        ((JCCompilationUnit) node).pid = (JCExpression) packageName;
//    }

    public static void setCondition(ConditionalExpressionTree node, ExpressionTree condition) {
        ((JCConditional) node).cond = (JCExpression) condition;
    }

    public static void setTrueExpression(ConditionalExpressionTree node, ExpressionTree trueExpression) {
        ((JCConditional) node).truepart = (JCExpression) trueExpression;
    }

    public static void setFalseExpression(ConditionalExpressionTree node, ExpressionTree falseExpression) {
        ((JCConditional) node).falsepart = (JCExpression) falseExpression;
    }

    public static void setCondition(WhileLoopTree node, ExpressionTree condition) {
        ((JCWhileLoop) node).cond = (JCExpression) condition;
    }

    public static void setCondition(DoWhileLoopTree node, ExpressionTree condition) {
        ((JCDoWhileLoop) node).cond = (JCExpression) condition;
    }

    public static void setCondition(ForLoopTree node, ExpressionTree condition) {
        ((JCForLoop) node).cond = (JCExpression) condition;
    }

    public static void setExpression(EnhancedForLoopTree node, ExpressionTree expression) {
        ((JCEnhancedForLoop) node).expr = (JCExpression) expression;
    }

    public static void setExpression(ExpressionStatementTree node, ExpressionTree expression) {
        ((JCExpressionStatement) node).expr = (JCExpression) expression;
    }

    public static void setExpression(MemberSelectTree node, ExpressionTree expression) {
        ((JCFieldAccess) node).selected = (JCExpression) expression;
    }

    public static void setCondition(IfTree node, ExpressionTree condition) {
        ((JCIf) node).cond = (JCExpression) condition;
    }

    public static void setExpression(InstanceOfTree node, ExpressionTree expression) {
        ((JCInstanceOf) node).expr = (JCExpression) expression;
    }

    public static void setQualifierExpression(MemberSelectTree node, ExpressionTree qualifierExpression) {
        ((JCMemberReference) node).expr = (JCExpression) qualifierExpression;
    }

    public static void setReturnType(MethodTree node, ExpressionTree returnType) {
        ((JCMethodDecl) node).restype = (JCExpression) returnType;
    }

    public static void setDefaultValue(MethodTree node, ExpressionTree defaultValue) {
        ((JCMethodDecl) node).defaultValue = (JCExpression) defaultValue;
    }

    public static void setMethodSelect(MethodInvocationTree node, ExpressionTree methodSelect) {
        ((JCMethodInvocation) node).meth = (JCExpression) methodSelect;
    }

    public static void setType(NewArrayTree node, ExpressionTree type) {
        ((JCNewArray) node).elemtype = (JCExpression) type;
    }

    public static void setType(TypeCastTree node, ExpressionTree type) {
        ((JCTypeCast) node).clazz = (JCTree) type;
    }

    public static void setEnclosingExpression(NewClassTree node, ExpressionTree enclosingExpression) {
        ((JCNewClass) node).encl = (JCExpression) enclosingExpression;
    }

    public static void setIdentifier(NewClassTree node, ExpressionTree identifier) {
        ((JCNewClass) node).clazz = (JCExpression) identifier;
    }

    public static void setExpression(ParenthesizedTree node, ExpressionTree expression) {
        ((JCParens) node).expr = (JCExpression) expression;
    }

    public static void setExpression(ReturnTree node, ExpressionTree expression) {
        ((JCReturn) node).expr = (JCExpression) expression;
    }

    public static void setExpression(SwitchTree node, ExpressionTree expression) {
        ((JCSwitch) node).selector = (JCExpression) expression;
    }

    public static void setExpression(SynchronizedTree node, ExpressionTree expression) {
        ((JCSynchronized) node).lock = (JCExpression) expression;
    }

    public static void setExpression(ThrowTree node, ExpressionTree expression) {
        ((JCThrow) node).expr = (JCExpression) expression;
    }

    public static void setType(ParameterizedTypeTree node, ExpressionTree type) {
        ((JCTypeApply) node).clazz = (JCExpression) type;
    }

    public static void setExpression(TypeCastTree node, ExpressionTree expression) {
        ((JCTypeCast) node).expr = (JCExpression) expression;
    }

    public static void setExpression(UnaryTree node, ExpressionTree expression) {
        ((JCUnary) node).arg = (JCExpression) expression;
    }

    public static void setNameExpression(VariableTree node, ExpressionTree nameExpression) {
        ((JCVariableDecl) node).nameexpr = (JCExpression) nameExpression;
    }

    public static void setType(VariableTree node, ExpressionTree type) {
        ((JCVariableDecl) node).vartype = (JCExpression) type;
    }

    public static void setInitializer(VariableTree node, ExpressionTree initializer) {
        ((JCVariableDecl) node).init = (JCExpression) initializer;
    }


    public static void setStatements(BlockTree node, Collection<? extends StatementTree> statements) {
        ((JCBlock) node).stats = toJCList(statements);
    }

    public static void addStatements(BlockTree node, Collection<? extends StatementTree> statements) {
        JCBlock block = (JCBlock) node;
        block.stats = append(block.stats, toJCList(statements));
    }

    public static void addStatements(BlockTree node, int index, Collection<? extends StatementTree> statements) {
        JCBlock block = (JCBlock) node;
        block.stats = insert(block.stats, index, toJCList(statements));
    }

    public static void setStatements(CaseTree node, Collection<? extends StatementTree> statements) {
        ((JCCase) node).stats = toJCList(statements);
    }

    public static void addStatements(CaseTree node, Collection<? extends StatementTree> statements) {
        JCCase cas = (JCCase) node;
        cas.stats = append(cas.stats, toJCList(statements));
    }

    public static void addStatements(CaseTree node, int index, Collection<? extends StatementTree> statements) {
        JCCase cas = (JCCase) node;
        cas.stats = insert(cas.stats, index, toJCList(statements));
    }


    public static void setArguments(AnnotationTree node, Collection<? extends ExpressionTree> arguments) {
        ((JCAnnotation) node).args = toJCList(arguments);
    }

    public static void addArguments(AnnotationTree node, Collection<? extends ExpressionTree> arguments) {
        JCAnnotation anno = (JCAnnotation) node;
        anno.args = append(anno.args, toJCList(arguments));
    }

    public static void addArguments(AnnotationTree node, int index, Collection<? extends ExpressionTree> arguments) {
        JCAnnotation anno = (JCAnnotation) node;
        anno.args = insert(anno.args, index, toJCList(arguments));
    }


    public static void setArguments(MethodInvocationTree node, Collection<? extends ExpressionTree> arguments) {
        ((JCMethodInvocation) node).args = toJCList(arguments);
    }

    public static void addArguments(MethodInvocationTree node, Collection<? extends ExpressionTree> arguments) {
        JCMethodInvocation inv = (JCMethodInvocation) node;
        inv.args = append(inv.args, toJCList(arguments));
    }

    public static void addArguments(MethodInvocationTree node, int index, Collection<? extends ExpressionTree> arguments) {
        JCMethodInvocation inv = (JCMethodInvocation) node;
        inv.args = insert(inv.args, index, toJCList(arguments));
    }


    public static void setArguments(NewClassTree node, Collection<? extends ExpressionTree> arguments) {
        ((JCNewClass) node).args = toJCList(arguments);
    }

    public static void addArguments(NewClassTree node, Collection<? extends ExpressionTree> arguments) {
        JCNewClass init = (JCNewClass) node;
        init.args = append(init.args, toJCList(arguments));
    }

    public static void addArguments(NewClassTree node, int index, Collection<? extends ExpressionTree> arguments) {
        JCNewClass init = (JCNewClass) node;
        init.args = insert(init.args, index, toJCList(arguments));
    }


    public static void setTypeArguments(MemberReferenceTree node, Collection<? extends ExpressionTree> typeArguments) {
        ((JCMemberReference) node).typeargs = toJCList(typeArguments);
    }

    public static void addTypeArguments(MemberReferenceTree node, Collection<? extends ExpressionTree> typeArguments) {
        JCMemberReference ref = (JCMemberReference) node;
        ref.typeargs = append(ref.typeargs, toJCList(typeArguments));
    }

    public static void addTypeArguments(MemberReferenceTree node, int index, Collection<? extends ExpressionTree> typeArguments) {
        JCMemberReference ref = (JCMemberReference) node;
        ref.typeargs = insert(ref.typeargs, index, toJCList(typeArguments));
    }


    public static void setTypeArguments(MethodInvocationTree node, Collection<? extends ExpressionTree> typeArguments) {
        ((JCMethodInvocation) node).typeargs = toJCList(typeArguments);
    }

    public static void addTypeArguments(MethodInvocationTree node, Collection<? extends ExpressionTree> typeArguments) {
        JCMethodInvocation inv = (JCMethodInvocation) node;
        inv.typeargs = append(inv.typeargs, toJCList(typeArguments));
    }

    public static void addTypeArguments(MethodInvocationTree node, int index, Collection<? extends ExpressionTree> typeArguments) {
        JCMethodInvocation inv = (JCMethodInvocation) node;
        inv.typeargs = insert(inv.typeargs, index, toJCList(typeArguments));
    }


    public static void setTypeArguments(NewClassTree node, Collection<? extends ExpressionTree> typeArguments) {
        ((JCNewClass) node).typeargs = toJCList(typeArguments);
    }

    public static void addTypeArguments(NewClassTree node, Collection<? extends ExpressionTree> typeArguments) {
        JCNewClass init = (JCNewClass) node;
        init.typeargs = append(init.typeargs, toJCList(typeArguments));
    }

    public static void addTypeArguments(NewClassTree node, int index, Collection<? extends ExpressionTree> typeArguments) {
        JCNewClass init = (JCNewClass) node;
        init.typeargs = insert(init.typeargs, index, toJCList(typeArguments));
    }


    public static void setBlock(CatchTree node, BlockTree body) {
        ((JCCatch) node).body = (JCBlock) body;
    }

    public static void setBlock(MethodTree node, BlockTree body) {
        ((JCMethodDecl) node).body = (JCBlock) body;
    }

    public static void setBlock(SynchronizedTree node, BlockTree body) {
        ((JCSynchronized) node).body = (JCBlock) body;
    }

    public static void setBlock(TryTree node, BlockTree body) {
        ((JCTry) node).body = (JCBlock) body;
    }

    public static void setStatement(WhileLoopTree node, StatementTree statement) {
        ((JCWhileLoop) node).body = (JCStatement) statement;
    }

    public static void setStatement(DoWhileLoopTree node, StatementTree statement) {
        ((JCDoWhileLoop) node).body = (JCStatement) statement;
    }

    public static void setStatement(EnhancedForLoopTree node, StatementTree statement) {
        ((JCEnhancedForLoop) node).body = (JCStatement) statement;
    }

    public static void setStatement(ForLoopTree node, StatementTree statement) {
        ((JCForLoop) node).body = (JCStatement) statement;
    }

    public static void setStatement(LabeledStatementTree node, StatementTree statement) {
        ((JCLabeledStatement) node).body = (JCStatement) statement;
    }

    public static void setBody(LambdaExpressionTree node, Tree body) {
        ((JCLambda) node).body = (JCTree) body;
    }


    public static boolean replaceTree(TreePath path, Tree repl) {
        TreePath root;
        if (path == null || (root = path.getParentPath()) == null) return false;
        Tree find = path.getLeaf();
        return replaceTree(root.getLeaf(), find, repl);
    }


    public static boolean replaceTree(Tree root, Tree find, Tree repl) {
        if (root == null || find == null) return false;
        class Replacer extends TreeTranslator {
            boolean replaced;
            @Override
            public <T extends JCTree> T translate(T t) {
                if (replaced) return t;
                if (t == find) {
                    replaced = true;
                    //noinspection unchecked
                    return (T) repl;
                }
                return super.translate(t);
            }
        }
        Replacer replacer = new Replacer();
        replacer.translate((JCTree) root);
        return replacer.replaced;
    }


    public static void filterTree(Tree root, boolean deep, Predicate<Tree> predicate) {
        if (root == null) return;
        ((JCTree) root).accept(new TreeTranslator() {
            @Override
            public <T extends JCTree> T translate(T t) {
                if (t == null || !predicate.test(t)) return null;
                return deep ? super.translate(t) : t;
            }
            @Override
            public <T extends JCTree> List<T> translate(List<T> list) {
                for(List<T> i = list; i.nonEmpty(); i = i.tail) {
                    T node = i.head;
                    if (!predicate.test(node)) {
                        List<T> res = List.nil();
                        for(List<T> j = list; j != i; j = j.tail) {
                            res.prepend(j.head);
                        }
                        for(List<T> j = i.tail; j.nonEmpty(); j = j.tail) {
                            T a = j.head;
                            if (predicate.test(a)) {
                                res = res.prepend(a);
                            }
                        }
                        return res.reverse();
                    } else
                    if (deep) {
                        i.head = super.translate(node);
                    }
                }
                return list;
            }
            @Override
            public List<JCTree.JCVariableDecl> translateVarDefs(List<JCTree.JCVariableDecl> list) {
                return translate(list);
            }
            @Override
            public List<JCTree.JCTypeParameter> translateTypeParams(List<JCTree.JCTypeParameter> list) {
                return translate(list);
            }
            @Override
            public List<JCTree.JCCase> translateCases(List<JCTree.JCCase> list) {
                return translate(list);
            }
            @Override
            public List<JCTree.JCCatch> translateCatchers(List<JCTree.JCCatch> list) {
                return translate(list);
            }
            @Override
            public List<JCTree.JCAnnotation> translateAnnotations(List<JCTree.JCAnnotation> list) {
                return translate(list);
            }
        });
    }
}
