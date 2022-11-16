import java.util.function.Supplier;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptSecondArg {

    public static void main(String[] args) {
        Holder holder = new Holder();
        int c = process(holder.counter.next(), opt(holder.counter.next(), 0));
        if (c != 12) {
            System.exit(2);
        }
    }


    static int process(int a, int b) {
        System.out.println(a + ": " + b);
        return a * 10 + b;
    }


    static class Counter {
        int count;
        int next() {
            return ++count;
        }
    }

    static class Holder {
        Counter counter = new Counter();
    }
}
