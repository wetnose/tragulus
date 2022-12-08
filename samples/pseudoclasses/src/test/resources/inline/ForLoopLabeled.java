public class ForLoopLabeled {

    public static void main(String[] args) {
        label: for (IntAnatomy0 an = (IntAnatomy0) ((IntAnatomy0) 0).get(); an.getByte(0) == 0 ; an.incByte(0)) {
            System.out.println(an.get());
            if (args.length == 1) continue label;
            System.out.println(an.getByte(1));
        }
    }
}