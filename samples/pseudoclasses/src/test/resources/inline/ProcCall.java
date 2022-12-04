public class ProcCall {

    public static void main(String[] args) {
        int x = Integer.decode("11");
        IntAnatomy0 an = (IntAnatomy0) (args.length + 15);
        an.setByte(an.getByte(2) & (1 << 1) + 1, x);
        an.setByteIdx(1+2, an.getByte(0));
        System.out.printf("%h%n", an);
        ProcCall pc = new ProcCall();
        System.out.printf("%h%n", pc.inc1(0x1010));
        System.out.printf("%h%n", pc.get1(0x123456));
        pc.set1(1);
        pc.set2(0x56);
    }


    int inc1(int val) {
        ((IntAnatomy0) val).incByte(1);
        return val;
    }


    int get1(int val) {
        return ((IntAnatomy0) val).getByte(1);
    }


    void set1(int val) {
        int x = val * 3;
        ((IntAnatomy0) val).setByte(x, x = 2);
        System.out.println(x);
        System.out.printf("%h%n", val);
    }


    void set2(int val) {
        int y;
        IntAnatomy0 x, z;
        (z = (IntAnatomy0) ((IntAnatomy0) val).getByte(0)).setByte(1, z.getByte(2));
        System.out.printf("%h%n", z);
    }
}