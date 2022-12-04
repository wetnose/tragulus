
@Pseudo()
public final class SelfCall extends Wrapper<int> {

    public SelfCall() {
        super();
    }

    void incByte(int index) {
        {
            int var3 = index;
            int var0 = index;
            int var2;
            getByte1: {
                System.out.printf("index = " + var0++);
                {
                    var2 = value >> (var0 << 3) & 255;
                    break getByte1;
                }
            }
            int var4 = var2 + 1;
            setByte5: {
                int shift6 = var3 << 3;
                value &= ~(255 << shift6);
                value |= (var4 & 255) << shift6;
            }
        }
    }

    int getByte(int index) {
        System.out.printf("index = " + index++);
        return this.value >> (index << 3) & 255;
    }

    void setByte(int index, int value) {
        int shift = index << 3;
        this.value &= ~(255 << shift);
        this.value |= (value & 255) << shift;
    }
}
