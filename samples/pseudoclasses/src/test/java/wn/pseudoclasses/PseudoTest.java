package wn.pseudoclasses;

import org.junit.jupiter.api.Assertions;
import wn.tragulus.JavacUtils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wn.tragulus.JavacUtils.OPT_PROCESS_ERRORS;

/**
 * Alexander A. Solovioff
 * Date: 23.11.2022
 * Time: 2:00 AM
 */
abstract class PseudoTest {


    static void assertReport(DiagnosticCollector<?> collector, Diagnostic.Kind kind, String msg) {
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


    static boolean compile(Processor processor, String ... targets) throws Exception {
        return compile(processor, new DiagnosticCollector<>(), targets);
    }


    static boolean compile(Processor processor, DiagnosticCollector<JavaFileObject> diagnostics,
                                   String ... targets) throws Exception {
        List<File> javaFiles = Stream.of(targets).map(PseudoTest::fileOf).collect(Collectors.toList());
        File testDir = javaFiles.get(0).getParentFile().getParentFile();
        return JavacUtils.complile(javaFiles, new File(testDir, "tmp"), OPT_PROCESS_ERRORS, processor, diagnostics);
    }


    static int run(String target) throws Exception {
        return run(target, System.out);
    }


    static int run(String target, OutputStream out) throws Exception {
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
                out.write(buf, 0, count);
            }
        }
        try (InputStream in = process.getErrorStream()) {
            int count;
            while ((count = in.read(buf)) > 0) {
                out.write(buf, 0, count);
            }
        }
        return ret;
    }
}
