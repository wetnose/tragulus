package wn.tragulus;

/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class JavaSugar {


    public static int opt(int expr, int def) {
        throw new AssertionError("opt(...) not processed");
    }

    public static <T> T opt(T expr) {
        throw new AssertionError("opt(...) not processed");
    }
}
