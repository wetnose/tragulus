import wn.pseudoclasses.Pseudo;
import wn.pseudoclasses.Wrapper;

@Pseudo
final class GoodWrapperConstructor extends Wrapper<String> {

    GoodWrapperConstructor(String s) {
        System.out.println(s);
    }
}