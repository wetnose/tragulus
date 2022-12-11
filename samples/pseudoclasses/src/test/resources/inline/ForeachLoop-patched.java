
public class ForeachLoop {

    public ForeachLoop() {
        super();
    }

    public static void main(String[] args) {
        for (int an : new int[]{(int)0, (int)1}) {
            {
                int var1;
                get0: {
                    {
                        var1 = an;
                        break get0;
                    }
                }
                System.out.println(var1);
            }
        }
    }
}