package wn.pseudoclasses.op;

import wn.pseudoclasses.Pseudo;

/**
 * Alexander A. Solovioff
 * Date: 18.11.2022
 * Time: 4:12 AM
 */
@Pseudo
public interface Augend<Addend,Sum> {

    Sum add(Addend addend);
}
