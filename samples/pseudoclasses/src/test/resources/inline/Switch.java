public class Switch {

    public static void main(String[] args) {
        IntAnatomy0 x = (IntAnatomy0) (args.length + 10);
        switch (((IntAnatomy0) args.length).get()) {
            case 1:
                x.incByte(1);
                System.out.println(x.get());
                break;
            case ((IntAnatomy0) 2).get():
            case ((IntAnatomy0) 3).getByte2(0):
                x.incByte(2);
                System.out.println(x.get() + 1);
                break;
            default:
                System.out.println(x.get());
        }
    }
}