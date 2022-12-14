import wn.pseudoclasses.Pseudo;

@Pseudo
final class GoodSubtypeConstructor1 extends String {

    GoodSubtypeConstructor1(String s) {
        System.out.println(s);
    }
}