public class DoWhileLoop {

    public static void main(String[] args) {
        IntAnatomy0 an = (IntAnatomy0) 0;
        do {
            System.out.println(an.get());
            an.incByte(0);
        } while (an.getByte(0) == 0);
    }
}