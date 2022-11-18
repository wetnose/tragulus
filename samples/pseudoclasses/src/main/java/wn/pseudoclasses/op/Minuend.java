package wn.pseudoclasses.op;

import wn.pseudoclasses.Pseudo;

/**
 * Alexander A. Solovioff
 * Date: 18.11.2022
 * Time: 4:28 AM
 */
@Pseudo
public interface Minuend<Subtrahend,Difference> {

    Difference sub(Subtrahend subtrahend);
}
