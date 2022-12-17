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

    int get() {
        return value;
    }

//    int loWord() {
//        return value & 0xffff;
//    }
//
//    int hiWord() {
//        return value >>> 16;
//    }

    void incByte(int index) {
        setByte(index, getByte(index) + 1);
    }

    int getByte(int index) {
        //System.out.println("index = " + index++);
        System.out.printf("getByte(%d)%n", index);
        return this.value >> (index << 3) & 0xff;
    }

    int getByte2(int index) {
        return this.value >> (index << 3) & 0xff;
    }

    void setByte(int index, int value) {
        int shift = index << 3;
        this.value &= ~(0xff << shift);
        this.value |= (value & 0xff) << shift;
    }

    void setByteIdx(int index, int value) {
        index <<= 3;
        this.value &= ~(0xff << index);
        this.value |= (value & 0xff) << index;
    }

//    int sumBytes() {
//        int s = (get() >> 8 & 0xff00ff) + (get() & 0xff0ff);
//        return (s >>> 16) + (s & 0xffff);
//    }


//    @Pseudo
//    static final class XX extends int {
//
//    }
}
