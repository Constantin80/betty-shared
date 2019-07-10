package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TargetBytesTest {
    @SuppressWarnings("ImplicitNumericConversion")
    @Test
    void getIP()
            throws UnknownHostException {
        final TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        final byte[] expResult = {-64, -88, 0, 1};
        final byte[] result = instance.getIPBytes();
        assertArrayEquals(expResult, result);
    }

    @SuppressWarnings("ImplicitNumericConversion")
    @Test
    void getPort()
            throws UnknownHostException {
        final TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        final byte[] expResult = {4, 56};
        final byte[] result = instance.getPortBytes();
        assertArrayEquals(expResult, result);
    }

    @Test
    void toStringTest()
            throws UnknownHostException {
        final TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        final String expResult = "IP=192.168.0.1 port=1080";
        final String result = instance.toString();
        assertEquals(expResult, result);
    }
}
