import wn.pseudoclasses.Processor;
import wn.tragulus.JavacUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static wn.tragulus.JavacUtils.OPT_PROCESS_ERRORS;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:40 PM
 */
public class XXX {

    public static void main(String[] args) throws Exception {
        File src = new File("/Users/asoloviev/work/jobbing/tragulus/samples/pseudoclasses/src/test/resources");
        List<File> files = Arrays.asList(
                new File(src, "Client1.java"),
              //new File(src, "inline/FuncCall.java"),
                new File(src, "inline/ProcCall.java"),
                new File(src, "inline/IntAnatomy0.java"));
        JavacUtils.complile(files, new File("tmp"), OPT_PROCESS_ERRORS, new Processor());
        //JavacUtils.complile("/Users/asoloviev/work/jobbing/tragulus/samples/pseudoclasses/src/test/resources", new Processor2());
    }
}
