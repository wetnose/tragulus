package wn.pseudoclasses;

/**
 * Alexander A. Solovioff
 * Date: 27.11.2022
 * Time: 12:39 AM
 */
class List<L extends List<L>> {

    // linked ring node
    private List<L> prev = this;
    private List<L> next = this;


    L next() {
        //noinspection unchecked
        return (L) next;
    }


    L add(List<L> list) {
        List<L> last1 = this.prev; assert last1.next == this;
        List<L> last2 = list.prev; assert last2.next == list;
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
        List<L> next = this.next;
        prev.next = next;
        next.prev = prev;
        this.next = this;
        this.prev = this;
        //noinspection unchecked
        return (L) next;
    }
}
