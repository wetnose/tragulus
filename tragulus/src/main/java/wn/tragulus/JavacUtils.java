package wn.tragulus;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.sun.tools.javac.code.Flags.PUBLIC;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPEBOUNDKIND;
import static java.util.Collections.singletonList;
import static wn.tragulus.Editors.replaceTree;


/**
 * Created by Alexander A. Solovioff
 * 05.08.2018
 */
public class JavacUtils {

    public static final int OPT_PROCESS_ERRORS = 1;


    public static boolean complile(String srcDir, Processor processor) throws IOException {
        return complile(srcDir, 0, processor);
    }


    public static boolean complile(String srcDir, int opt, Processor processor) throws IOException {
        return complile(srcDir, "tmp", opt, processor);
    }

    public static boolean complile(String srcDir, String outDir, int opt, Processor processor) throws IOException {
        ArrayList<File> src = new ArrayList<>();
        collectJavaFiles(new File(srcDir), src);
        return complile(src, new File(outDir), opt, processor);
    }


    public static boolean complile(Collection<File> javaFiles, File outDir, int opt, Processor processor) throws IOException {
        return complile(javaFiles, outDir, opt, processor, new DiagnosticCollector<>());
    }


    public static boolean complile(Collection<File> javaFiles, File outDir, int opt, Processor processor,
                                   DiagnosticCollector<JavaFileObject> diagnostics) throws IOException {
        if (!emptyDir(outDir)) throw new IOException("Cannot empty " + outDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, singletonList(outDir));
        ArrayList<String> options;
        if (opt == 0) {
            options = null;
        } else {
            options = new ArrayList<>();
            if ((opt & OPT_PROCESS_ERRORS) != 0) {
                options.add("-XDshouldStopPolicyIfError=PROCESS"); // JDK 1.8
                options.add("-XDshould-stop.ifError=PROCESS");
            }
//            options.add("-source");
//            options.add("9");
//            options.add("-target");
//            options.add("9");
        }
        //((com.sun.tools.javac.main.JavaCompiler) compiler).shouldStopPolicyIfError = CompileStates.CompileState.INIT;
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, fileManager.getJavaFileObjectsFromFiles(javaFiles));
        //Options.instance(((BasicJavacTask) task).getContext()).put("shouldStopPolicyIfError", "PROCESS");
        if (processor != null) task.setProcessors(singletonList(processor));
        boolean success = task.call();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            System.err.println(diagnostic);
        }
        return success;
    }


    private static void collectJavaFiles(File f, Collection<File> javaFiles) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) collectJavaFiles(c, javaFiles);
        } else
        if (f.getName().endsWith(".java")) {
            javaFiles.add(f);
        }
    }


    private static boolean emptyDir(File f) throws IOException {
        if (f.exists()) {
            if (!f.isDirectory()) throw new IOException(f + " is not a directory");
            boolean res = true;
            //noinspection ConstantConditions
            for (File c : f.listFiles()) {
                res &= delete(c);
            }
            return res;
        } else {
            return f.mkdirs();
        }
    }


    private static boolean delete(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles(); assert children != null;
            for (File c : children) delete(c);
        }
        return f.delete();
    }


    public static TypeMirror typeOf(Tree node) {
        return ((JCTree) node).type;
    }


    public static PackageElement packageOf(CompilationUnitTree node) {
        return ((JCCompilationUnit) node).packge;
    }


    public static TypeElement asElement(ClassTree node) {
        return ((JCClassDecl) node).sym;
    }


    public static TreePath getEnclosingBlock(TreePath path) {
        return getEnclosingTree(path, node -> node.getKind() == Tree.Kind.BLOCK);
    }


    public static TreePath getEnclosingStatement(TreePath path) {
        return getEnclosingTree(path, JavacUtils::isStatement);
    }


    public static TreePath getEnclosingExpression(TreePath path) {
        return getEnclosingTree(path, JavacUtils::isExpression);
    }


    public static TreePath getEnclosingTree(TreePath path, Predicate<Tree> predicate) {
        if (path == null) return null;
        while ((path = path.getParentPath()) != null) {
            if (predicate.test(path.getLeaf())) return path;
        }
        return null;
    }


    public static boolean isStatement(Tree node) {
        return node instanceof StatementTree;
    }


    public static boolean isExpression(Tree node) {
        return node instanceof ExpressionTree;
    }


    public static boolean isVariableElement(Element element) {
        return element instanceof VariableElement;
    }


    public static boolean isPublic(Element element) {
        return (((Symbol) element).flags() & PUBLIC) != 0;
    }


    public static Tree findFirst(Tree root, Predicate<Tree> predicate) {
        if (root == null || predicate == null) return null;
        return root.accept(new TreeScanner<Tree,Void>() {
            Tree found;
            @Override
            public Tree scan(Tree node, Void unused) {
                if (found != null) return found;
                if (predicate.test(node)) {
                    found = node;
                    return node;
                }
                return super.scan(node, null);
            }
            @Override
            public Tree reduce(Tree r1, Tree r2) {
                return r1;
            }
        }, null);
    }


    public static void scan(Tree root, Consumer<Tree> consumer) {
        if (root == null || consumer == null) return;
        root.accept(new TreeScanner<Void,Void>() {
            @Override
            public Void scan(Tree node, Void unused) {
                if (node == null) return null;
                consumer.accept(node);
                super.scan(node, null);
                return null;
            }
        }, null);
    }


    public static void walkOver(CompilationUnitTree unit, Consumer<TreeWalker> consumer) {
        walkOver(new TreePath(unit), consumer);
    }


    public static void walkOver(TreePath root, Consumer<TreeWalker> consumer) {
        if (root == null || consumer == null) return;
        new TreeWalker(root).start(consumer);
    }


    public static class TreeWalker {

        private final TreePath root;
        private final ArrayList<Object> stack = new ArrayList<>();

        private JCTree top;
        private JCTree bak;

        private TreeWalker(TreePath root) {
            this.root = root;
        }

        private boolean isOver(JCTree node) {
            JCTree bk;
            if ((bk = bak) != null) {
                if (bk != node) return true;
                bak = null;
            }
            return false;
        }

        private void start(Consumer<TreeWalker> consumer) {
            JCTree rootNode = (JCTree) root.getLeaf();
            new com.sun.tools.javac.tree.TreeScanner() {
                @Override
                public void scan(JCTree node) {
                    if (node != null) {
                        push(node);
                        try {
                            if (node.getTag() != TYPEBOUNDKIND) consumer.accept(TreeWalker.this);
                            if (isOver(node)) return;
                            do {
                                super.scan(node = top);
                                if (isOver(node)) return;
                            } while (node != top);
                        } finally {
                            pop(node);
                        }
                    }
                }
            }.scan(rootNode);
        }

        public Tree node() {
            return top;
        }

        private void push(JCTree node) {
            TreePath root = this.root;
            stack.add(node == root.getLeaf() ? root : node);
            top = node;
        }

        private void pop() {
            int sz = stack.size()-1;
            stack.remove(sz);
            top = sz == 0 ? null : tree(stack.get(sz-1));
        }

        private void pop(JCTree node) {
            if (top == node) {
                pop();
            }
        }

        private JCTree tree(Object elem) {
            return (JCTree) (elem instanceof TreePath ? ((TreePath) elem).getLeaf() : elem);
        }

        private TreePath path(int idx) {
            Object elem = stack.get(idx);
            if (elem instanceof TreePath) return (TreePath) elem;
            TreePath path = new TreePath(path(idx-1), (Tree) elem);
            stack.set(idx, path);
            return path;
        }

        public TreePath path() {
            int sz;
            if ((sz = stack.size()) == 0) throw new IllegalStateException();
            return path(sz-1);
        }

        public Tree up() {
            pop();
            JCTree top = this.top;
            bak = top;
            return top;
        }

        public void replaceNode(Tree repl) {
            if (replaceTree(path(), repl)) {
                stack.set(stack.size()-1, repl);
                top = (JCTree) repl;
            } else {
                throw new AssertionError();
            }
        }
    }


    public static void relocate(Tree root, Tree base, int correction) {
        if (root == null) return;
        int oldPos = ((JCTree) root).getStartPosition();
        int newPos = ((JCTree) base).getStartPosition();
        int offset  = correction + newPos - oldPos;
        new TreeScanner<Void,Void>() {
            @Override
            public Void scan(Tree node, Void unused) {
                if (node == null) return null;
                ((JCTree) node).pos += offset;
                return super.scan(node, unused);
            }
        }.scan(root, null);
    }


    static void cleanup(Tree tree) {
        if (tree == null) return;
        ((JCTree) tree).accept(new JCTree.Visitor() {
            @Override
            public void visitTree(JCTree node) {
                node.type = null;
            }
        });
    }


    public static Tree getTree(Diagnostic<? extends JavaFileObject> diagnostic) {
        DiagnosticPosition position = ((JCDiagnostic) diagnostic).getDiagnosticPosition();
        return position == null ? null : position.getTree();
    }


    public static <E extends Element> E asElement(TypeMirror type) {
        //noinspection unchecked
        return (E) ((Type) type).asElement();
    }


    public static <E extends Element> E asElement(Tree tree) {
        //noinspection unchecked
        return (E) TreeInfo.symbolFor((JCTree) tree);
    }


    public static long modifiersOf(ClassTree tree) {
        return ((JCModifiers) tree.getModifiers()).flags;

    }

    // 14.8. Expression Statements
    public static boolean isStatementExpression(ExpressionTree tree) {
        //return TreeInfo.isExpressionStatement((JCExpression) tree);
        switch (tree.getKind()) {
            case ASSIGNMENT:
            case PREFIX_INCREMENT:
            case PREFIX_DECREMENT:
            case POSTFIX_INCREMENT:
            case POSTFIX_DECREMENT:
            case METHOD_INVOCATION:
            case NEW_CLASS:
                return true;
            default:
                return false;
        }
    }


