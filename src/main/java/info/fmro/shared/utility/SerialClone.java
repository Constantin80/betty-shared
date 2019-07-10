package info.fmro.shared.utility;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

@SuppressWarnings("UtilityClass")
public final class SerialClone {
    @Contract(pure = true)
    private SerialClone() {
    }

    public static <T extends Serializable> T clone(final T x) {
        try {
            return cloneX(x);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T cloneX(final T x)
            throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        final CloneOutput cOut = new CloneOutput(bOut);
        cOut.writeObject(x);
        final byte[] bytes = bOut.toByteArray();

        final ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        final T clone;
        try (final CloneInput cin = new CloneInput(bin, cOut)) {
            clone = (T) cin.readObject();
        }
        return clone;
    }

    private static class CloneOutput
            extends ObjectOutputStream {
        private final Queue<Class<?>> classQueue = new LinkedList<>();

        CloneOutput(final OutputStream out)
                throws IOException {
            super(out);
        }

        @Override
        protected void annotateClass(final Class<?> cl) {
            this.classQueue.add(cl);
        }

        @Override
        protected void annotateProxyClass(final Class<?> cl) {
            this.classQueue.add(cl);
        }
    }

    private static class CloneInput
            extends ObjectInputStream {
        private final CloneOutput output;

        CloneInput(final InputStream in, final CloneOutput output)
                throws IOException {
            super(in);
            this.output = output;
        }

        @Override
        protected Class<?> resolveClass(@NotNull final ObjectStreamClass desc)
                throws InvalidClassException {
            final Class<?> c = this.output.classQueue.poll();
            final String expected = desc.getName();
            final String found = (c == null) ? null : c.getName();
            if (!expected.equals(found)) {
                throw new InvalidClassException("Classes desynchronized: " + "found " + found + " when expecting " + expected);
            }
            return c;
        }

        @Override
        protected Class<?> resolveProxyClass(@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") final String[] interfaceNames) {
            return this.output.classQueue.poll();
        }
    }
}
