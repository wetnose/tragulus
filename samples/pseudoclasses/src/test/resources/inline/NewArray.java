import java.util.Arrays;

public class NewArray {

    public static void main(String[] args) {
        IntAnatomy0[] x;
        x = new IntAnatomy0[] {(IntAnatomy0) 1, (IntAnatomy0) 2};
        IntAnatomy0[][] y = {{(IntAnatomy0) 1}, {(IntAnatomy0) 2}};
        IntAnatomy0[] z[] = new IntAnatomy0[2][3];
        System.out.println(Arrays.toString(x));
        System.out.println(Arrays.deepToString(y));
        System.out.println(Arrays.deepToString(z));
    }
}