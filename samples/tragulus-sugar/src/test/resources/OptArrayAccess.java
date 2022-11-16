import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptArrayAccess {

    static final Account acc = new Account();


    public static void main(String[] args) {
        acc.user = new User();
        acc.user.name = "xxx2";
        acc.user.privileges = new Privileges();

        String name1 = opt(acc.user().privileges.accesses)[0].target.name;

        acc.user.privileges.accesses = new Access[]{new Access()};
        acc.user.privileges.accesses[0].target = new Target();
        acc.user.privileges.accesses[0].target.name = "yyy2";
        String name2 = opt(acc.user().privileges.accesses)[0].target.name;

        System.out.println("name1 = " + name1);
        System.out.println("name2 = " + name2);

        if (name1 != null || !"yyy2".equals(name2)) {
            System.exit(2);
        }
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
