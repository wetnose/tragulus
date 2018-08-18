import java.util.function.Supplier;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptLambda {

    public static void main(String[] args) {
        Account acc = new Account();

        User user1 = process(() -> opt(acc.user()));
        System.out.println("user1 = " + user1);

        acc.user = new User();
        acc.user.name = "xxx2";

        String name = process(() -> opt(acc.user().name));
        System.out.println("name = " + name);


        Privileges p1 = process(() -> opt(acc.user().privileges));
        System.out.println("p1 = " + p1);

        acc.user.privileges = new Privileges();

        Privileges p2 = process(() -> opt(acc.user().privileges));
        System.out.println("p2 = " + p2);

        if (user1 != null || name == null || p1 != null || p2 == null) {
            System.exit(2);
        }
    }


    static <T> T process(Supplier<T> supplier) {
        System.out.println(supplier.get());
        return supplier.get();
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
