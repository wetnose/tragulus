public class ProcCall {

    public static void main(String[] args) {
        IntAnatomy0 an = (IntAnatomy0) (args.length + 15);
        an.setByte(an.getByte(2) & (1 << 1) + 1, 1 + 5);
        an.setByte(1+2, an.getByte(2));
        System.out.println(an);
    }
}