
public class NewArray {

    public NewArray() {
        super();
    }

    public static void main(String[] args) {
        int[] x;
        x = new int[]{(int)1, (int)2};
        int[][] y = {{(int)1}, {(int)2}};
        int[][] z = new int[2][3];
        System.out.println(Arrays.toString(x));
        System.out.println(Arrays.deepToString(y));
        System.out.println(Arrays.deepToString(z));
    }
}