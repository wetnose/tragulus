
public class ForLoopLabeled {

    public ForLoopLabeled() {
        super();
    }

    public static void main(String[] args) {
        int an;
        {
            int self0 = (int)0;
            int var2;
            get1: {
                {
                    var2 = self0;
                    break get1;
                }
            }
            an = var2;
        }
        label: for (; ; incByte5: {
            {
                ;
                ;
                int var26;
                getByte1: {
                    System.out.printf("getByte(%d)%n", 0);
                    {
                        var26 = an >> 0 & 255;
                        break getByte1;
                    }
                }
                int var47 = var26 + 1;
                setByte5: {
                    ;
                    an &= -256;
                    an |= (var47 & 255) << 0;
                }
            }
        }) {
            {
                int var4;
                getByte3: {
                    System.out.printf("getByte(%d)%n", 0);
                    {
                        var4 = an >> 0 & 255;
                        break getByte3;
                    }
                }
                if (var4 == 0) ; else break;
            }
            {
                {
                    int var9;
                    get8: {
                        {
                            var9 = an;
                            break get8;
                        }
                    }
                    System.out.println(var9);
                }
                if (args.length == 1) continue label;
                {
                    int var11;
                    getByte10: {
                        System.out.printf("getByte(%d)%n", 1);
                        {
                            var11 = an >> 8 & 255;
                            break getByte10;
                        }
                    }
                    System.out.println(var11);
                }
            }
        }
    }
}