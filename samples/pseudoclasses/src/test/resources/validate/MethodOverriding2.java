import wn.pseudoclasses.Pseudo;
import wn.pseudoclasses.Wrapper;

@Pseudo
final class MethodOverriding extends Wrapper<String> {

    public String toString() {
        return super.toString();
    }
}