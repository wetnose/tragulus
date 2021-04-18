package wn.tragulus;

import org.junit.Assert;
import org.junit.Test;

import javax.annotation.processing.Processor;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static java.util.Collections.singletonList;

public class OptProcessorTest {

    @Test
    public void simplePositive() throws Exception {
        Assert.assertTrue( compile("OptSimple", new OptProcessor()) );
        int ret = run("OptSimple");
        Assert.assertEquals(0, ret);
    }

    @Test
    public void simpleNegative() throws Exception {
        Assert.assertTrue( compile("OptSimple", new FakeProcessor()) );
        int ret = run("OptSimple");
        Assert.assertEquals(1, ret);
    }


    @Test
    public void lambdaPositive() throws Exception {
        Assert.assertTrue( compile("OptLambda", new OptProcessor()) );
        int ret = run("OptLambda");
        Assert.assertEquals(0, ret);
    }

    @Test
    public void lambdaNegative() throws Exception {
        Assert.assertTrue( compile("OptLambda", new FakeProcessor()) );
        int ret = run("OptLambda");
        Assert.assertEquals(1, ret);
    }


    @Test
    public void nestedPositive() throws Exception {
        Assert.assertTrue( compile("OptNested", new OptProcessor()) );
        int ret = run("OptNested");
        Assert.assertEquals(0, ret);
    }

    @Test
    public void nestedNegative() throws Exception {
        Assert.assertTrue( compile("OptNested", new FakeProcessor()) );
        int ret = run("OptNested");
        Assert.assertEquals(1, ret);
    }


    @Test
    public void enclosingPositive() throws Exception {
        System.out.println(new File(".").getCanonicalPath());
        Assert.assertTrue( compile("OptEnclosing", new OptProcessor()) );
        int ret = run("OptEnclosing");
        Assert.assertEquals(0, ret);
    }

    @Test
    public void enclosingNegative() throws Exception {
        Assert.assertTrue( compile("OptEnclosing", new FakeProcessor()) );
        int ret = run("OptEnclosing");
        Assert.assertEquals(1, ret);
    }


    @Test
    public void voidPositive() throws Exception {
        Assert.assertTrue( compile("OptVoid", new OptProcessor()) );
        int ret = run("OptVoid");
        Assert.assertEquals(0, ret);
    }

    @Test
    public void voidNegative() throws Exception {
        Assert.assertTrue( compile("OptVoid", new FakeProcessor()) );
        int ret = run("OptVoid");
        Assert.assertEquals(1, ret);
    }


    @Test
    public void newPositive() throws Exception {
        Assert.assertTrue( compile("OptNew", new OptProcessor()) );
        int ret = run("OptNew");
        Assert.assertEquals(0, ret);
    }

    @Test
    public void newNegative() throws Exception {
        Assert.assertTrue( compile("OptNew", new FakeProcessor()) );
        int ret = run("OptNew");
        Assert.assertEquals(1, ret);
    }


    @Test
    public void filesPositive() throws Exception {
        Assert.assertTrue( compile("OptFiles", new OptProcessor()) );
        int ret = run("OptFiles");
        Assert.assertEquals(0, ret);
    }

    @Test
    public void filesNegative() throws Exception {
        Assert.assertTrue( compile("OptFiles", new FakeProcessor()) );
        int ret = run("OptFiles");
        Assert.assertEquals(1, ret);
    }


    @Test
    public void reference() throws Exception {
        Assert.assertFalse( compile("OptReference", new OptProcessor()) );
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
        return JavacUtils.complile(singletonList(javaFile), new File(testDir, "tmp"), processor);
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
