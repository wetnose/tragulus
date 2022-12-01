public class ProcCall {

    public static void main(String[] args) {
        int x = Integer.decode("11");
        IntAnatomy0 an = (IntAnatomy0) (args.length + 15);
        an.setByte(an.getByte(2) & (1 << 1) + 1, x);
        an.setByteIdx(1+2, an.getByte(2));
        System.out.println(an);
    }


    int get1(int val) {
        return ((IntAnatomy0) val).getByte(1);
    }


//    void set1(int val) {
//        int x = val * 3;
//        ((IntAnatomy0) val).setByte(x, x = 2);
//        System.out.println(x);
//    }
}