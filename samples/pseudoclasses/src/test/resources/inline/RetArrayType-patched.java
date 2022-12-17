
public class RetArrayType {

    public RetArrayType() {
        super();
    }

    public static void main(String[] args) {
        int[] an = wrap(15);
        System.out.println(Arrays.toString(an));
    }

    static int[] wrap(int val) {
        return new int[]{val};
    }
}