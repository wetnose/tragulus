package wn.pseudoclasses;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import wn.tragulus.JavacUtils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wn.tragulus.JavacUtils.OPT_PROCESS_ERRORS;

/**
 * Alexander A. Solovioff
 * Date: 20.11.2022
 * Time: 4:11 AM
 */
public class ValidatorTest {

    @Test
    public void myString() throws Exception {
        Assertions.assertTrue( compile(new Processor(), "MyString") );
    }


    @Test
    public void inner() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "Outer") );
        assertReport(collector, Diagnostic.Kind.ERROR, "nested pseudoclasses not supported");
    }


    @Test
    public void local() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "Local") );
        assertReport(collector, Diagnostic.Kind.ERROR, "local pseudoclasses not supported");
    }


    @Test
    public void notFinal() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "NotFinal") );
        assertReport(collector, Diagnostic.Kind.ERROR, "missing final modifier");
    }


    @Test
    public void noBase() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "NoBase") );
        assertReport(collector, Diagnostic.Kind.ERROR, "missing base type");
    }


    @Test
    public void pseudobase() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "Pseudobase", "MyString") );
        assertReport(collector, Diagnostic.Kind.ERROR, "prohibited pseudoclass inheritance");
    }


    @Test
    public void unsupportedDecl() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "UnsupportedDecl") );
        assertReport(collector, Diagnostic.Kind.ERROR, "unsupported declaration");
    }


    @Test
    public void badSubtypeConstructor0() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadSubtypeConstructor0") );
        assertReport(collector, Diagnostic.Kind.ERROR, "prohibited constructor declaration");
    }


    @Test
    public void badSubtypeConstructor1() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadSubtypeConstructor1") );
        assertReport(collector, Diagnostic.Kind.ERROR, "prohibited constructor declaration");
    }


    @Test
    public void badWrapperConstructor0() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadWrapperConstructor0") );
        assertReport(collector, Diagnostic.Kind.ERROR, "prohibited constructor declaration");
    }


    @Test
    public void badWrapperConstructor2() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadWrapperConstructor2") );
        assertReport(collector, Diagnostic.Kind.ERROR, "prohibited constructor declaration");
    }


    @Test
    public void goodWrapperConstructor() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertTrue( compile(new Processor(), collector, "GoodWrapperConstructor") );
    }


    @Test
    public void methodOverriding() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "MethodOverriding") );
        assertReport(collector, Diagnostic.Kind.ERROR, "method overriding not supported");
    }


    @Test
    public void unexpectedAnnotation() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "UnexpectedAnnotation") );
        assertReport(collector, Diagnostic.Kind.ERROR, "unexpected annotation type");
    }


    @Test
    public void intAnatomy() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertTrue( compile(new Processor(), collector, "IntAnatomy2") );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Supplementary classes & routines                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private static void assertReport(DiagnosticCollector<?> collector, Diagnostic.Kind kind, String msg) {
        for (Diagnostic<?> diag : collector.getDiagnostics()) {
            if (diag.getKind() == kind && msg.equals(diag.getMessage(null))) return;
        }
        Assertions.fail("missing " + kind + "'" + msg + "' report");
    }


    private static File fileOf(String target) {
        try {
            URL url = ClassLoader.getSystemResource(target + ".java");
            return new File(url.toURI());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }


    private static boolean compile(Processor processor, String ... targets) throws Exception {
        return compile(processor, new DiagnosticCollector<>(), targets);
    }


    private static boolean compile(Processor processor, DiagnosticCollector<JavaFileObject> diagnostics,
                                   String ... targets) throws Exception {
        List<File> javaFiles = Stream.of(targets).map(ValidatorTest::fileOf).collect(Collectors.toList());
        File testDir = javaFiles.get(0).getParentFile().getParentFile();
        return JavacUtils.complile(javaFiles, new File(testDir, "tmp"), OPT_PROCESS_ERRORS, processor, diagnostics);
    }


    private static int run(String target) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        File javaFile = fileOf(target);
        File testDir = javaFile.getParentFile().getParentFile();
        String[] command = {"java", "-cp", new File(testDir, "tmp").toString(), target};
        System.out.println("> " + String.join(" ", command));
        Process process = runtime.exec(command);
        int ret = process.waitFor();
        System.out.println(ret);
        byte[] buf = new byte[4096];
        try (InputStream in = process.getInputStream()) {
            int count;
            while ((count = in.read(buf)) > 0) {
                System.out.write(buf, 0, count);
            }
        }
        try (InputStream in = process.getErrorStream()) {
            int count;
            while ((count = in.read(buf)) > 0) {
                System.out.write(buf, 0, count);
            }
        }
        return ret;
    }
}
