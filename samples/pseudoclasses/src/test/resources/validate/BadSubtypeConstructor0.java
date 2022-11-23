import wn.pseudoclasses.Pseudo;

@Pseudo
final class BadSubtypeConstructor0 extends String {

    BadSubtypeConstructor0(String s) {
        System.out.println(s);
    }
}