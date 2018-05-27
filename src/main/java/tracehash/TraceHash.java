package tracehash;

import tracehash.internal.KeyStackTraceComponent;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TraceHash {
    public static Parameters DEFAULT_PARAMETERS = new Parameters(255, 2, 5);
    private static String NULL_STRING = "{null}";

    public static class Parameters {
        private final int maxFragmentSize;
        private final int minFragmentCount;
        private final int nonSOESize;

        public Parameters(int maxFragmentSize, int minFragmentCount, int nonSOESize) {
            this.maxFragmentSize = maxFragmentSize;
            this.minFragmentCount = minFragmentCount;
            this.nonSOESize = nonSOESize;
        }
    }

    public String hash(Parameters parameters, Throwable throwable, State state) {
        final StackTraceElement[] stackTrace = throwable.getStackTrace();

        KeyStackTraceComponent.get(stackTrace,
                                   throwable instanceof StackOverflowError,
                                   parameters.maxFragmentSize,
                                   parameters.minFragmentCount,
                                   state.keyStackTraceComponent);

        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append(throwable.getClass().getCanonicalName()).append(':');

        for (int i = 0; i < state.keyStackTraceComponent.length; i++) {
            int index = state.keyStackTraceComponent.index + i;
            String className = stackTrace[index].getClassName();
            if (className == null) className = NULL_STRING;
            String methodName = stackTrace[index].getMethodName();
            if (methodName == null) methodName = NULL_STRING;

            descriptionBuilder.append(className).append('/').append(methodName);
            if (i < state.keyStackTraceComponent.length - 1) descriptionBuilder.append('|');
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

    public String hash(Parameters parameters, Throwable throwable) {
        State state;
        try {
            state = new State();
        } catch (StateInitializationException e) {
            throw new Error(e);
        }
        return hash(parameters, throwable);
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

        private byte[] result;

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
