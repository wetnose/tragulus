import wn.pseudoclasses.Pseudo;
import wn.pseudoclasses.Wrapper;

import java.nio.charset.StandardCharsets;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:32 PM
 */
@Pseudo
public final class SelfCall extends Wrapper<int> {

    void incByte(int index) {
        setByte(index, getByte(index) + 1);
    }

    int getByte(int index) {
        System.out.printf("index = " + index++);
        return this.value >> (index << 3) & 0xff;
    }

    void setByte(int index, int value) {
        int shift = index << 3;
        this.value &= ~(0xff << shift);
        this.value |= (value & 0xff) << shift;
    }
}
