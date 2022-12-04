
public class PrefixIncDec {

    public PrefixIncDec() {
        super();
    }

    public static void main(String[] args) {
        {
            int self0 = (int)74565;
            int var2;
            getBytePrefix1: {
                System.out.println("index = " + 2 + "/" + 1);
                {
                    var2 = self0 >> 8 & 255;
                    break getBytePrefix1;
                }
            }
            System.out.print(var2);
        }
    }
}