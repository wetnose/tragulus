public class TypeCast {

    public void test1() {
        IntAnatomy0 an = (IntAnatomy0) readInt();
        System.out.println(an);
    }


    static int readInt() {
        return 15;
    }


    public static void main(String[] args) {
        new TypeCast().test1();
    }
}