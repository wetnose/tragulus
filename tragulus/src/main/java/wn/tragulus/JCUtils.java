package wn.tragulus;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import java.util.Collection;


/**
 * Created by Alexander A. Solovioff
 * 05.08.2018
 */
public class JCUtils {

    public static <S,T extends S> List<T> toJCList(Iterable<? extends S> expr) {
        //noinspection unchecked
        return expr == null ? List.nil() : List.from((Iterable<? extends T>) expr);
    }


    public static <T> List<T> set(List<T> list, int i, T val) {
        ListBuffer<T> buf = new ListBuffer<>();
        for (T t : list) {
            if (i-- == 0) {
                buf.add(val);
            } else {
                buf.append(t);
            }
        }
        if (i >= 0) {
            throw new IndexOutOfBoundsException();
        }
        return buf.toList();
    }


    public static <T> List<T> append(List<T> list, Collection<? super T> val) {
        ListBuffer<T> buf = new ListBuffer<>();
        buf.appendList(list);
        //noinspection unchecked
        buf.addAll((Collection<? extends T>) val);
        return buf.toList();
    }


    public static <S, T extends S> List<T> insert(List<T> list, int i, S val) {
        ListBuffer<T> buf = new ListBuffer<>();
        for (T t : list) {
            if (i-- == 0) //noinspection unchecked
                buf.add((T) val);
            buf.append(t);
        }
        if (i == 0) {
            //noinspection unchecked
            buf.add((T) val);
        } else
        if (i > 0) {
            throw new IndexOutOfBoundsException();
        }
        return buf.toList();
    }


    public static <T> List<T> insert(List<T> list, int i, Collection<? super T> val) {
        ListBuffer<T> buf = new ListBuffer<>();
        for (T t : list) {
            if (i-- == 0) //noinspection unchecked
                buf.addAll((Collection<? extends T>) val);
            buf.append(t);
        }
        if (i == 0) {
            //noinspection unchecked
            buf.addAll((Collection<? extends T>) val);
        } else
        if (i > 0) {
            throw new IndexOutOfBoundsException();
        }
        return buf.toList();
    }


    public static <T> List<T> remove(List<T> list, T val) {
        return List.filter(list, val);
    }
}
