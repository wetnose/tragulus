
public class Param {

    public Param() {
        super();
    }

    public static void main(String[] args) {
        System.out.println(unwrap((int)15));
    }

    static int unwrap(int an) {
        {
            int var1;
            get0: {
                {
                    var1 = an;
                    break get0;
                }
            }
            return var1;
        }
    }
}