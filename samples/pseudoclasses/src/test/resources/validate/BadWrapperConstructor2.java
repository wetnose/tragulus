import wn.pseudoclasses.Pseudo;
import wn.pseudoclasses.Wrapper;

@Pseudo
final class BadWrapperConstructor2 extends Wrapper<int> {

    BadWrapperConstructor2(String a, String b) {
        System.out.println(a + b);
    }
}