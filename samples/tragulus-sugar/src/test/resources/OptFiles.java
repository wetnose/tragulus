import java.io.File;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 18.08.2018
 */
public class OptFiles {

    public static void main(String[] args) {
        int fileCount = opt(getFilesDir().listFiles().length, 0);
        if (fileCount != 0) {
            System.exit(2);
        }
    }

    static File getFilesDir() {
        return null;
    }
}
