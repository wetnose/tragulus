public class ProcCall {

    public static void main(String[] args) {
        int x = Integer.decode("11");
        IntAnatomy0 an = (IntAnatomy0) (args.length + 15);
        an.setByte(an.getByte(2) & (1 << 1) + 1, x);
        an.setByteIdx(1+2, an.getByte(2));
        System.out.println(an);
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
        System.out.println(val);
    }


//    void set2(int val) {
//        int z, y;
//        IntAnatomy0 x;
//        ((z = ((IntAnatomy0) val).getByte(0)) > 2 ? x : (IntAnatomy0) (y = 2)).setByte(1, z);
//        System.out.println(x);
//    }
}