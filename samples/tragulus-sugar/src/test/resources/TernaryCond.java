import java.util.function.Supplier;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class TernaryCond {

    public static void main(String[] args) {
        Holder holder = null;
        String ret1 = opt(holder.counter).hasNext() ? "yes" : "no";
        System.out.println("ret1 = " + ret1);

        holder = new Holder();
        String ret2 = opt(holder.counter).hasNext() ? "yes" : "no";
        System.out.println("ret2 = " + ret2);

        if (!"no".equals(ret1) || !"yes".equals(ret2)) {
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
