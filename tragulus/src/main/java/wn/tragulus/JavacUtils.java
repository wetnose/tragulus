package wn.tragulus;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

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

import static com.sun.tools.javac.tree.JCTree.Tag.TYPEBOUNDKIND;
import static java.util.Collections.singletonList;
import static wn.tragulus.Editors.replaceTree;


/**
 * Created by Alexander A. Solovioff
 * 05.08.2018
 */
public class JavacUtils {

    public static boolean complile(String srcDir, Processor processor) throws IOException {
        return complile(srcDir, "tmp", processor);
    }

    public static boolean complile(String srcDir, String outDir, Processor processor) throws IOException {
        ArrayList<File> src = new ArrayList<>();
        collectJavaFiles(new File(srcDir), src);
        return complile(src, new File(outDir), processor);
    }


    public static boolean complile(Collection<File> javaFiles, File outDir, Processor processor) throws IOException {
        if (!emptyDir(outDir)) throw new IOException("Cannot empty " + outDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, singletonList(outDir));
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, fileManager.getJavaFileObjectsFromFiles(javaFiles));
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
}
