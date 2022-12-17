
public class RetType {

    public RetType() {
        super();
    }

    public static void main(String[] args) {
        int an = wrap(15);
        System.out.println(an);
    }

    static int wrap(int val) {
        return val;
    }
}