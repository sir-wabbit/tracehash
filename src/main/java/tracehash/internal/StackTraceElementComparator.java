package tracehash.internal;

import java.util.Comparator;

public class StackTraceElementComparator implements Comparator<StackTraceElement> {
    /**
     * Compares two strings using {@link String#compareTo}.
     *
     * Allocation-less. Complexity is O(N), where N is
     * the length of the smallest argument.
     * @param a the first string
     * @param b the second string
     * @return  the result of comparison
     * @see     String#compareTo(String)
     */
    static int compareStrings(String a, String b) {
        if (a == null) {
            if (b != null) return -1;
        } else {
            if (b == null) return 1;
            int r = a.compareTo(b);
            if (r != 0) return r;
        }
        return 0;
    }

    @Override public int compare(StackTraceElement s1, StackTraceElement s2) {
        if (s1 == null) {
            if (s2 != null) return -1;
        } else {
            if (s2 == null) return 1;

            int r = compareStrings(s1.getClassName(), s2.getClassName());
            if (r != 0) return r;

            r = compareStrings(s1.getMethodName(), s2.getMethodName());
            if (r != 0) return r;

            r = compareStrings(s1.getFileName(), s2.getFileName());
            if (r != 0) return r;

            if (s1.getLineNumber() < s2.getLineNumber()) return -1;
            if (s1.getLineNumber() > s2.getLineNumber()) return 1;
        }

        return 0;
    }
}
