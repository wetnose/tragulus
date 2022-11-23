import wn.pseudoclasses.Pseudo;

public class Local {

    public static void test() {
        @Pseudo
        class Loc extends Integer {
        }
        Loc x;
    }
}