//    public static boolean isAssignment(Tree.Kind kind) {
//        if (kind == null) return false;
//        switch (kind) {
//            case ASSIGNMENT:                      // =
//            case PREFIX_INCREMENT:                // ++ _
//            case PREFIX_DECREMENT:                // -- _
//            case POSTFIX_INCREMENT:               // _ ++
//            case POSTFIX_DECREMENT:               // _ --
//            case MULTIPLY_ASSIGNMENT:             // *=
//            case DIVIDE_ASSIGNMENT:               // /=
//            case REMAINDER_ASSIGNMENT:            // %=
//            case PLUS_ASSIGNMENT:                 // +=
//            case MINUS_ASSIGNMENT:                // -=
//            case LEFT_SHIFT_ASSIGNMENT:           // <<=
//            case RIGHT_SHIFT_ASSIGNMENT:          // >>=
//            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT: // >>>=
//            case AND_ASSIGNMENT:                  // &=
//            case XOR_ASSIGNMENT:                  // ^=
//            case OR_ASSIGNMENT:                   // |=
//                return true;
//            default:
//                return false;
//        }
//    }


    public static ExpressionTree getAssignableVariable(Tree expr) {
        if (expr == null) return null;
        switch (expr.getKind()) {
            case ASSIGNMENT:                      // =
                return ((AssignmentTree) expr).getVariable();
            case PREFIX_INCREMENT:                // ++ _
            case PREFIX_DECREMENT:                // -- _
            case POSTFIX_INCREMENT:               // _ ++
            case POSTFIX_DECREMENT:               // _ --
                return ((UnaryTree) expr).getExpression();
            case MULTIPLY_ASSIGNMENT:             // *=
            case DIVIDE_ASSIGNMENT:               // /=
            case REMAINDER_ASSIGNMENT:            // %=
            case PLUS_ASSIGNMENT:                 // +=
            case MINUS_ASSIGNMENT:                // -=
            case LEFT_SHIFT_ASSIGNMENT:           // <<=
            case RIGHT_SHIFT_ASSIGNMENT:          // >>=
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT: // >>>=
            case AND_ASSIGNMENT:                  // &=
            case XOR_ASSIGNMENT:                  // ^=
            case OR_ASSIGNMENT:                   // |=
                return ((CompoundAssignmentTree) expr).getVariable();
            default:
                return null;
        }
    }


    static Tag kindToTag(Tree.Kind kind) {
        if (kind == null) return null;
        switch (kind) {

            // Unary operators

            case PREFIX_INCREMENT     : return Tag.PREINC;     // ++ _
            case PREFIX_DECREMENT     : return Tag.PREDEC;     // -- _
            case POSTFIX_INCREMENT    : return Tag.POSTINC;    // _ ++
            case POSTFIX_DECREMENT    : return Tag.POSTDEC;    // _ --
            case UNARY_PLUS           : return Tag.POS;        // +
            case UNARY_MINUS          : return Tag.NEG;        // -
            case BITWISE_COMPLEMENT   : return Tag.COMPL ;     // ~
            case LOGICAL_COMPLEMENT   : return Tag.NOT;        // !

            // Binary operators

            case MULTIPLY             : return Tag.MUL;        // *
            case DIVIDE               : return Tag.DIV;        // /
            case REMAINDER            : return Tag.MOD;        // %
            case PLUS                 : return Tag.PLUS;       // +
            case MINUS                : return Tag.MINUS;      // -
            case LEFT_SHIFT           : return Tag.SL;         // <<
            case RIGHT_SHIFT          : return Tag.SR;         // >>
            case UNSIGNED_RIGHT_SHIFT : return Tag.USR;        // >>>
            case LESS_THAN            : return Tag.LT;         // <
            case GREATER_THAN         : return Tag.GT;         // >
            case LESS_THAN_EQUAL      : return Tag.LE;         // <=
            case GREATER_THAN_EQUAL   : return Tag.GE;         // >=
            case EQUAL_TO             : return Tag.EQ;         // ==
            case NOT_EQUAL_TO         : return Tag.NE;         // !=
            case AND                  : return Tag.BITAND;     // &
            case XOR                  : return Tag.BITXOR;     // ^
            case OR                   : return Tag.BITOR;      // |
            case CONDITIONAL_AND      : return Tag.AND;        // &&
            case CONDITIONAL_OR       : return Tag.OR;         // ||

            // Assignment operators

            case MULTIPLY_ASSIGNMENT  : return Tag.MUL_ASG;    // *=
            case DIVIDE_ASSIGNMENT    : return Tag.DIV_ASG;    // /=
            case REMAINDER_ASSIGNMENT : return Tag.MOD_ASG;    // %=
            case PLUS_ASSIGNMENT      : return Tag.PLUS_ASG;   // +=
            case MINUS_ASSIGNMENT     : return Tag.MINUS_ASG;  // -=
            case LEFT_SHIFT_ASSIGNMENT: return Tag.SL_ASG;     // <<=
            case RIGHT_SHIFT_ASSIGNMENT:
                                        return Tag.SR_ASG;     // >>=
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                                        return Tag.USR_ASG;    // >>>=
            case AND_ASSIGNMENT       : return Tag.BITAND_ASG; // &=
            case XOR_ASSIGNMENT       : return Tag.BITXOR_ASG; // ^=
            case OR_ASSIGNMENT        : return Tag.BITOR_ASG;  // |=
        }
        throw new IllegalArgumentException();
    }
}
