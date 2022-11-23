import wn.pseudoclasses.Pseudo;

@Pseudo
final class BadWrapperConstructor2 extends String {

    BadWrapperConstructor2(String a, String b) {
        System.out.println(a + b);
    }
}