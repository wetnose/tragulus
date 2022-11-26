package wn.pseudoclasses;

/**
 * Alexander A. Solovioff
 * Date: 27.11.2022
 * Time: 12:39 AM
 */
abstract class Ring<L extends Ring<L>> {

    // linked ring node
    private Ring<L> prev = this;
    private Ring<L> next = this;


    L next() {
        //noinspection unchecked
        return (L) next;
    }


    L add(Ring<L> list) {
        Ring<L> last1 = this.prev; assert last1.next == this;
        Ring<L> last2 = list.prev; assert last2.next == list;
        last1.next = list;
        list .prev = last1;
        this .prev = last2;
        last2.next = this;
        //noinspection unchecked
        return (L) this;
    }


    L remove() {
        if (next == this) {
            assert prev == this;
            return null;
        }
        Ring<L> next = this.next;
        prev.next = next;
        next.prev = prev;
        this.next = this;
        this.prev = this;
        //noinspection unchecked
        return (L) next;
    }

    protected abstract void appendTo(StringBuilder buf);
}
