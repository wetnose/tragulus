
public class Variable {

    public Variable() {
        super();
    }

    public static void main(String[] args) {
        int an;
        {
            int self0 = (int)1193046;
            int var2;
            getByte1: {
                System.out.printf("getByte(%d)%n", 1);
                {
                    var2 = self0 >> 8 & 255;
                    break getByte1;
                }
            }
            an = var2;
        }
        int x = (int)1;
        {
            int var4;
            get3: {
                {
                    var4 = an;
                    break get3;
                }
            }
            System.out.println(var4);
        }
    }
}