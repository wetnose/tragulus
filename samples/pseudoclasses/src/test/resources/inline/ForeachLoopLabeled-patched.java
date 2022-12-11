
public class ForeachLoopLabeled {

    public ForeachLoopLabeled() {
        super();
    }

    public static void main(String[] args) {
        lab: for (int an : new int[]{(int)0, (int)1}) {
            int x;
            {
                int var1;
                get0: {
                    {
                        var1 = an;
                        break get0;
                    }
                }
                x = var1;
            }
            System.out.println(x);
            if (x != 0) break lab;
        }
    }
}