package tracehash.internal;

import java.util.Comparator;

public class ArrayUtil {
    /**
     * Compares two subarrays for equality.
     * Allocation-less. Complexity is O(F).
     * @param array
     * @param start1
     * @param start2
     * @param length
     * @return
     */
    public static <A> boolean equalRange(A[] array, int start1, int start2, int length) {
        assert array != null : "array can not be null";
        assert 0 <= start1 : "start1 must be non-negative";
        assert 0 <= start2 : "start2 must be non-negative";
        assert start1 + length <= array.length : "length is too high";
        assert start2 + length <= array.length : "length is too high";

        for (int i = 0; i < length; i++) {
            A s1 = array[start1 + i];
            A s2 = array[start2 + i];

            if (s1 == null) {
                if (s2 != null) return false;
            } else if (!s1.equals(s2)) return false;
        }
        return true;
    }

    /**
     * Compares two subarrays.
     * Allocation-less. Complexity is O(F), where F is the length.
     * Result is always either -1, 0, or 1.
     * @param array
     * @param start1
     * @param start2
     * @param length
     * @return
     * @see Comparable#compareTo(Object)
     */
    public static <A> int compareRange(A[] array, int start1, int start2, int length, Comparator<A> comparator) {
        assert array != null : "array can not be null";
        assert 0 <= start1 : "start1 must be non-negative";
        assert 0 <= start2 : "start2 must be non-negative";
        assert start1 + length <= array.length : "length is too high";
        assert start2 + length <= array.length : "length is too high";

        for (int i = 0; i < length; i++) {
            A s1 = array[start1 + i];
            A s2 = array[start2 + i];

            int r = comparator.compare(s1, s2);
            if (r != 0) return r;
        }
        return 0;
    }
}
