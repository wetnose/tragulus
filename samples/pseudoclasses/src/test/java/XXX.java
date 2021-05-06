import wn.pseudoclasses.Processor;
import wn.tragulus.JavacUtils;

import static wn.tragulus.JavacUtils.OPT_PROCESS_ERRORS;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:40 PM
 */
public class XXX {

    public static void main(String[] args) throws Exception {
        JavacUtils.complile("/Users/asoloviev/work/jobbing/tragulus/samples/pseudoclasses/src/test/resources", OPT_PROCESS_ERRORS, new Processor());
        //JavacUtils.complile("/Users/asoloviev/work/jobbing/tragulus/samples/pseudoclasses/src/test/resources", new Processor2());
    }
}
