package wn.pseudoclasses;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import wn.tragulus.BasicProcessor;
import wn.tragulus.TreeAssembler;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
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


    @Test
    public void variable() throws Exception {
        SourceCollector sc = new SourceCollector("Variable");
        Assertions.assertTrue( compile(new Processor(sc), "Variable", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("Variable", out);
        Assertions.assertEquals("getByte(1)\n52", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("Variable-patched")), norm(sc.get("Variable")));
    }


    @Test
    public void newArray() throws Exception {
        SourceCollector sc = new SourceCollector("NewArray");
        Assertions.assertTrue( compile(new Processor(sc), "NewArray", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("NewArray", out);
        Assertions.assertEquals("[1, 2]\n[[1], [2]]\n[[0, 0, 0], [0, 0, 0]]", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("NewArray-patched")), norm(sc.get("NewArray")));
    }


    @Test
    public void forLoop() throws Exception {
        SourceCollector sc = new SourceCollector("ForLoop");
        Assertions.assertTrue( compile(new Processor(sc), "ForLoop", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("ForLoop", out);
        Assertions.assertEquals("getByte(0)\n0\ngetByte(1)\n0\ngetByte(0)\ngetByte(0)", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("ForLoop-patched")), norm(sc.get("ForLoop")));
    }


    @Test
    public void forLoopLabeled() throws Exception {
        SourceCollector sc = new SourceCollector("ForLoopLabeled");
        Assertions.assertTrue( compile(new Processor(sc), "ForLoopLabeled", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("ForLoopLabeled", out);
        Assertions.assertEquals("getByte(0)\n0\ngetByte(1)\n0\ngetByte(0)\ngetByte(0)", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("ForLoopLabeled-patched")), norm(sc.get("ForLoopLabeled")));
    }


    @Test
    public void foreachLoop() throws Exception {
        SourceCollector sc = new SourceCollector("ForeachLoop");
        Assertions.assertTrue( compile(new Processor(sc), "ForeachLoop", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("ForeachLoop", out);
        Assertions.assertEquals("0\n1", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("ForeachLoop-patched")), norm(sc.get("ForeachLoop")));
    }


    @Test
    public void foreachLoopLabeled() throws Exception {
        SourceCollector sc = new SourceCollector("ForeachLoopLabeled");
        Assertions.assertTrue( compile(new Processor(sc), "ForeachLoopLabeled", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("ForeachLoopLabeled", out);
        Assertions.assertEquals("0\n1", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("ForeachLoopLabeled-patched")), norm(sc.get("ForeachLoopLabeled")));
    }


    @Test
    public void whileLoop() throws Exception {
        SourceCollector sc = new SourceCollector("WhileLoop");
         Assertions.assertTrue( compile(new Processor(sc), "WhileLoop", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("WhileLoop", out);
        Assertions.assertEquals("getByte(0)\n0\ngetByte(0)\ngetByte(0)", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("WhileLoop-patched")), norm(sc.get("WhileLoop")));
    }


    @Test
    public void doWhileLoop() throws Exception {
        SourceCollector sc = new SourceCollector("DoWhileLoop");
        Assertions.assertTrue( compile(new Processor(sc), "DoWhileLoop", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("DoWhileLoop", out);
        Assertions.assertEquals("0\ngetByte(0)\ngetByte(0)", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("DoWhileLoop-patched")), norm(sc.get("DoWhileLoop")));
    }


    @Test
    public void assign() throws Exception {
        SourceCollector sc = new SourceCollector("Assign");
        Assertions.assertTrue( compile(new Processor(sc), "Assign", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("Assign", out);
        Assertions.assertEquals("6", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("Assign-patched")), norm(sc.get("Assign")));
    }


    @Test
    public void instanceOfPseudo() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "InstanceOfPseudo", "IntAnatomy0") );
        assertReport(collector, Diagnostic.Kind.ERROR, "regular class expected");
    }


    @Test
    public void instanceOfPseudoArray() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "InstanceOfPseudoArray", "IntAnatomy0") );
        assertReport(collector, Diagnostic.Kind.ERROR, "regular class expected");
    }


    @Test
    public void instanceOfPseudoArray2() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "InstanceOfPseudoArray2", "IntAnatomy0") );
        assertReport(collector, Diagnostic.Kind.ERROR, "regular class expected");
    }


    @Test
    public void instanceOf() throws Exception {
        SourceCollector sc = new SourceCollector("InstanceOf");
        Assertions.assertTrue( compile(new Processor(sc), "InstanceOf", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("InstanceOf", out);
        Assertions.assertEquals("ok", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("InstanceOf-patched")), norm(sc.get("InstanceOf")));
    }


    @Test
    public void badBinary() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadBinary", "IntAnatomy0") );
        assertReport(collector, Diagnostic.Kind.ERROR,
                "bad operand types for binary operator '+'\n" +
                "  first type:  IntAnatomy0\n" +
                "  second type: int");
    }


    @Test
    public void binary() throws Exception {
        SourceCollector sc = new SourceCollector("Binary");
        Assertions.assertTrue( compile(new Processor(sc), "Binary", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("Binary", out);
        Assertions.assertEquals("17", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("Binary-patched")), norm(sc.get("Binary")));
    }


    @Test
    public void badUnary() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadUnary", "IntAnatomy0") );
        assertReport(collector, Diagnostic.Kind.ERROR, "bad operand type IntAnatomy0 for unary operator '-'");
    }


    @Test
    public void unary() throws Exception {
        SourceCollector sc = new SourceCollector("Unary");
        Assertions.assertTrue( compile(new Processor(sc), "Unary", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("Unary", out);
        Assertions.assertEquals("-15", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("Unary-patched")), norm(sc.get("Unary")));
    }


    @Test
    public void badPlusPlus() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadPlusPlus", "IntAnatomy0") );
        assertReport(collector, Diagnostic.Kind.ERROR, "bad operand type IntAnatomy0 for unary operator '++'");
    }


    @Test
    public void badPlusPlus2() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadPlusPlus2", "IntAnatomy0") );
        for (Diagnostic<?> diag : collector.getDiagnostics()) {
            System.out.println(diag.getMessage(null));
        }
        assertReport(collector, Diagnostic.Kind.ERROR, "unexpected type\n" +
                "  required: variable\n" +
                "  found:    value");
    }


    @Test
    public void switchCase() throws Exception {
        SourceCollector sc = new SourceCollector("Switch");
        Assertions.assertTrue( compile(new Processor(sc), "Switch", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("Switch", out);
        Assertions.assertEquals("10", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("Switch-patched")), norm(sc.get("Switch")));
    }


    @Test
    public void badSwitch() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "BadSwitch", "IntAnatomy0") );
        for (Diagnostic<?> diag : collector.getDiagnostics()) {
            System.out.println(diag.getMessage(null));
        }
        assertReport(collector, Diagnostic.Kind.ERROR, "constant expression required");
    }


    @Test
    public void reduce() throws Exception {
        class Processor extends BasicProcessor {
            static final int A = 0, B = 1, C = 2;
            ExpressionTree reduced;
            @Override
            public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                if (roundEnv.processingOver()) return true;
                Pseudos pseudos = new Pseudos(helper);
                Inliner inliner = new Inliner(pseudos);
                TreeAssembler asm = inliner.asm;
                TypeMirror tint = helper.asType(int.class);
                Inliner.Statements stmts = inliner.new Statements();
                stmts.add(asm.declareVar(tint, "x").get());
                stmts.add(asm.declareVar(tint, "y").get());
                stmts.add(asm.ident(A, "x").literal(B, 12).assign(A, B).exec(A).list(A).block(A).get(A));
                StatementTree yx3 = asm.ident(A, "y").ident(B, "x").literal(C, 3)
                        .bin(Tree.Kind.PLUS, B, C).assign(A, B).asStat(A);
                StatementTree brk = asm.brk("label").asStat();
                stmts.add(asm.block(Arrays.asList(yx3, brk)).labeled("label").get());
                Inliner.Extract extr = inliner.new Extract(stmts,
                        asm.ident(A, "y").ident(B, "z").bin(Tree.Kind.MINUS, A, B).asExpr(A));
                System.out.println(extr);
                System.out.println(reduced = extr.reduce());
                return false;
            }
        }
        Processor processor = new Processor();
        compile(processor, "Nop");
        Assertions.assertEquals("12 + 3 - z", String.valueOf(processor.reduced));
    }


    @Test
    public void tryResource() throws Exception {
        SourceCollector sc = new SourceCollector("Try");
        Assertions.assertTrue( compile(new Processor(sc), "Try") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("Try", out);
        Assertions.assertEquals(
                "r0 created\nr1 created\nr2 created\nr3 created\n" +
                "block\n" +
                "r3 closed\nr0 closed\nr2 closed\nr1 closed\n" +
                "done",
                norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("Try-patched")), norm(sc.get("Try")));
    }


    @Test
    public void retType() throws Exception {
        SourceCollector sc = new SourceCollector("RetType");
        Assertions.assertTrue( compile(new Processor(sc), "RetType", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("RetType", out);
        Assertions.assertEquals("15", norm(out.toString(US_ASCII)));
        Assertions.assertEquals(norm(contentOf("RetType-patched")), norm(sc.get("RetType")));
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
