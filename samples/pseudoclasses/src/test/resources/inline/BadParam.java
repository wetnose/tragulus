public class BadParam {

    public static void main(String[] args) {
        System.out.println(unwrap(15));
    }

    static int unwrap(IntAnatomy0 an) {
        return an.get();
    }
}