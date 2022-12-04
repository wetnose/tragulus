
public class PostfixIncDec {

    public PostfixIncDec() {
        super();
    }

    public static void main(String[] args) {
        {
            int self0 = (int)74565;
            int var2;
            getBytePostfix1: {
                System.out.println("index = " + 1 + "/" + 2);
                {
                    var2 = self0 >> 8 & 255;
                    break getBytePostfix1;
                }
            }
            System.out.print(var2);
        }
    }
}