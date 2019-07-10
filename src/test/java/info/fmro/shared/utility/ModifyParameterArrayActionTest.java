package info.fmro.shared.utility;

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.hamcrest.StringDescription;
import org.jmock.api.Invocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModifyParameterArrayActionTest {
    @Test
    void describeTo() {
        final Description description = new StringDescription();
        final SelfDescribing modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 0, null);
        modifyParameterArrayAction.describeTo(description);

        final String expResult = "modifies array element";
        assertEquals(expResult, description.toString());
    }

    @SuppressWarnings("ImplicitNumericConversion")
    @Test
    void invoke() {
        final byte[] byteArray = {4, 1, 5};
        @SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod") final Invocation invocation = new Invocation(null, null, byteArray);
        ModifyParameterArrayAction<?> modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 1, (byte) 3);

        byte[] expResult = {4, 3, 5};
        modifyParameterArrayAction.invoke(invocation);
        assertArrayEquals(expResult, byteArray);

        modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 1, new byte[]{8, 6});
        expResult = new byte[]{4, 8, 6};
        modifyParameterArrayAction.invoke(invocation);
        assertArrayEquals(expResult, byteArray);
    }
}
