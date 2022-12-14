
public class Unary {

    public Unary() {
        super();
    }

    public static void main(String[] args) {
        int an = (int)15;
        {
            int var1;
            get0: {
                {
                    var1 = an;
                    break get0;
                }
            }
            System.out.println(-var1);
        }
    }
}