package wn.pseudoclasses.op;

import wn.pseudoclasses.Pseudo;

/**
 * Alexander A. Solovioff
 * Date: 18.11.2022
 * Time: 4:09 AM
 */
@Pseudo
public interface Multiplier<Multiplicand, Product> {

    Product mul(Multiplicand multiplicand);
}
