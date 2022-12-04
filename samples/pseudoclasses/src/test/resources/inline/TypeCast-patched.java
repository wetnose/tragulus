
public class TypeCast {

    public TypeCast() {
        super();
    }

    public void test1() {
        int an = readInt();
        System.out.println(an);
    }

    static int readInt() {
        return 15;
    }

    public static void main(String[] args) {
        new TypeCast().test1();
    }
}
