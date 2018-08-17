package wn.tragulus;

import com.sun.tools.javac.util.List;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by Alexander A. Solovioff
 * 05.08.2018
 */
class JCUtils {

    public static <S,T extends S> List<T> toJCList(Iterable<? extends S> expr) {
        //noinspection unchecked
        return expr == null ? List.nil() : List.from((Iterable<? extends T>) expr);
    }


    public static <T> List<T> set(List<T> list, int i, T val) {
        java.util.List<T> tmp = new ArrayList<>(list);
        tmp.set(i, val);
        return List.from(tmp);
    }


    public static <T> List<T> append(List<T> list, Collection<? super T> val) {
        java.util.List<T> tmp = new ArrayList<>(list.size() + val.size());
        tmp.addAll(list);
        //noinspection unchecked
        tmp.addAll((Collection<? extends T>) val);
        return List.from(tmp);
    }


    public static <S, T extends S> List<T> insert(List<T> list, int i, S val) {
        java.util.List<T> tmp = new ArrayList<>(list);
        //noinspection unchecked
        tmp.add(i, (T) val);
        return List.from(tmp);
    }


    public static <T> List<T> insert(List<T> list, int i, Collection<? super T> val) {
        java.util.List<T> tmp = new ArrayList<>(list);
        //noinspection unchecked
        tmp.addAll(i, (Collection<? extends T>) val);
        return List.from(tmp);
    }


    public static <T> List<T> remove(List<T> list, T val) {
        return List.filter(list, val);
    }
}
