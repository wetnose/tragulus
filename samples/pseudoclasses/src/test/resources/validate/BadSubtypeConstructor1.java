import wn.pseudoclasses.Pseudo;

@Pseudo
final class BadSubtypeConstructor1 extends String {

    BadSubtypeConstructor1(String s) {
        System.out.println(s);
    }
}