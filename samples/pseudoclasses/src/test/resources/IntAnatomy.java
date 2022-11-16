package wn.pseudoclasses;

import java.nio.charset.StandardCharsets;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:32 PM
 */
@Pseudo
public interface IntAnatomy extends Template {

    int  get();
    void set(int value);

    default int loWord() {
        return get() & 0xffff;
    }

    default int hiWord() {
        return get() >>> 16;
    }

    default int getByte(int index) {
        return get() >> (index << 3) & 0xff;
    }

    default void setByte(int index, int value) {
        int shift = index << 3;
        set(get() & ~0xff << shift | (value & 0xff) << shift);
    }

    default int sumBytes() {
        int s = (get() >> 8 & 0xff00ff) + (get() & 0xff0ff);
        return (s >>> 16) + (s & 0xffff);
    }

    @Override
    default String toString() {
        byte[] s = new byte[8];
        for (int i=0, n=get(); i < 8; i++, n >>= 4) {
            int z = n & 0xf, a = z - 10;
            s[i] = (byte) (a < 0 ? '0' + z : 'a' + a);
        }
        return new String(s, StandardCharsets.US_ASCII);
    }
}
