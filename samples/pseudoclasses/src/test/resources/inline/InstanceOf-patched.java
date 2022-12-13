
public class InstanceOf {

    public InstanceOf() {
        super();
    }

    public static void main(String[] args) {
        {
            int self0 = (int)15;
            int var2;
            get1: {
                {
                    var2 = self0;
                    break get1;
                }
            }
            if (Integer.valueOf(var2) instanceof Integer) {
                System.out.println("ok");
            }
        }
    }
}