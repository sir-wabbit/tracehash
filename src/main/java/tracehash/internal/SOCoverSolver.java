package tracehash.internal;

import java.util.Comparator;

public class SOCoverSolver {
    public static class Result {
        int suffixLength;
        int fragmentLength;
        int coverLength;
    }

    /**
     * Allocation-less. Complexity = O(F^3 + F^2 * (S / F) * F) = O(S F^2), where F is the maxFragmentLength.
     * @param stack
     * @param maxFragmentLength
     * @param minFragmentCount
     * @param result
     */
    public static <A> void solve(A[] stack, int maxFragmentLength, int minFragmentCount, Result result) {
        assert stack != null : "stack can not be null";
        assert result != null : "callback can not be null";
        assert maxFragmentLength > 0 : "maxFragmentLength must be positive";
        assert minFragmentCount > 0 : "minFragmentCount must be positive";

        int resultCoverLength = 0;
        int resultFragmentLength = 0;
        int resultSuffixLength = 0;

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

                if (!ArrayUtil.equalRange(stack, suffixStart, initialFragmentStart, suffixLength))
                    continue;

                // Now that we know that the fragment is good, we compute its coverage.
                int coverage = suffixLength + fragmentLength;
                int count = 1;
                while (coverage + fragmentLength <= stack.length) {
                    // We can potentially fit one more fragment.

                    int fragmentStart = stack.length - coverage - fragmentLength;

                    if (ArrayUtil.equalRange(stack, initialFragmentStart, fragmentStart, fragmentLength)) {
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
            if (resultCoverLength < bestCoverage) {
                resultCoverLength = bestCoverage;
                resultFragmentLength = bestFragmentLength;
                resultSuffixLength = suffixLength;
            } else if (resultCoverLength == bestCoverage) {
                if (resultFragmentLength > bestFragmentLength) {
                    resultFragmentLength = bestFragmentLength;
                    resultSuffixLength = suffixLength;
                } else if (resultFragmentLength == bestFragmentLength) {
                    if (resultSuffixLength > suffixLength) {
                        resultSuffixLength = suffixLength;
                    }
                }
            }
        }

        assert resultSuffixLength <= resultFragmentLength;
        assert resultSuffixLength + resultFragmentLength <= resultCoverLength;

        result.suffixLength = resultSuffixLength;
        result.fragmentLength = resultFragmentLength;
        result.coverLength = resultCoverLength;
    }

    /**
     * Allocation-less. Complexity is O(F).
     * @param stack
     * @param fragmentLength
     * @return
     */
    public static <A> int findRepresentativeFragment(A[] stack, int fragmentLength, Comparator<A> comparator) {
        assert stack != null : "stack can not be null";
        assert 2 * fragmentLength <= stack.length : "at least two fragments must fit into stack";

        int best = stack.length - fragmentLength;
        for (int i = 1; i < fragmentLength; i++) {
            int candidate = stack.length - fragmentLength - i;
            int r = ArrayUtil.compareRange(stack, candidate, best, fragmentLength, comparator);
            if (r < 0) best = candidate;
        }

        return best;
    }
}
