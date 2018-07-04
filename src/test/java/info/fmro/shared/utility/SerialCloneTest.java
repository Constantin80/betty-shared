package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerialCloneTest {
  @Test
    void cloneTest() {
        String input = "abc";
        Object expResult = "abc";
        Object result = SerialClone.clone(input);
        assertEquals(expResult, result, "first");

        input = "";
        expResult = "";
        result = SerialClone.clone(input);
        assertEquals(expResult, result, "second");

        input = null;
        expResult = null;
        result = SerialClone.clone(input);
        assertEquals(expResult, result, "third");
    }
}
