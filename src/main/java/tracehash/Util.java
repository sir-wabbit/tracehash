package tracehash;

class Util {
    static class MutableCover {
        public int suffixLength;
        public int fragmentLength;
        public int coverLength;
    }

    // Complexity = O(F)
    public static boolean equalRange(StackTraceElement[] array, int start1, int start2, int length) {
        if (array == null) throw new IllegalArgumentException("array can not be null");
        if (0 > start1) throw new IllegalArgumentException("start1 must be non-negative");
        if (0 > start2) throw new IllegalArgumentException("start2 must be non-negative");
        if (start1 + length > array.length) throw new IllegalArgumentException("length is too high");
        if (start2 + length > array.length) throw new IllegalArgumentException("length is too high");

        for (int i = 0; i < length; i++) {
            StackTraceElement s1 = array[start1 + i];
            StackTraceElement s2 = array[start2 + i];

            if (s1 == null) {
                if (s2 != null) return false;
            } else if (!s1.equals(s2)) return false;
        }
        return true;
    }

    public static int compareStrings(String a, String b) {
        if (a == null) {
            if (b != null) return -1;
        } else {
            if (b == null) return 1;
            int r = a.compareTo(b);
            if (r != 0) return r;
        }
        return 0;
    }

    // Complexity = O(F)
    public static int compareRange(StackTraceElement[] array, int start1, int start2, int length) {
        if (array == null) throw new IllegalArgumentException("array can not be null");
        if (0 > start1) throw new IllegalArgumentException("start1 must be non-negative");
        if (0 > start2) throw new IllegalArgumentException("start2 must be non-negative");
        if (start1 + length > array.length) throw new IllegalArgumentException("length is too high");
        if (start2 + length > array.length) throw new IllegalArgumentException("length is too high");

        for (int i = 0; i < length; i++) {
            StackTraceElement s1 = array[start1 + i];
            StackTraceElement s2 = array[start2 + i];

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
        }
        return 0;
    }

    // Complexity = O(F^3 + F^2 * (S / F) * F) = O(S F^2)
    public static void bestCover(StackTraceElement[] stack, int maxFragmentLength, int minFragmentCount, MutableCover result) {
        if (stack == null) throw new IllegalArgumentException("stack can not be null");
        if (result == null) throw new IllegalArgumentException("result can not be null");
        if (maxFragmentLength <= 0) throw new IllegalArgumentException("maxFragmentLength must be positive");
        if (minFragmentCount <= 0) throw new IllegalArgumentException("minFragmentCount must be positive");

        result.coverLength = 0;
        result.fragmentLength = 0;
        result.suffixLength = 0;

        for (int suffixLength = 1; suffixLength <= maxFragmentLength; suffixLength++) {
            if (2 * suffixLength > stack.length)
                break;

            // [ 0 .. suffixLength-1]

            int bestCoverage = 0;
            int bestFragmentLength = 0;

            for (int fragmentLength = suffixLength; fragmentLength <= maxFragmentLength; fragmentLength++) {
                if (fragmentLength + suffixLength > stack.length)
                    break;

                // [0 .. suffixLength-1] [suffixLength .. suffixLength+fragmentLength-1]
                // We verify that the candidate fragment ends with the suffix.
                // First element of the suffix within the fragment has index
                // suffixLength+fragmentLength-1 - (suffixLength-1) = fragmentLength

                int suffixStart = stack.length - suffixLength;
                int initialFragmentStart = stack.length - suffixLength - fragmentLength;

                if (!equalRange(stack, suffixStart, initialFragmentStart, suffixLength))
                    continue;

                // Now that we know that the fragment is good, we compute its coverage.
                int coverage = suffixLength + fragmentLength;
                int count = 1;
                while (coverage + fragmentLength <= stack.length) {
                    // We can potentially fit one more fragment.

                    int fragmentStart = stack.length - coverage - fragmentLength;

                    if (equalRange(stack, initialFragmentStart, fragmentStart, fragmentLength)) {
                        coverage += fragmentLength;
                        count += 1;
                    } else break;
                }

                if (count < minFragmentCount) continue;

                // Best fragment will have maximum coverage.
                if (bestCoverage < coverage) {
                    bestCoverage = coverage;
                    bestFragmentLength = fragmentLength;
                }
            }

            // Best cover has maximum coverage, minimum fragment length, and minimum suffix length.
            if (result.coverLength < bestCoverage) {
                result.coverLength = bestCoverage;
                result.fragmentLength = bestFragmentLength;
                result.suffixLength = suffixLength;
            } else if (result.coverLength == bestCoverage) {
                if (result.fragmentLength > bestFragmentLength) {
                    result.fragmentLength = bestFragmentLength;
                    result.suffixLength = suffixLength;
                } else if (result.fragmentLength == bestFragmentLength) {
                    if (result.suffixLength > suffixLength) {
                        result.suffixLength = suffixLength;
                    }
                }
            }
        }

        assert result.suffixLength <= result.fragmentLength;
        assert result.suffixLength + result.fragmentLength <= result.coverLength;
    }

    public static int sortFragments(StackTraceElement[] stack, int fragmentLength) {
        if (stack == null) throw new IllegalArgumentException("stack can not be null");
        if (2 * fragmentLength > stack.length) throw new IllegalArgumentException("at least two fragments must fit into stack");

        int bestIndex = 0;
        for (int i = 1; i < fragmentLength; i++) {
            int bestFragmentStart = stack.length - fragmentLength - bestIndex;
            int candidateFragmentStart = stack.length - fragmentLength - i;
            int r = compareRange(stack, candidateFragmentStart, bestFragmentStart, fragmentLength);
            if (r < 0) bestIndex = i;
        }
        return bestIndex;
    }
}
