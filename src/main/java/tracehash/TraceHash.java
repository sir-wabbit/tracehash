package tracehash;

import tracehash.internal.KeyStackTraceComponent;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Objects;

public class TraceHash {
    public static Parameters DEFAULT_PARAMETERS = new Parameters(255, 2, 5, false);
    private static String NULL_STRING = "{null}";

    public static class Parameters {
        private final int maxFragmentSize;
        private final int minFragmentCount;
        private final int nonSOESize;
        private final boolean noSynthetic;

        public Parameters(int maxFragmentSize, int minFragmentCount, int nonSOESize, boolean noSynthetic) {
            this.maxFragmentSize = maxFragmentSize;
            this.minFragmentCount = minFragmentCount;
            this.nonSOESize = nonSOESize;
            this.noSynthetic = noSynthetic;
        }
    }

    private static boolean isDefinitelySynthetic(String className, String methodName) {
        if (className == null || methodName == null)
            return false;

        Class<?> cls;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return false;
        }

        final Method[] declaredMethods = cls.getDeclaredMethods();
        if (declaredMethods.length == 0) return false;
        for (Method method : declaredMethods) {
            if (!method.getName().equals(methodName)) continue;
            if (!method.isSynthetic()) return false;
        }
        return true;
    }

    public static String hash(Parameters parameters, Throwable throwable, State state) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(throwable);
        Objects.requireNonNull(state);

        final StackTraceElement[] stackTrace = throwable.getStackTrace();
        final boolean isStackOverflow = throwable instanceof StackOverflowError;

        KeyStackTraceComponent.get(stackTrace, isStackOverflow,
                                   parameters.maxFragmentSize,
                                   parameters.minFragmentCount,
                                   state.keyStackTraceComponent);

        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append(throwable.getClass().getCanonicalName()).append(':');

        int total = 0;
        int maxSize = isStackOverflow ? Integer.MAX_VALUE : parameters.nonSOESize;
        for (int i = 0; i < state.keyStackTraceComponent.length && total < maxSize; i++) {
            int index = state.keyStackTraceComponent.index + i;
            String className = stackTrace[index].getClassName();
            String methodName = stackTrace[index].getMethodName();

            if (parameters.noSynthetic && isDefinitelySynthetic(className, methodName))
                continue;

            if (className == null) className = NULL_STRING;
            if (methodName == null) methodName = NULL_STRING;
            descriptionBuilder.append(className).append('/').append(methodName).append('|');

            total += 1;
        }
        String hash = digest(state.messageDigest, state.charset, descriptionBuilder.toString());

        StringBuilder resultBuilder = new StringBuilder();
        String throwableName = throwable.getClass().getName();
        for (int i = 0; i < throwableName.length(); i++) {
            char ch = throwableName.charAt(i);
            if (Character.isUpperCase(ch)) resultBuilder.append(ch);
        }
        resultBuilder.append('-').append(hash);
        return resultBuilder.toString();
    }

    public static String hash(Parameters parameters, Throwable throwable) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(throwable);

        State state;
        try {
            state = new State();
        } catch (StateInitializationException e) {
            throw new Error(e);
        }
        return hash(parameters, throwable, state);
    }

    public static StackTraceElement[] principal(Parameters parameters, Throwable throwable) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(throwable);

        State state;
        try {
            state = new State();
        } catch (StateInitializationException e) {
            throw new Error(e);
        }

        final StackTraceElement[] stackTrace = throwable.getStackTrace();
        final boolean isStackOverflow = throwable instanceof StackOverflowError;

        KeyStackTraceComponent.get(stackTrace, isStackOverflow,
                                   parameters.maxFragmentSize,
                                   parameters.minFragmentCount,
                                   state.keyStackTraceComponent);

        ArrayList<StackTraceElement> builder = new ArrayList<>();

        int total = 0;
        int maxSize = isStackOverflow ? Integer.MAX_VALUE : parameters.nonSOESize;
        for (int i = 0; i < state.keyStackTraceComponent.length && total < maxSize; i++) {
            int index = state.keyStackTraceComponent.index + i;
            String className = stackTrace[index].getClassName();
            String methodName = stackTrace[index].getMethodName();

            if (parameters.noSynthetic && isDefinitelySynthetic(className, methodName))
                continue;

            builder.add(stackTrace[index]);

            total += 1;
        }

        return builder.toArray(new StackTraceElement[0]);
    }

    private static class StateInitializationException extends Exception {
        public StateInitializationException(Exception cause) {
            super(cause);
        }
    }

    public static class State {
        private final MessageDigest messageDigest;
        private final Charset charset;
        private final KeyStackTraceComponent.State keyStackTraceComponent;

        public State() throws StateInitializationException {
            try {
                messageDigest = MessageDigest.getInstance("SHA-1");
                charset = Charset.forName("UTF-8");
            } catch (NoSuchAlgorithmException | UnsupportedCharsetException e) {
                throw new StateInitializationException(e);
            }

            keyStackTraceComponent = new KeyStackTraceComponent.State();
        }
    }

    private static char hexChar(int x) {
        if (x <= 9) return (char) (x + '0');
        else return (char) ('a' + (x - 10));
    }

    static String digest(MessageDigest md, Charset charset, String str) {
        md.update(str.getBytes(charset));

        byte[] bytes = md.digest();
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(hexChar((b >> 4) & 0xF));
            builder.append(hexChar(b & 0xF));
        }
        return builder.toString();
    }
}
