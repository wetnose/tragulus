public class ForLoop {

    public static void main(String[] args) {
        for (IntAnatomy0 an = (IntAnatomy0) ((IntAnatomy0) 0).get(); an.getByte(0) == 0 ; an.incByte(0)) {
            System.out.println(an.get());
            if (args.length == 1) continue;
            System.out.println(an.getByte(1));
        }
    }
}