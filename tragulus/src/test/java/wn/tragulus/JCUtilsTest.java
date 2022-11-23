package wn.tragulus;

import com.sun.tools.javac.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Alexander A. Solovioff
 * Date: 23.11.2022
 * Time: 4:40 AM
 */
public class JCUtilsTest {

    @Test
    public void set0() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.set(src, 0, "X");
        Assertions.assertEquals(Arrays.asList("X", "B", "C"), new ArrayList<>(act));
    }


    @Test
    public void set1() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.set(src, 1, "X");
        Assertions.assertEquals(Arrays.asList("A", "X", "C"), new ArrayList<>(act));
    }


    @Test
    public void set3() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            List<String> src = List.of("A", "B", "C");
            JCUtils.set(src, 3, "X");
        });
    }


    @Test
    public void append() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.append(src, Arrays.asList("X", "Y", "Z"));
        Assertions.assertEquals(Arrays.asList("A", "B", "C", "X", "Y", "Z"), new ArrayList<>(act));
    }


    @Test
    public void insertItemAt0() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.insert(src, 0, "X");
        Assertions.assertEquals(Arrays.asList("X", "A", "B", "C"), new ArrayList<>(act));
    }


    @Test
    public void insertItemAt1() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.insert(src, 1, "X");
        Assertions.assertEquals(Arrays.asList("A", "X", "B", "C"), new ArrayList<>(act));
    }


    @Test
    public void insertItemAt3() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.insert(src, 3, "X");
        Assertions.assertEquals(Arrays.asList("A", "B", "C", "X"), new ArrayList<>(act));
    }


    @Test
    public void insertItemAt4() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            List<String> src = List.of("A", "B", "C");
            JCUtils.insert(src, 4, "X");
        });
    }


    @Test
    public void insertCollectionAt0() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.insert(src, 0, Arrays.asList("X", "Y", "Z"));
        Assertions.assertEquals(Arrays.asList("X", "Y", "Z", "A", "B", "C"), new ArrayList<>(act));
    }


    @Test
    public void insertCollectionAt1() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.insert(src, 1, Arrays.asList("X", "Y", "Z"));
        Assertions.assertEquals(Arrays.asList("A", "X", "Y", "Z", "B", "C"), new ArrayList<>(act));
    }


    @Test
    public void insertCollectionAt3() {
        List<String> src = List.of("A", "B", "C");
        List<String> act = JCUtils.insert(src, 3, Arrays.asList("X", "Y", "Z"));
        Assertions.assertEquals(Arrays.asList("A", "B", "C", "X", "Y", "Z"), new ArrayList<>(act));
    }


    @Test
    public void insertCollectionAt4() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            List<String> src = List.of("A", "B", "C");
            JCUtils.insert(src, 4, Arrays.asList("X", "Y", "Z"));
        });
    }
}
