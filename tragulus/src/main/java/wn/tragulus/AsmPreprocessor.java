package wn.tragulus;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sun.source.tree.Tree.Kind.*;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EOF;
import static javax.lang.model.SourceVersion.RELEASE_9;


@SupportedSourceVersion(RELEASE_9)
public class AsmPreprocessor extends BasicProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Trees trees = helper.getTreeUtils();
        Types types = helper.getTypeUtils();
        Elements elements = helper.getElementUtils();
        TreeAssembler asm = helper.newAssembler(4);

        TypeMirror processorType = helper.asType(Processor.class);

        TypeElement treeAsmElem = helper.getElementUtils().getTypeElement(TreeAssembler.class.getName());
        TypeMirror treeAsmType = treeAsmElem.asType();

        Predicate<CompilationUnitTree> importFilter = importFilter(treeAsmType);

        List<CompilationUnitTree> units = roundEnv.getRootElements().stream()
                .filter(e -> e.getKind() == ElementKind.CLASS).map(e -> (TypeElement) e)
                .filter(t -> types.isAssignable(t.asType(), processorType))
                .map   (helper::getUnit).distinct()
                .filter(importFilter)
                .collect(Collectors.toList());

        Set<Name> macroNames = Stream.of("expr", "stat")
                .sorted()
                .map(elements::getName)
                .collect(Collectors.toSet());

        Name exprName = elements.getName("expr");

        Predicate<MethodInvocationTree> invFastTest = inv -> macroNames.contains(methodName(inv));

        Predicate<TreePath> invSureTest = path -> {
            helper.attributeExpr(path);
            ExecutableElement element = (ExecutableElement) trees.getElement(path);
            return element.getEnclosingElement().asType() == treeAsmType;
        };

        MacroProcessor macroProcessor = new MacroProcessor(helper.context());

        units.forEach(unit -> {
            JavacUtils.walkOver(unit, walker -> {
                Tree node = walker.node();
                TreePath path;
                if (node.getKind() == METHOD_INVOCATION
                        && invFastTest.test((MethodInvocationTree) node)
                        && invSureTest.test(path = walker.path())) {

                    MethodInvocationTree inv = (MethodInvocationTree) node;
                    System.out.println(inv);
                    List<? extends Tree> args = new ArrayList<>(inv.getArguments());

                    int argCount = args.size();
                    if (argCount < 1 || argCount > 2) {
                        helper.printError("unexpected argument count", path);
                        return;
                    }

                    Tree srcArg = args.get(argCount-1);
                    TreePath srcArgPath = new TreePath(path, srcArg);
                    if (srcArg.getKind() != STRING_LITERAL) {
                        helper.printError("single string literal expected", srcArgPath);
                        return;
                    }

                    boolean exprMode = methodName(inv) == exprName;

                    String scr = (String) ((LiteralTree) srcArg).getValue();
                    JavacParser parser;
                    Tree parsedTree;
                    int off = 0;
                    if (exprMode) {
                        parsedTree = (parser = newParser(scr)).parseExpression();
                    } else {
                        String trimmed = scr.trim();
                        BlockTree block;
                        if (trimmed.startsWith("{")) {
                            parsedTree = (parser = newParser(scr)).block();
                        } else {
                            scr = trimmed.endsWith(";") ? '{' + scr + '}' : '{' + scr + ";}";
                            block = (parser = newParser(scr)).block();
                            List<? extends StatementTree> stats = block.getStatements();
                            parsedTree = stats.size() == 1 ? stats.get(0) : block;
                            off = -1;
                        }
                    }
                    JavacUtils.relocate(parsedTree, srcArg, off);
                    if (parser.token().kind != EOF) {
                        ((JCTree) parsedTree).pos += parser.token().pos + off;
                        helper.printError((exprMode ? "expression" : "statement") + " syntax error", new TreePath(srcArgPath, parsedTree));
                        return;
                    }

//                    Scope scope = trees.getScope(srcArgPath);
//                    System.out.println(scope);
                    System.out.println(parsedTree);

                    Map<String,VariableElement> srcScope = helper.getLocalElements(srcArgPath).stream()
                            .filter(JavacUtils::isVariableElement)
                            .map   (e -> (VariableElement) e)
                            .collect(Collectors.toMap(v -> v.getSimpleName().toString(), v -> v));

                    TreePath exprPath = new TreePath(path, parsedTree);

                    int[] err = {0};

                    JavacUtils.walkOver(exprPath, w -> {
                        Tree n = w.node();
                        String ref = n.getKind() == IDENTIFIER ? asRef(((IdentifierTree) n).getName()) : null;
                        if (ref != null)  {
                            VariableElement var;
                            if (isNum(ref)) return;
                            if ((var = srcScope.get(ref)) == null) {
                                err[0]++;
                                helper.printError("Unknown variable ''" + ref + '\'', w.path());
                                return;
                            }
                            if (var.asType().getKind() != TypeKind.INT) {
                                err[0]++;
                                helper.printError("Invalid variable type ('int' expected)", w.path());
                            }
                        }
                    });

                    if (err[0] > 0) return;

                    final int A=0, B=1, C=2, D=3;

                    Function<String,String> varNameGen = base -> {
                        if (!srcScope.containsKey(base)) return base;
                        String name;
                        int i = 1;
                        while (srcScope.containsKey(name = base + i)) i++;
                        return name;
                    };

                    String N   = varNameGen.apply("N");
                    String M   = varNameGen.apply("M");
                    String MEM = varNameGen.apply("mem");

                    macroProcessor.setScope(N, M);

                    Consumer<String> asm_ref = ref -> {
                        if (isNum(ref)) {
                            asm.literal(B, Integer.parseInt(ref));
                        } else {
                            asm.ident(B, ref);
                        }
                    };

                    ExpressionTree maker = macroProcessor.process(parsedTree, new MacroProcessor.Filter() {
                        @Override
                        public ExpressionTree filterNode(Tree n) {
                            switch (n.getKind()) {
                                case EXPRESSION_STATEMENT: {
                                    ExpressionStatementTree exec = (ExpressionStatementTree) n;
                                    n = exec.getExpression();
                                    if (n.getKind() != METHOD_INVOCATION) break;
//                                }
//                                case METHOD_INVOCATION: {
                                    MethodInvocationTree mi = (MethodInvocationTree) n;
                                    if (!mi.getTypeArguments().isEmpty()) return null;
                                    n = mi.getMethodSelect();
                                    if (n.getKind() != IDENTIFIER) break;
                                    String ref = asRef(((IdentifierTree) n).getName());
                                    if (ref == null) break;
                                    asm.at(n);
                                    asm_ref.accept(ref);
                                    asm.ident(MEM).select("asStat").invoke(A, B);
                                    return asm.get();
                                }
                                case IDENTIFIER: {
                                    String ref = asRef(((IdentifierTree) n).getName());
                                    if (ref == null) break;
                                    asm.at(n);
                                    asm_ref.accept(ref);
                                    asm.ident(MEM).select("asExpr").invoke(A, B);
                                    return asm.get();
                                }
                            }
                            return null;
                        }
                        @Override
                        public ExpressionTree filterName(Name name) {
                            String ref = asRef(name);
                            if (ref == null) return null;
                            asm.reset();
                            asm_ref.accept(ref);
                            asm.ident(MEM).select("asName").invoke(A, B);
                            return asm.get();
                        }
                    });

                    asm.at(maker);
                    asm.set(A, maker);
                    asm.declareVar(B, helper.asType(TreeMaker    .class), M);
                    asm.declareVar(C, helper.asType(Names        .class), N);
                    asm.declareVar(D, helper.asType(TreeAssembler.class), MEM);
                    asm.lambda(A, B, C, D);

                    System.out.println(MacroProcessor.toString(asm.<JCTree>get()));

                    Editors.replaceTree(srcArgPath, asm.get());
                }
            });
            JavacUtils.cleanup(unit);
        });

        return false;
    }


    private JavacParser newParser(String s) {
        return ParserFactory.instance(helper.context()).newParser(s, false, false, false);
    }


    private static Name methodName(MethodInvocationTree inv) {
        Tree sel = inv.getMethodSelect();
        return sel.getKind() == MEMBER_SELECT ? ((MemberSelectTree) sel).getIdentifier() : ((IdentifierTree) sel).getName();
    }


    private static String asRef(Name candidate) {
        return candidate != null && candidate.charAt(0) == '$'
                ? candidate.subSequence(1, candidate.length()).toString()
                : null;
    }


    private static boolean isNum(String s) {
        int len;
        if (s == null || (len = s.length()) == 0 || len > 9) return false;
        for (int i=0; i < len; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }


    public static void main(String[] args) throws Exception {
        JavacUtils.complile("tragulus-sugar-designtime/src", new AsmPreprocessor());
//        JavacUtils.complile(
//                Collections.singletonList(new File("tragulus-sugar-designtime/src/wn/tragulus/JsonProcessor.java")),
//                new File("tmp"), new AsmPreprocessor());
    }
}
