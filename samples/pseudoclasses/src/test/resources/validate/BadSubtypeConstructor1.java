import wn.pseudoclasses.Pseudo;

@Pseudo
final class BadSubtypeConstructor extends int {

    BadSubtypeConstructor1(String s) {
        System.out.println(s);
    }
}