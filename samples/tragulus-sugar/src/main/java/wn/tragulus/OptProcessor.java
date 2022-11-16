package wn.tragulus;

import com.sun.source.tree.*;
import com.sun.source.tree.LambdaExpressionTree.BodyKind;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;
import static wn.tragulus.JavacUtils.walkOver;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class OptProcessor extends BasicProcessor {

    @Override
    public
    boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (roundEnv.getRootElements().isEmpty()) return false;

        TypeMirror operatorsType = helper.asType(JavaSugar.class);
        Name optName = helper.getElementUtils().getName("opt");

        Predicate<ImportTree> optImport = imp -> {
            Tree id = imp.getQualifiedIdentifier();
            if (imp.isStatic()) {
                MemberSelectTree ref = (MemberSelectTree) id;
                return ref.getIdentifier() == optName && JavacUtils.typeOf(ref.getExpression()) == operatorsType;
            } else {
                return JavacUtils.typeOf(id) == operatorsType;
            }
        };

        List<CompilationUnitTree> units = collectCompilationUnits(roundEnv, importFilter(optImport));

        Trees trees = helper.getTreeUtils();
        TreeAssembler asm = helper.newAssembler();

        units.forEach(unit -> {
            boolean hasStaticImport = unit.getImports().stream().anyMatch(imp -> imp.isStatic() && optImport.test(imp));
            Predicate<MethodInvocationTree> invFastTest = inv -> {
                String select = inv.getMethodSelect().toString();
                return select.equals("JavaSugar.opt") || hasStaticImport && select.equals("opt");
            };

            Predicate<TreePath> invSureTest = path -> {
                ExecutableElement element = (ExecutableElement) trees.getElement(path);
                return element.getSimpleName() == optName && element.getEnclosingElement().asType() == operatorsType;
            };

            walkOver(unit, walker -> {
                Tree node = walker.node();
                switch (node.getKind()) {
                    case METHOD_INVOCATION: {
                        MethodInvocationTree inv0 = (MethodInvocationTree) node;
                        if (invFastTest.test(inv0)) {

                            TreePath path = walker.path();
                            helper.attributeExpr(path);
                            if (!invSureTest.test(path)) return;

                            MethodInvocationTree opt = (MethodInvocationTree) path.getLeaf();

                            int optType = opt.getArguments().size();
                            if (optType != 1 && optType != 2) {
                                helper.printNote("opt(...) skipped", path);
                                return;
                            }

                            ExpressionTree arg1 = opt.getArguments().get(0);
                            TreePath arg1Path = new TreePath(path, arg1);

                            TreePath blockPath = null;
                            TreePath stmtPath  = null;
                            int      stmtIndex = -1;
                            { // detect the scope
                                TreePath p = path;
                                StatementTree s = null;
                                while ((p = p.getParentPath()) != null) {
                                    Tree leaf = p.getLeaf();
                                    if (stmtPath == null) {
                                        LambdaExpressionTree lambda;
                                        if (leaf instanceof LambdaExpressionTree
                                                && (lambda = ((LambdaExpressionTree) leaf)).getBodyKind() == BodyKind.EXPRESSION) {
                                            ExpressionTree expr = (ExpressionTree) lambda.getBody();
                                            while (node != expr) node = walker.up();
                                            BlockTree block = asm.reset().at(expr).set(expr).ret().list().block().get();
                                            walker.replaceNode(block);
                                            return;
                                        }
                                    }
                                    if (leaf instanceof BlockTree) {
                                        blockPath = p;
                                        stmtIndex = ((BlockTree) p.getLeaf()).getStatements().indexOf(s);
                                        break;
                                    }
                                    if (leaf instanceof StatementTree) {
                                        s = (StatementTree) leaf;
                                        if (stmtPath == null) {
                                            stmtPath = p;
                                        }
                                    }
                                }
                            }

                            if (stmtPath == null || blockPath == null || stmtIndex < 0) {
                                helper.printError("opt(...) not allowed here", path);
                                return;
                            }

                            TreePath enclExprPath = null;
                            { // detect the outer expression
                                Tree n = node;
                                TreePath p = path;
                                loop:
                                while ((p = p.getParentPath()) != stmtPath) {
                                    Tree expr = p.getLeaf();
                                    switch (expr.getKind()) {
                                        case ARRAY_ACCESS:
                                            if (((ArrayAccessTree) expr).getExpression() != n) break loop;
                                            break;
                                        case MEMBER_SELECT:
                                            break;
                                        case METHOD_INVOCATION:
                                            if (((MethodInvocationTree) expr).getMethodSelect() != n) break loop;
                                            break;
                                        case NEW_CLASS:
                                            if (((NewClassTree) expr).getEnclosingExpression() != n) break loop;
                                            break;
                                    }
                                    enclExprPath = p;
                                    n = expr;
                                }
                            }


                            BlockTree block = (BlockTree) blockPath.getLeaf();
                            List<? extends StatementTree> blockStats = block.getStatements();

                            Set<String> stmtLocals = helper
                                    .getLocalElements(new TreePath(blockPath, blockStats.get(blockStats.size()-1))).stream()
                                    .map(e -> e.getSimpleName().toString())
                                    .collect(Collectors.toSet());


                            List<ExpressionTree> parts = new ArrayList<>();
                            boolean ret = new SimpleTreeVisitor<ExpressionTree,Void>() {
                                boolean visit(Tree node) {
                                    ExpressionTree n = visit(node, null);
                                    if (n != null) {
                                        parts.add(n);
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                                @Override
                                public ExpressionTree visitIdentifier(IdentifierTree node, Void unused) {
                                    return node;
                                }
                                @Override
                                public ExpressionTree visitArrayAccess(ArrayAccessTree node, Void unused) {
                                    return visit(node.getExpression()) ? node : null;
                                }
                                @Override
                                public ExpressionTree visitMethodInvocation(MethodInvocationTree node, Void unused) {
                                    ExpressionTree select = node.getMethodSelect();
                                    if (select.getKind() == IDENTIFIER) return node;
                                    return visit(((MemberSelectTree) select).getExpression()) ? node : null;
                                }
                                @Override
                                public ExpressionTree visitNewClass(NewClassTree node, Void aVoid) {
                                    return visit(node.getEnclosingExpression()) ? node : null;
                                }
                                @Override
                                public ExpressionTree visitMemberSelect(MemberSelectTree node, Void unused) {
                                    return visit(node.getExpression()) ? node : null;
                                }
                            }.visit(arg1);

                            if (!ret) {
                                helper.printMessage(ERROR, "Unsupported expression", arg1Path);
                                return;
                            }

                            if (parts.size() == 0) {
                                helper.printMessage(WARNING, "Nothing to check (skipped)", arg1Path);
                                return;
                            }

                            if (path.getParentPath().getLeaf().getKind() == MEMBER_SELECT) {
                                System.out.println("OUTER SELECT: " + path.getParentPath().getLeaf());
                            }

                            asm.reset().at(opt);

                            int psz = parts.size(), last = psz - (enclExprPath == null ? 2 : 1);
                            List<VariableTree> varDecls = new ArrayList<>(last+2);
                            for (int i=0, vi=1; i <= last; i++) {
                                ExpressionTree t = parts.get(i);
                                String varName;
                                while (stmtLocals.contains(varName = "var" + (vi++)));
                                varDecls.add(asm.declareVar(JavacUtils.typeOf(t), varName).get());
                            }

                            final int A = 0, B = 1, C = 2;

                            Consumer<Tree> makeAccessor = t -> {
                                ExpressionTree expr = asm.get();
                                Tree cpy = asm.set(t).cpy().get();
                                switch (t.getKind()) {
                                    case ARRAY_ACCESS:
                                        Editors.setExpression((ArrayAccessTree) cpy, expr);
                                        break;
                                    case METHOD_INVOCATION:
                                        MethodInvocationTree inv = (MethodInvocationTree) cpy;
                                        Editors.setExpression((MemberSelectTree) inv.getMethodSelect(), expr);
                                        break;
                                    case NEW_CLASS:
                                        Editors.setEnclosingExpression((NewClassTree) cpy, expr);
                                        break;
                                    case MEMBER_SELECT:
                                        Editors.setExpression((MemberSelectTree) cpy, expr);
                                        break;
                                    default:
                                        throw new AssertionError();
                                }
                            };

                            for (int i=0; i <= last; i++) {
                                ExpressionTree t = parts.get(i);

                                if (i == 0) {
                                    asm.set(t);
                                } else {
                                    asm.ident(varDecls.get(i-1));
                                    makeAccessor.accept(t);
                                }

                                asm.ident(B, varDecls.get(i));
                                asm.expr(A, "($B = $A) != null");
                                if (i == 0) {
                                    asm.mov(A, C);
                                } else {
                                    asm.and(C, A);
                                }
                            }

                            asm.mov(C, B);

                            boolean noRet = false;

                            asm.ident(varDecls.get(last));
                            if (enclExprPath == null) {
                                makeAccessor.accept(parts.get(last+1));
                            } else {
                                noRet = optType == 1 && enclExprPath.getParentPath().getLeaf().getKind() == EXPRESSION_STATEMENT;
                                Tree enclExpr = enclExprPath.getLeaf();
                                while (node != enclExpr) node = walker.up();
                                Editors.replaceTree(node, opt, asm.get());
                                asm.set(node);
                                if (noRet) {
                                    node = walker.up();
                                }
                            }

                            if (noRet) {
                                asm.stat(B, "if ($B) $A()");
                            } else {
                                if (optType == 1) {
                                    asm.nil(C);
                                } else {
                                    asm.set(C, opt.getArguments().get(1));
                                }
                                asm.expr(B, "$B ? $A : $C");
                            }

                            Editors.addStatements(block, stmtIndex, varDecls);
                            walker.replaceNode(asm.get(B));
                            System.out.println(block);
                        }
                        break;
                    }
                    case MEMBER_REFERENCE: { // JavaSugar::opt
                        MemberReferenceTree ref = (MemberReferenceTree) node;
                        if (ref.getMode() == ReferenceMode.INVOKE
                                && ref.getName() == optName
                                && ref.getQualifierExpression().toString().equals("JavaSugar")) {
                            TreePath path = walker.path();
                            helper.attribute(path.getParentPath());
                            ExecutableElement element = (ExecutableElement) trees.getElement(path);
                            if (element.getEnclosingElement().asType() == operatorsType) {
                                helper.printError("illegal usage of opt(...)", path);
                            }
                        }
                        break;
                    }
                }
            });

            //System.out.println(unit);
        });


        return false;
    }


    public static void main(String[] args) throws Exception {
        JavacUtils.complile("test-bed/src", new OptProcessor());
        //JavacUtils.complile("samples/tragulus-sugar/src/test/resources", new OptProcessor());
        //System.out.println(JavacUtils.complile(Arrays.asList(new File("C:\\jobbing\\java-custom-ops\\annotation-processor\\test\\test\\OptReference.java")), new File("tmp"), new OptProcessor()));
    }
}
