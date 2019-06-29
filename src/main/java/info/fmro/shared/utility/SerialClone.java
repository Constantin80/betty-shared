package info.fmro.shared.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

public class SerialClone {

    private SerialClone() {
    }

    public static <T> T clone(final T x) {
        try {
            return cloneX(x);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static <T> T cloneX(final T x)
            throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        CloneOutput cout = new CloneOutput(bout);
        cout.writeObject(x);
        byte[] bytes = bout.toByteArray();

        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        CloneInput cin = new CloneInput(bin, cout);

        @SuppressWarnings("unchecked")
        T clone = (T) cin.readObject();
        return clone;
    }

    private static class CloneOutput
            extends ObjectOutputStream {

        Queue<Class<?>> classQueue = new LinkedList<>();

        CloneOutput(final OutputStream out)
                throws IOException {
            super(out);
        }

        @Override
        protected void annotateClass(final Class<?> c) {
            classQueue.add(c);
        }

        @Override
        protected void annotateProxyClass(final Class<?> c) {
            classQueue.add(c);
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
        protected Class<?> resolveClass(final ObjectStreamClass osc)
                throws IOException, ClassNotFoundException {
            Class<?> c = output.classQueue.poll();
            String expected = osc.getName();
            String found = (c == null) ? null : c.getName();
            if (!expected.equals(found)) {
                throw new InvalidClassException("Classes desynchronized: " +
                        "found " + found + " when expecting " + expected);
            }
            return c;
        }

        @Override
        protected Class<?> resolveProxyClass(final String[] interfaceNames)
                throws IOException, ClassNotFoundException {
            return output.classQueue.poll();
        }
    }
}
