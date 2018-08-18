import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptNew {

    static int nobodyCalled;
    static int hasbodyCalled;
    static int created;


    public static void main(String[] args) {

        Account acc = null;
        opt(acc.user1).complete(opt(acc.user2).new Body());

        acc = new Account();
        opt(acc.user1).complete(opt(acc.user2).new Body());

        acc.user1 = new User();
        opt(acc.user1).complete(opt(acc.user2).new Body());

        acc.user2 = new User();
        opt(acc.user1).complete(opt(acc.user2).new Body());

        System.out.println("nobodyCalled = " + nobodyCalled);
        System.out.println("hasbodyCalled = " + hasbodyCalled);
        System.out.println("created = " + created);

        if (nobodyCalled != 1 || hasbodyCalled != 1 || created != 1) {
            System.exit(2);
        }
    }


    static class Account {
        User user1;
        User user2;
    }


    static class User {
        void complete(Body body) {
            System.out.println("complete " + body);
            if (body == null) {
                nobodyCalled++;
            } else {
                hasbodyCalled++;
            }
        }
        class Body {
            { created++; }
        }
    }
}
