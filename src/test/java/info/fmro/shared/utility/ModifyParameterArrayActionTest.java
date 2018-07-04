package info.fmro.shared.utility;

import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.jmock.api.Invocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModifyParameterArrayActionTest {
    public ModifyParameterArrayActionTest() {
    }

    @Test
    void describeTo() {
        Description description = new StringDescription();
        ModifyParameterArrayAction<Object> modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 0, null);
        modifyParameterArrayAction.describeTo(description);

        String expResult = "modifies array element";
        assertEquals(expResult, description.toString());
    }

    @Test
    void invoke()
            throws Throwable {
        byte[] byteArray = new byte[]{4, 1, 5};
        Invocation invocation = new Invocation(null, null, byteArray);
        ModifyParameterArrayAction<?> modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 1, (byte) 3);

        byte[] expResult = new byte[]{4, 3, 5};
        modifyParameterArrayAction.invoke(invocation);
        assertArrayEquals(expResult, byteArray);

        modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 1, new byte[]{8, 6});
        expResult = new byte[]{4, 8, 6};
        modifyParameterArrayAction.invoke(invocation);
        assertArrayEquals(expResult, byteArray);
    }
}
