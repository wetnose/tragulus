package wn.pseudoclasses;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;

/**
 * Alexander A. Solovioff
 * Date: 20.11.2022
 * Time: 4:11 AM
 */
public class ValidatorTest extends PseudoTest {

    @Test
    public void myString() throws Exception {
        Assertions.assertTrue( compile(new Processor(), "MyString") );
    }


    @Test
    public void inner() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "Outer") );
        assertReport(collector, Diagnostic.Kind.ERROR, "'static' modifier expected");
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
    public void goodSubtypeConstructor0() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertTrue( compile(new Processor(), collector, "GoodSubtypeConstructor0") );
    }


    @Test
    public void goodSubtypeConstructor1() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertTrue( compile(new Processor(), collector, "GoodSubtypeConstructor1") );
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
    public void goodWrapperConstructor0() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertTrue( compile(new Processor(), collector, "GoodWrapperConstructor0") );
    }


    @Test
    public void goodWrapperConstructor1() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertTrue( compile(new Processor(), collector, "GoodWrapperConstructor1") );
    }


    @Test
    public void methodOverriding() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "MethodOverriding") );
        assertReport(collector, Diagnostic.Kind.ERROR, "method overriding not supported");
    }


    @Test
    public void methodOverriding2() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "MethodOverriding2") );
        assertReport(collector, Diagnostic.Kind.ERROR, "method overriding not supported");
    }


    @Test
    public void unexpectedAnnotation() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertFalse( compile(new Processor(), collector, "UnexpectedAnnotation") );
        assertReport(collector, Diagnostic.Kind.ERROR, "unexpected annotation type");
    }


    @Disabled
    @Test
    public void intAnatomy() throws Exception {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        Assertions.assertTrue( compile(new Processor(), collector, "IntAnatomy2") );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Supplementary classes & routines                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected File fileOf(String target) {
        return super.fileOf("validate/" + target);
    }
}
