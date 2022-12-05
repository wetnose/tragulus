package wn.pseudoclasses;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Alexander A. Solovioff
 * Date: 23.11.2022
 * Time: 2:02 AM
 */
public class InlinerTest extends PseudoTest {

    @Test
    public void typeCast() throws Exception {
        SourceCollector sc = new SourceCollector("TypeCast");
        Assertions.assertTrue( compile(new Processor(sc), "TypeCast", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("TypeCast", out);
        Assertions.assertEquals("15", out.toString(US_ASCII).trim());
        Assertions.assertEquals(norm(contentOf("TypeCast-patched")), norm(sc.get("TypeCast")));
        //System.out.println(sc.get("TypeCast"));
    }

    @Test
    public void selfCall() throws Exception {
        SourceCollector sc = new SourceCollector("SelfCall");
        Assertions.assertTrue( compile(new Processor(sc), "SelfCall") );
        //System.out.println(sc.get("SelfCall"));
        Assertions.assertEquals(norm(contentOf("SelfCall-patched")), norm(sc.get("SelfCall")));
    }

    @Test
    public void procCall() throws Exception {
        SourceCollector sc = new SourceCollector("ProcCall");
        Assertions.assertTrue( compile(new Processor(sc), "ProcCall", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("ProcCall", out);
        Assertions.assertEquals(
                "getByte(2)\n" +
                "getByte(0)\n" +
                "b00000b\n" +
                "getByte(1)\n" +
                "1110\n" +
                "getByte(1)\n" +
                "34\n" +
                "2\n" +
                "2000001\n" +
                "getByte(0)\n" +
                "getByte(2)\n" +
                "56", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("ProcCall-patched")), norm(sc.get("ProcCall")));
        //System.out.println(sc.get("ProcCall"));
    }

    @Test
    public void prefixIncDec() throws Exception {
        SourceCollector sc = new SourceCollector("PrefixIncDec");
        Assertions.assertTrue( compile(new Processor(sc), "PrefixIncDec", "IntAnatomy1") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("PrefixIncDec", out);
        Assertions.assertEquals("index = 2/1\n35", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("PrefixIncDec-patched")), norm(sc.get("PrefixIncDec")));
    }

    @Test
    public void postfixIncDec() throws Exception {
        SourceCollector sc = new SourceCollector("PostfixIncDec");
        Assertions.assertTrue( compile(new Processor(sc), "PostfixIncDec", "IntAnatomy1") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("PostfixIncDec", out);
        Assertions.assertEquals("index = 1/2\n35", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("PostfixIncDec-patched")), norm(sc.get("PostfixIncDec")));
    }

    @Test
    public void ifThen() throws Exception {
        SourceCollector sc = new SourceCollector("IfThen");
        Assertions.assertTrue( compile(new Processor(sc), "IfThen", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("IfThen", out);
        Assertions.assertEquals("getByte(2)\n12340078", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("IfThen-patched")), norm(sc.get("IfThen")));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Supplementary classes & routines                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private static String norm(String java) {
        String[] lines = java.split("\r?\n");
        return Stream.of(lines).map(String::stripTrailing).collect(Collectors.joining(System.lineSeparator()));
    }


    @Override
    protected File fileOf(String target) {
        return super.fileOf("inline/" + target);
    }


    private static class SourceCollector extends HashMap<String,String> implements Processor.Listener {

        SourceCollector(String ... classes) {
            for (String c : classes) put(c, null);
        }

        @Override
        public void onInlined(String className, String patchedSrc) {
            replace(className, patchedSrc);
        }
    }
}
