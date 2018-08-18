import wn.tragulus.JavaSugar;

import java.util.function.Function;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptReference {

    public static void main(String[] args) {
        accpet(JavaSugar::opt);
    }

    static <X,Y> void accpet(Function<X,Y> opt) {
        System.out.println("accepted");
    }
}
