import wn.pseudoclasses.Pseudo;
import wn.pseudoclasses.Wrapper;

import java.nio.charset.StandardCharsets;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:32 PM
 */
@Pseudo
public final class IntAnatomy1 extends Wrapper<int> {

    int getBytePrefix(int index) {
        System.out.println("index = " + ++index + "/" + --index);
        return this.value >> (index << 3) & 0xff;
    }

    int getBytePostfix(int index) {
        System.out.println("index = " + index++ + "/" + index--);
        return this.value >> (index << 3) & 0xff;
    }
}
