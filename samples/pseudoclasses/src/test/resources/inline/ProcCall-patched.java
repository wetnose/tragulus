
public class ProcCall {

    public ProcCall() {
        super();
    }

    public static void main(String[] args) {
        int x = Integer.decode("11");
        int an = (args.length + 15);
        {
            int var1;
            getByte0: {
                System.out.printf("getByte(%d)%n", 2);
                {
                    var1 = an >> 16 & 255;
                    break getByte0;
                }
            }
            int var2 = var1 & 3;
            int var3 = x;
            setByte4: {
                int shift5 = var2 << 3;
                an &= ~(255 << shift5);
                an |= (var3 & 255) << shift5;
            }
        }
        {
            int var7;
            getByte6: {
                System.out.printf("getByte(%d)%n", 0);
                {
                    var7 = an >> 0 & 255;
                    break getByte6;
                }
            }
            setByteIdx8: {
                ;
                an &= 16777215;
                an |= (var7 & 255) << 24;
            }
        }
        System.out.printf("%h%n", an);
        ProcCall pc = new ProcCall();
        System.out.printf("%h%n", pc.inc1(4112));
        System.out.printf("%h%n", pc.get1(1193046));
        pc.set1(1);
        pc.set2(86);
    }

    int inc1(int val) {
        incByte9: {
            {
                ;
                ;
                int var210;
                getByte1: {
                    System.out.printf("getByte(%d)%n", 1);
                    {
                        var210 = val >> 8 & 255;
                        break getByte1;
                    }
                }
                int var411 = var210 + 1;
                setByte5: {
                    ;
                    val &= -65281;
                    val |= (var411 & 255) << 8;
                }
            }
        }
        return val;
    }

    int get1(int val) {
        {
            int var13;
            getByte12: {
                System.out.printf("getByte(%d)%n", 1);
                {
                    var13 = val >> 8 & 255;
                    break getByte12;
                }
            }
            return var13;
        }
    }

    void set1(int val) {
        int x = val * 3;
        {
            int var14 = x;
            int var15 = x = 2;
            setByte16: {
                int shift17 = var14 << 3;
                val &= ~(255 << shift17);
                val |= (var15 & 255) << shift17;
            }
        }
        System.out.println(x);
        System.out.printf("%h%n", val);
    }

    void set2(int val) {
        int y;
        int x;
        int z;
        {
            int var19;
            getByte18: {
                System.out.printf("getByte(%d)%n", 0);
                {
                    var19 = val >> 0 & 255;
                    break getByte18;
                }
            }
            z = var19;
            int var21;
            getByte20: {
                System.out.printf("getByte(%d)%n", 2);
                {
                    var21 = z >> 16 & 255;
                    break getByte20;
                }
            }
            setByte22: {
                ;
                z &= -65281;
                z |= (var21 & 255) << 8;
            }
        }
        System.out.printf("%h%n", z);
    }
}