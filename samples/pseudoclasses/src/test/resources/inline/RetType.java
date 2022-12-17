public class RetType {

    public static void main(String[] args) {
        IntAnatomy0 an = wrap(15);
        System.out.println(an);
    }

    static IntAnatomy0 wrap(int val) {
        return (IntAnatomy0) val;
    }
}