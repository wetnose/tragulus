import java.util.function.Supplier;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptForLoop {

    public static void main(String[] args) {
        Holder holder1 = new Holder();
        for (int i=0; i < opt(holder1.counter.next(), 0) && i < 10; i++) {
            Counter counter = holder1.counter;
            counter.count = Math.min(2, counter.count);
        }
        if (holder1.counter.count != 3) {
            System.exit(1);
        }
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
