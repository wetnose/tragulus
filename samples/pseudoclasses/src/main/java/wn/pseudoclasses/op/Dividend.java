package wn.pseudoclasses.op;

import wn.pseudoclasses.Pseudo;

/**
 * Alexander A. Solovioff
 * Date: 18.11.2022
 * Time: 4:17 AM
 */
@Pseudo
public interface Dividend<Divisor,Quotient> {
    Quotient div(Divisor divisor);
}
