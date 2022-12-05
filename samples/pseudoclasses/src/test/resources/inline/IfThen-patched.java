
public class IfThen {

    public IfThen() {
        super();
    }

    public static void main(String[] args) {
        int an = (int)305419896;
        {
            int var1;
            getByte0: {
                System.out.printf("getByte(%d)%n", 2);
                {
                    var1 = an >> 16 & 255;
                    break getByte0;
                }
            }
            if (var1 != 0) {
                setByte2: {
                    ;
                    an &= -65281;
                    an |= 0;
                }
            } else setByte3: {
                ;
                an &= 16777215;
                an |= 0;
            }
        }
        {
            int var5;
            get4: {
                {
                    var5 = an;
                    break get4;
                }
            }
            System.out.printf("%h%n", var5);
        }
    }
}