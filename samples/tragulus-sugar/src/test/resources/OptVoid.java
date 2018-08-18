import static wn.tragulus.JavaSugar.opt;


/**
 * Created by Alexander A. Solovioff
 * 28.07.2018
 */
public class OptVoid {

    static final Account acc = new Account();
    static int completeCalled;


    public static void main(String[] args) {

        opt(acc.user).complete();

        acc.user = new User();

        opt(acc.user).complete();

        System.out.println("completeCalled = " + completeCalled);

        if (completeCalled != 1) {
            System.exit(2);
        }
    }


    static class Account{
        User user;
    }


    static class User {
        void complete() {
            System.out.println("complete");
            completeCalled++;
        }
    }
}
