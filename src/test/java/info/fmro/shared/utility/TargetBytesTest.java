package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TargetBytesTest {
    public TargetBytesTest() {
    }

  @Test
    void getIP()
            throws UnknownHostException {
        TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        byte[] expResult = new byte[]{-64, -88, 0, 1};
        byte[] result = instance.getIP();
        assertArrayEquals(expResult, result);
    }

  @Test
    void getPort()
            throws UnknownHostException {
        TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        byte[] expResult = new byte[]{4, 56};
        byte[] result = instance.getPort();
        assertArrayEquals(expResult, result);
    }

  @Test
    void toStringTest()
            throws UnknownHostException {
        TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        String expResult = "IP=192.168.0.1 port=1080";
        String result = instance.toString();
        assertEquals(expResult, result);
    }
}
