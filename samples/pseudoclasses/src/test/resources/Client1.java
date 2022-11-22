public class Client1 {

    public void test1() {
        IntAnatomy0 an = (IntAnatomy0) readInt();
        System.out.println(an);
    }


    static int readInt() {
        return 15;
    }


    public static void main(String[] args) {
        new Client1().test1();
    }
}