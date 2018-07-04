package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class URIVarsTest {

    public URIVarsTest() {
    }

  @Test
    void getProtocol()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        String expResult = "http";
        String result = instance.getProtocol();
        assertEquals(expResult, result);
    }

  @Test
    void setProtocol_String()
            throws UnknownHostException {
        String protocol = "https";
        URIVars instance = new URIVars("google.com");
        instance.setProtocol(protocol);
        assertEquals(protocol, instance.getProtocol(), "equals");
        assertTrue(443 == instance.getPort(), "true 443");
    }

  @Test
    void setProtocol_String_boolean()
            throws UnknownHostException {
        String protocol = "https";
        boolean initialize = false;
        URIVars instance = new URIVars("google.com");
        instance.setProtocol(protocol, initialize);
        assertEquals(protocol, instance.getProtocol(), "protocol https false");
        assertEquals(443, instance.getPort(), "port https false");

        // logger.warn is expected
        protocol = "httpsxx";
        initialize = true;
        instance = new URIVars("google.com");
        instance.setProtocol(protocol, initialize);
        assertEquals(protocol, instance.getProtocol(), "protocol httpsxx true");
        assertEquals(80, instance.getPort(), "port httpsxx true");
    }

  @Test
    void getHost()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        String expResult = "google.com";
        String result = instance.getHost();
        assertEquals(expResult, result);
    }

  @Test
    void getPath()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        String expResult = "/";
        String result = instance.getPath();
        assertEquals(expResult, result);
    }

  @Test
    void setPath()
            throws UnknownHostException {
        String newValue = "testPath";
        URIVars instance = new URIVars("google.com");
        instance.setPath(newValue);
        assertEquals(newValue, instance.getPath());
    }

  @Test
    void getPort()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        int expResult = 80;
        int result = instance.getPort();
        assertEquals(expResult, result);
    }

  @Test
    void modify()
            throws UnknownHostException {
        String url = "  '  htTp://testUser:testPass@yahoo.Co.uk:81/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel  \"'  \r\n\r  ";
        boolean initialize = true;
        URIVars instance = new URIVars("google.com");
        String expResult = "http://testUser:testPass@yahoo.co.uk/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel";
        instance.modify(url, initialize);

        assertEquals(expResult, instance.toString(), "complete toString");
        assertEquals(81, instance.getPort(), "complete port");
    }

  @Test
    void newPage()
            throws UnknownHostException {
        String url = "  '  htTp://testUser:testPass@yahoo.Co.uk:81/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel  \"'  \r\n\r  ";
        URIVars instance = new URIVars("google.com");
        String expResult = "http://testUser:testPass@yahoo.co.uk/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel";
        instance.newPage(url);

        assertEquals(expResult, instance.toString(), "complete toString");
        assertEquals(81, instance.getPort(), "complete port");

        url = "/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel  \"'  \r\n\r  ";
        instance = new URIVars("google.com");
        expResult = "http://google.com/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel";
        instance.newPage(url);

        assertEquals(expResult, instance.toString(), "absolute_path toString");
        assertEquals(80, instance.getPort(), "absolute_path port");

        url = "Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel  \"'  \r\n\r  ";
        instance = new URIVars("google.com");
        expResult = "http://google.com/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel";
        instance.newPage(url);

        assertEquals(expResult, instance.toString(), "relative_path toString");
        assertEquals(80, instance.getPort(), "relative_path port");

        url = "#fragmentLabel  \"'  \r\n\r  ";
        instance = new URIVars("google.com");
        expResult = "http://google.com/#fragmentLabel";
        instance.newPage(url);

        assertEquals(expResult, instance.toString(), "label toString");
        assertEquals(80, instance.getPort(), "label port");
    }

  @Test
    void setDefaultPort_boolean()
            throws UnknownHostException {
        boolean initialize = false;
        URIVars instance = new URIVars("google.com");
        instance.setDefaultPort(initialize);
        assertEquals(80, instance.getPort(), "false");

        initialize = true;
        instance = new URIVars("google.com");
        instance.setDefaultPort(initialize);
        assertEquals(80, instance.getPort(), "true");
    }

  @Test
    void setDefaultPort_String_boolean()
            throws UnknownHostException {
        String protocol = "https";
        boolean initialize = false;
        URIVars instance = new URIVars("google.com");
        instance.setDefaultPort(protocol, initialize);
        assertEquals(443, instance.getPort(), "https false");

        protocol = "https";
        initialize = true;
        instance = new URIVars("google.com");
        instance.setDefaultPort(protocol, initialize);
        assertEquals(443, instance.getPort(), "https true");

        // logger.warn is expected
        protocol = "httpsxx";
        initialize = true;
        instance = new URIVars("google.com");
        instance.setDefaultPort(protocol, initialize);
        assertEquals(80, instance.getPort(), "httpsxx true");

        // logger.warn is expected
        protocol = "httpsxx";
        initialize = false;
        instance = new URIVars("google.com");
        instance.setDefaultPort(protocol, initialize);
        assertEquals(80, instance.getPort(), "httpsxx false");
    }

  @Test
    void testToString()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        String expResult = "http://google.com/";
        String result = instance.toString();
        assertEquals(expResult, result);
    }

  @Test
    void testClone()
            throws UnknownHostException, CloneNotSupportedException {
        URIVars instance = new URIVars("google.com");
        instance.setProtocol("https");
        URIVars result = instance.clone();

        assertEquals(instance.getPort(), result.getPort(), "port");
        assertEquals(instance.getProtocol(), result.getProtocol(), "protocol");
        assertEquals(instance.getPath(), result.getPath(), "path");
        assertEquals(instance.getHost(), result.getHost(), "host");
    }
}
