import wn.pseudoclasses.Pseudo;
import wn.pseudoclasses.Wrapper;

import java.nio.charset.StandardCharsets;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:32 PM
 */
@Pseudo
public final class IntAnatomy0 extends Wrapper<int> {

//    int get() {
//        return value;
//    }
//
//    int loWord() {
//        return value & 0xffff;
//    }
//
//    int hiWord() {
//        return value >>> 16;
//    }

    int getByte(int index) {
        System.out.printf("index = " + index++);
        return this.value >> (index << 3) & 0xff;
    }

    void setByte(int index, int value) {
        int shift = index << 3;
        this.value &= ~(0xff << shift);
        this.value |= (value & 0xff) << shift;
    }

//    int sumBytes() {
//        int s = (get() >> 8 & 0xff00ff) + (get() & 0xff0ff);
//        return (s >>> 16) + (s & 0xffff);
//    }

//    @Override
//    default String toString() {
//        byte[] s = new byte[8];
//        for (int i=0, n=get(); i < 8; i++, n >>= 4) {
//            int z = n & 0xf, a = z - 10;
//            s[i] = (byte) (a < 0 ? '0' + z : 'a' + a);
//        }
//        return new String(s, StandardCharsets.US_ASCII);
//    }
}
