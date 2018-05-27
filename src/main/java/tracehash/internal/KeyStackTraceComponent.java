package tracehash.internal;

public class KeyStackTraceComponent {
    public static void get(StackTraceElement[] stack, boolean stackOverflow, int maxFragmentLength, int minFragmentCount, State result) {
        assert stack != null         : "stack can not be null";
        assert result != null        : "result can not be null";
        assert maxFragmentLength > 0 : "maxFragmentLength must be positive";
        assert minFragmentCount > 0  : "minFragmentCount must be positive";

        if (stackOverflow && getSO(stack, maxFragmentLength, minFragmentCount, result))
            return;

        result.index = 0;
        result.length = stack.length;
    }

    public static class State {
        private SOCoverSolver.Result cover = new SOCoverSolver.Result();

        public int index = 0;
        public int length = 0;
    }

    private static StackTraceElementComparator comparator = new StackTraceElementComparator();

    public static boolean getSO(StackTraceElement[] stack, int maxFragmentLength, int minFragmentCount, State result) {
        assert stack != null         : "stack can not be null";
        assert result != null        : "result can not be null";
        assert maxFragmentLength > 0 : "maxFragmentLength must be positive";
        assert minFragmentCount > 0  : "minFragmentCount must be positive";

        SOCoverSolver.solve(stack, maxFragmentLength, minFragmentCount, result.cover);

        if (result.cover.coverLength >= result.cover.fragmentLength * 2) {
            result.index = SOCoverSolver.findRepresentativeFragment(stack, result.cover.fragmentLength, comparator);
            result.length = result.cover.fragmentLength;
            return true;
        } else return false;
    }
}
