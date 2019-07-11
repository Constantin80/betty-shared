package info.fmro.shared.utility;

import org.hamcrest.Description;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Only Byte/byte/byte[] are implemented for now
@SuppressWarnings("ClassOnlyUsedInOneModule")
class ModifyParameterArrayAction<T>
        implements Action {
    private static final Logger logger = LoggerFactory.getLogger(ModifyParameterArrayAction.class);
    private final int parameterIndex, arrayIndex;
    private final T value;

    ModifyParameterArrayAction(final int parameterIndex, final int arrayIndex, final T value) {
        this.arrayIndex = arrayIndex;
        this.parameterIndex = parameterIndex;
        this.value = value;
    }

    @Override
    public void describeTo(@NotNull final Description description) {
        description.appendText("modifies array element");
    }

    @Nullable
    @SuppressWarnings("SuspiciousSystemArraycopy")
    @Override
    public Object invoke(final Invocation invocation) {
        if (this.value.getClass().equals(Byte.class)) {
            // System.out.println("byte");
            ((byte[]) invocation.getParameter(this.parameterIndex))[this.arrayIndex] = Byte.valueOf(this.value.toString());
        } else if (this.value.getClass().equals(byte[].class)) {
            // System.out.println("byte array");
            System.arraycopy(this.value, 0, invocation.getParameter(this.parameterIndex), this.arrayIndex, ((byte[]) this.value).length);
        } else {
            logger.error("Not supported class: {}", this.value.getClass());
            // System.out.println(value.getClass());
            // System.out.println(Byte[].class);
            // System.out.println(Byte.class);
            // System.out.println(byte[].class);
            // System.out.println(Byte.TYPE);
        }
        return null;
    }
}
