import java.util.function.Supplier;

import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptEnclosing {

    public static void main(String[] args) {
        Account acc = new Account();

        String name1 = opt(acc.user).name;
        String name2 = process(() -> opt(acc.user()).repeat(opt(acc.user).name));

        System.out.println("name1 = " + name1);
        System.out.println("name2 = " + name2);

        acc.user = new User();
        acc.user.name = "xxx2";

        String name3 = opt(acc.user).name;
        String name4 = process(() -> opt(acc.user()).repeat(opt(acc.user).name));

        System.out.println("name3 = " + name3);
        System.out.println("name4 = " + name4);

        if (name1 != null || name2 != null || name3 == null || name4 == null) {
            System.exit(2);
        }
    }


    static <T> T process(Supplier<T> supplier) {
        System.out.println(supplier.get());
        return supplier.get();
    }


    static class Base {
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
        String repeat(String arg) {
            System.out.println("repeat " + arg);
            return arg;
        }
    }

    static class Privileges {
    }
}
