package wn.tragulus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static java.util.Collections.singletonList;

public class OptProcessorTest {

    @Test
    public void simplePositive() throws Exception {
        Assertions.assertTrue( compile("OptSimple", new OptProcessor()) );
        int ret = run("OptSimple");
        Assertions.assertEquals(0, ret);
    }

    @Test
    public void simpleNegative() throws Exception {
        Assertions.assertTrue( compile("OptSimple", new FakeProcessor()) );
        int ret = run("OptSimple");
        Assertions.assertEquals(1, ret);
    }


    @Test
    public void lambdaPositive() throws Exception {
        Assertions.assertTrue( compile("OptLambda", new OptProcessor()) );
        int ret = run("OptLambda");
        Assertions.assertEquals(0, ret);
    }

    @Test
    public void lambdaNegative() throws Exception {
        Assertions.assertTrue( compile("OptLambda", new FakeProcessor()) );
        int ret = run("OptLambda");
        Assertions.assertEquals(1, ret);
    }


    @Test
    public void nestedPositive() throws Exception {
        Assertions.assertTrue( compile("OptNested", new OptProcessor()) );
        int ret = run("OptNested");
        Assertions.assertEquals(0, ret);
    }

    @Test
    public void nestedNegative() throws Exception {
        Assertions.assertTrue( compile("OptNested", new FakeProcessor()) );
        int ret = run("OptNested");
        Assertions.assertEquals(1, ret);
    }


    @Test
    public void enclosingPositive() throws Exception {
        System.out.println(new File(".").getCanonicalPath());
        Assertions.assertTrue( compile("OptEnclosing", new OptProcessor()) );
        int ret = run("OptEnclosing");
        Assertions.assertEquals(0, ret);
    }

    @Test
    public void enclosingNegative() throws Exception {
        Assertions.assertTrue( compile("OptEnclosing", new FakeProcessor()) );
        int ret = run("OptEnclosing");
        Assertions.assertEquals(1, ret);
    }


    @Test
    public void voidPositive() throws Exception {
        Assertions.assertTrue( compile("OptVoid", new OptProcessor()) );
        int ret = run("OptVoid");
        Assertions.assertEquals(0, ret);
    }

    @Test
    public void voidNegative() throws Exception {
        Assertions.assertTrue( compile("OptVoid", new FakeProcessor()) );
        int ret = run("OptVoid");
        Assertions.assertEquals(1, ret);
    }


    @Test
    public void newPositive() throws Exception {
        Assertions.assertTrue( compile("OptNew", new OptProcessor()) );
        int ret = run("OptNew");
        Assertions.assertEquals(0, ret);
    }

    @Test
    public void newNegative() throws Exception {
        Assertions.assertTrue( compile("OptNew", new FakeProcessor()) );
        int ret = run("OptNew");
        Assertions.assertEquals(1, ret);
    }


    @Test
    public void filesPositive() throws Exception {
        Assertions.assertTrue( compile("OptFiles", new OptProcessor()) );
        int ret = run("OptFiles");
        Assertions.assertEquals(0, ret);
    }

    @Test
    public void filesNegative() throws Exception {
        Assertions.assertTrue( compile("OptFiles", new FakeProcessor()) );
        int ret = run("OptFiles");
        Assertions.assertEquals(1, ret);
    }


    @Test
    public void reference() throws Exception {
        Assertions.assertFalse( compile("OptReference", new OptProcessor()) );
    }

    @Test
    public void secondArg() throws Exception {
        Assertions.assertTrue( compile("OptSecondArg", new OptProcessor()) );
        int ret = run("OptSecondArg");
        Assertions.assertEquals(0, ret);
    }

    @Test
    public void forLoop() throws Exception {
        Assertions.assertTrue( compile("OptForLoop", new OptProcessor()) );
        int ret = run("OptForLoop");
        Assertions.assertEquals(0, ret);
        int a, b;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Supplementary classes & routines                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


//    private static File projectDir() {
//        File currDir = new File(".");
//        //noinspection ConstantConditions
//        List<String> content = Arrays.asList(currDir.list((dir, name) -> name.equals("tragulus") || name.equals("src")));
//        if (content.isEmpty()) throw new AssertionError("Invalid working dir '" + currDir + "'");
//        if (content.contains("tragulus")) {
//            return currDir;
//        } else {
//            return new File(currDir, "..");
//        }
//    }


    private static File fileOf(String target) throws Exception {
        URL url = ClassLoader.getSystemResource(target + ".java");
        return new File(url.toURI());
    }


    private static boolean compile(String target, Processor processor) throws Exception {
        File javaFile = fileOf(target);
        File testDir = javaFile.getParentFile().getParentFile();
        return JavacUtils.complile(singletonList(javaFile), new File(testDir, "tmp"), 0, processor);
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
