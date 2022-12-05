public class IfThen {

    public static void main(String[] args) {
        IntAnatomy0 an = (IntAnatomy0) 0x12345678;
        if (an.getByte(2) != 0) {
            an.setByte(1, 0x00);
        } else
            an.setByte(3, 0x00);
        System.out.printf("%h%n", an.get());
    }
}