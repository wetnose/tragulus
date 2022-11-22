package wn.pseudoclasses;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Alexander A. Solovioff
 * Date: 23.11.2022
 * Time: 2:02 AM
 */
public class InlinerTest extends PseudoTest {

    @Test
    public void cast() throws Exception {
        Assertions.assertTrue( compile(new Processor(), "TypeCast", "IntAnatomy0") );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run("TypeCast", out);
        Assertions.assertEquals("15", out.toString(US_ASCII).trim());
    }
}
