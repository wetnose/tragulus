
public class Assign {

    public Assign() {
        super();
    }

    public static void main(String[] args) {
        int x = 0;
        {
            int self0 = (int)2;
            int var2;
            get1: {
                {
                    var2 = self0;
                    break get1;
                }
            }
            x += var2;
        }
        {
            int self3 = (int)3;
            int var5;
            get4: {
                {
                    var5 = self3;
                    break get4;
                }
            }
            x *= var5;
        }
        System.out.println(x);
    }
}