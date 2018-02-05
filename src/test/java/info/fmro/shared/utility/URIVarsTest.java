package info.fmro.shared.utility;

import java.net.UnknownHostException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URIVarsTest {

    private static final Logger logger = LoggerFactory.getLogger(URIVarsTest.class);

    public URIVarsTest() {
    }
    @Rule
    @SuppressWarnings("PublicField")
    public TestRule watchman = new TestWatcher() {
        @Override
        public void starting(Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };

    @Test
    public void testGetProtocol()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        String expResult = "http";
        String result = instance.getProtocol();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetProtocol_String()
            throws UnknownHostException {
        String protocol = "https";
        URIVars instance = new URIVars("google.com");
        instance.setProtocol(protocol);
        assertEquals(protocol, instance.getProtocol());
        assertTrue(443 == instance.getPort());
    }

    @Test
    public void testSetProtocol_String_boolean()
            throws UnknownHostException {
        String protocol = "https";
        boolean initialize = false;
        URIVars instance = new URIVars("google.com");
        instance.setProtocol(protocol, initialize);
        assertEquals("protocol https false", protocol, instance.getProtocol());
        assertEquals("port https false", 443, instance.getPort());

        // logger.warn is expected
        protocol = "httpsxx";
        initialize = true;
        instance = new URIVars("google.com");
        instance.setProtocol(protocol, initialize);
        assertEquals("protocol httpsxx true", protocol, instance.getProtocol());
        assertEquals("port httpsxx true", 80, instance.getPort());
    }

    @Test
    public void testGetHost()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        String expResult = "google.com";
        String result = instance.getHost();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPath()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        String expResult = "/";
        String result = instance.getPath();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetPath()
            throws UnknownHostException {
        String newValue = "testPath";
        URIVars instance = new URIVars("google.com");
        instance.setPath(newValue);
        assertEquals(newValue, instance.getPath());
    }

    @Test
    public void testGetPort()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        int expResult = 80;
        int result = instance.getPort();
        assertEquals(expResult, result);
    }

    @Test
    public void testModify()
            throws UnknownHostException {
        String url = "  '  htTp://testUser:testPass@yahoo.Co.uk:81/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel  \"'  \r\n\r  ";
        boolean initialize = true;
        URIVars instance = new URIVars("google.com");
        String expResult = "http://testUser:testPass@yahoo.co.uk/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel";
        instance.modify(url, initialize);

        assertEquals("complete toString", expResult, instance.toString());
        assertEquals("complete port", 81, instance.getPort());
    }

    @Test
    public void testNewPage()
            throws UnknownHostException {
        String url = "  '  htTp://testUser:testPass@yahoo.Co.uk:81/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel  \"'  \r\n\r  ";
        URIVars instance = new URIVars("google.com");
        String expResult = "http://testUser:testPass@yahoo.co.uk/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel";
        instance.newPage(url);

        assertEquals("complete toString", expResult, instance.toString());
        assertEquals("complete port", 81, instance.getPort());

        url = "/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel  \"'  \r\n\r  ";
        instance = new URIVars("google.com");
        expResult = "http://google.com/Path/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel";
        instance.newPage(url);

        assertEquals("absolute_path toString", expResult, instance.toString());
        assertEquals("absolute_path port", 80, instance.getPort());

        url = "Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel  \"'  \r\n\r  ";
        instance = new URIVars("google.com");
        expResult = "http://google.com/Path2/Path3/File.html?query=1&query2=333&query3=#fragmentLabel";
        instance.newPage(url);

        assertEquals("relative_path toString", expResult, instance.toString());
        assertEquals("relative_path port", 80, instance.getPort());

        url = "#fragmentLabel  \"'  \r\n\r  ";
        instance = new URIVars("google.com");
        expResult = "http://google.com/#fragmentLabel";
        instance.newPage(url);

        assertEquals("label toString", expResult, instance.toString());
        assertEquals("label port", 80, instance.getPort());
    }

    @Test
    public void testSetDefaultPort_boolean()
            throws UnknownHostException {
        boolean initialize = false;
        URIVars instance = new URIVars("google.com");
        instance.setDefaultPort(initialize);
        assertEquals("false", 80, instance.getPort());

        initialize = true;
        instance = new URIVars("google.com");
        instance.setDefaultPort(initialize);
        assertEquals("true", 80, instance.getPort());
    }

    @Test
    public void testSetDefaultPort_String_boolean()
            throws UnknownHostException {
        String protocol = "https";
        boolean initialize = false;
        URIVars instance = new URIVars("google.com");
        instance.setDefaultPort(protocol, initialize);
        assertEquals("https false", 443, instance.getPort());

        protocol = "https";
        initialize = true;
        instance = new URIVars("google.com");
        instance.setDefaultPort(protocol, initialize);
        assertEquals("https true", 443, instance.getPort());

        // logger.warn is expected
        protocol = "httpsxx";
        initialize = true;
        instance = new URIVars("google.com");
        instance.setDefaultPort(protocol, initialize);
        assertEquals("httpsxx true", 80, instance.getPort());

        // logger.warn is expected
        protocol = "httpsxx";
        initialize = false;
        instance = new URIVars("google.com");
        instance.setDefaultPort(protocol, initialize);
        assertEquals("httpsxx false", 80, instance.getPort());
    }

    @Test
    public void testToString()
            throws UnknownHostException {
        URIVars instance = new URIVars("google.com");
        String expResult = "http://google.com/";
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    @Test
    public void testClone()
            throws UnknownHostException, CloneNotSupportedException {
        URIVars instance = new URIVars("google.com");
        instance.setProtocol("https");
        URIVars result = instance.clone();

        assertEquals("port", instance.getPort(), result.getPort());
        assertEquals("protocol", instance.getProtocol(), result.getProtocol());
        assertEquals("path", instance.getPath(), result.getPath());
        assertEquals("host", instance.getHost(), result.getHost());
    }
}
