package info.fmro.shared.utility;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Only Byte/byte/byte[] are implemented for now
public class ModifyParameterArrayAction<T>
        implements Action {

    private static final Logger logger = LoggerFactory.getLogger(ModifyParameterArrayAction.class);
    private final int parameterIndex, arrayIndex;
    private final T value;

    public ModifyParameterArrayAction(int parameterIndex, int arrayIndex, T value) {
        this.arrayIndex = arrayIndex;
        this.parameterIndex = parameterIndex;
        this.value = value;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("modifies array element");
    }

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    @Override
    public Object invoke(Invocation invocation)
            throws Throwable {
        if (value.getClass().equals(Byte.class)) {
            // System.out.println("byte");
            ((byte[]) invocation.getParameter(parameterIndex))[arrayIndex] = Byte.valueOf(value.toString());
        } else if (value.getClass().equals(byte[].class)) {
            // System.out.println("byte array");
            System.arraycopy(value, 0, invocation.getParameter(parameterIndex), arrayIndex, ((byte[]) value).length);
        } else {
            logger.error("Not supported class: {}", value.getClass());
            // System.out.println(value.getClass());
            // System.out.println(Byte[].class);
            // System.out.println(Byte.class);
            // System.out.println(byte[].class);
            // System.out.println(Byte.TYPE);
        }
        return null;
    }

}
