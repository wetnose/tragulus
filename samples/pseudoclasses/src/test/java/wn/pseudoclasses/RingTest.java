package wn.pseudoclasses;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Alexander A. Solovioff
 * Date: 27.11.2022
 * Time: 12:43 AM
 */
public class RingTest {


    @Test
    public void addRemove() {
        Entry a = new Entry("a");
        Entry b = new Entry("b");
        Entry c = new Entry("c");
        System.out.println(a);
        System.out.println(b);
        System.out.println(c);

        a.add(b).add(c);
        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        Assertions.assertEquals("a b c", a.toString());
        Assertions.assertEquals("b c a", b.toString());
        Assertions.assertEquals("c a b", c.toString());
        Assertions.assertEquals(b, a.next());

        Assertions.assertEquals(c, b.remove());
        Assertions.assertEquals("a c", a.toString());
        Assertions.assertEquals("c a", c.toString());
        Assertions.assertEquals("b", b.toString());
        Assertions.assertEquals(null, b.remove());

        Assertions.assertEquals(a, c.remove());
        Assertions.assertEquals("a", a.toString());
        Assertions.assertEquals("b", b.toString());
        Assertions.assertEquals("c", c.toString());

        Assertions.assertEquals(null, a.remove());
        Assertions.assertEquals(null, c.remove());
    }


    @Test
    public void concat() {
        Entry a = new Entry("a");
        Entry b = new Entry("b");
        Entry c = new Entry("c");
        Entry d = new Entry("d");

        a.add(b);
        c.add(d);
        System.out.println(a);
        System.out.println(c);
        Assertions.assertEquals("a b", a.toString());
        Assertions.assertEquals("c d", c.toString());

        a.add(c);
        System.out.println(a);
        System.out.println(c);
        Assertions.assertEquals("a b c d", a.toString());
        Assertions.assertEquals("c d a b", c.toString());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Supplementary classes & routines                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static class Entry extends Ring<Entry> {

        final String val;

        Entry(String val) {
            this.val = val;
        }

        @Override
        public String toString() {
            int count = 0;
            StringBuilder buf = new StringBuilder();
            buf.append(val);
            Entry e = next();
            while (e != this) {
                if (++count == 20) {
                    buf.append("...");
                    break;
                }
                buf.append(" ");
                e.appendTo(buf);
                e = e.next();
            }
            return buf.toString();
        }

        @Override
        protected void appendTo(StringBuilder buf) {
            buf.append(val);
        }
    }
}
