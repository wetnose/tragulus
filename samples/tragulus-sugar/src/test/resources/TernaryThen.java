import java.util.function.Supplier;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class TernaryThen {

    public static void main(String[] args) {
        Counter c1 = new Counter();
        Holder holder = null;
        Counter ret1 = true ? opt(holder).counter : c1;
        System.out.println("ret1 = " + ret1);

        holder = new Holder();
        Counter ret2 = false ? opt(holder).counter : c1;
        System.out.println("ret2 = " + ret2);

        if (ret1 != null || ret2 != c1) {
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
