import java.util.function.Supplier;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptNested {

    static int userCall;
    static int privilegesNullCall;
    static int privilegesNameCall;

    public static void main(String[] args) {
        Account acc = new Account();

        Privileges p0 = process(() -> opt(acc.user().privileges(opt(acc.user.name))));
        System.out.println("p0 = " + p0);

        acc.user = new User();

        Privileges p1 = process(() -> opt(acc.user().privileges(opt(acc.user.name))));
        System.out.println("p1 = " + p1);

        acc.user.name = "xxx2";

        Privileges p2 = process(() -> opt(acc.user().privileges(opt(acc.user.name))));
        System.out.println("p2 = " + p2);

        acc.user.name = "yyy1";
        acc.user.privileges = new Privileges();

        Privileges p3 = process(() -> opt(acc.user().privileges(opt(acc.user.name))));
        System.out.println("p3 = " + p3);


        System.out.println("userCall = " + userCall);
        System.out.println("privilegesNullCall = " + privilegesNullCall);
        System.out.println("privilegesNameCall = " + privilegesNameCall);

        if (p0 != null || p1 != null || p2 != null || p3 == null || userCall != 4 || privilegesNullCall != 1 || privilegesNameCall != 2) {
            System.exit(2);
        }
    }


    static <T> T process(Supplier<T> supplier) {
        T ret = supplier.get();
        System.out.println(ret);
        return ret;
    }


    static class Base {

        User boss;
        int user(int index) {
            return -1;
        }
    }

    static class Account extends Base {
        User user;
        User user() {
            System.out.println("get user");
            userCall++;
            return user;
        }
    }


    static class User {
        String name;
        Body body;
        Privileges privileges;
        Privileges privileges() {
            return privileges(null);
        }
        Privileges privileges(String n) {
            System.out.println("get privileges with " + n);
            if (n == null) {
                privilegesNullCall++;
            } else {
                privilegesNameCall++;
            }
            return privileges;
        }
        void complete() { System.out.println("complete"); }
        void complete(Body body) { System.out.println("complete"); }
        class Body {
            int weight;
            int height;
        }
    }

    static class Privileges {
        Access[] accesses;
    }

    static class Access {
        Target target;
        Target target() {
            System.out.println("get target");
            return target;
        }
    }

    static class Target {
        String name;
    }
}
