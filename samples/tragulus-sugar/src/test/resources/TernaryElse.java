import java.util.function.Supplier;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class TernaryElse {

    public static void main(String[] args) {
        Counter c1 = new Counter();
        Holder holder = null;
        Counter ret1 = true ? c1 : opt(holder).counter;
        System.out.println("ret1 = " + ret1);

        Counter ret2 = false ? c1 : opt(holder).counter;
        System.out.println("ret2 = " + ret2);

        if (ret1 != c1 || ret2 != null) {
            System.exit(1);
        }
    }


    static class Counter {
        boolean hasNext() {
            return true;
        }
    }

    static class Holder {
        Counter counter = new Counter();
    }
}
