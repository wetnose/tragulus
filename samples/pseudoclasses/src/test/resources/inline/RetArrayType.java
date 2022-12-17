import java.util.Arrays;

public class RetArrayType {

    public static void main(String[] args) {
        IntAnatomy0[] an = wrap(15);
        System.out.println(Arrays.toString(an));
    }

    static IntAnatomy0[] wrap(int val) {
        return new IntAnatomy0[] {(IntAnatomy0) val};
    }
}