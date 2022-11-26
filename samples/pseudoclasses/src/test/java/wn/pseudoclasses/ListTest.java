package wn.pseudoclasses;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Alexander A. Solovioff
 * Date: 27.11.2022
 * Time: 12:43 AM
 */
public class ListTest {


    @Test
    public void addRemove() {
        Node a = new Node("a");
        Node b = new Node("b");
        Node c = new Node("c");
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
        Node a = new Node("a");
        Node b = new Node("b");
        Node c = new Node("c");
        Node d = new Node("d");

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

    static class Node extends List<Node> {

        final String val;

        Node(String val) {
            this.val = val;
        }

        @Override
        public String toString() {
            int count = 0;
            StringBuilder buf = new StringBuilder();
            buf.append(val);
            Node n = next();
            while (n != this) {
                if (++count == 20) {
                    buf.append("...");
                    break;
                }
                buf.append(" ");
                buf.append(n.val);
                n = n.next();
            }
            return buf.toString();
        }
    }
}
