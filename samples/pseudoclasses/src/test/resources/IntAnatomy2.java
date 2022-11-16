import wn.pseudoclasses.Pseudo;
import wn.pseudoclasses.Wrapper;

/**
 * Alexander A. Solovioff
 * Date: 21.04.2021
 * Time: 7:32 PM
 */
@Pseudo
public class IntAnatomy2 extends Wrapper<int> implements IntAnatomy {

    @Override
    public int get() {
        return this.value;
    }

    @Override
    public void set(int value) {
        this.value = value;
    }
}
