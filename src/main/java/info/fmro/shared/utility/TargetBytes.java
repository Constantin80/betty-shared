package info.fmro.shared.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

public class TargetBytes
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(TargetBytes.class);
    private static final long serialVersionUID = 8094499526857901587L;
    private final byte[] IP = new byte[4], port = new byte[2];

    public TargetBytes(final String host, final int port)
            throws java.net.UnknownHostException {
        this.get(host, port);
    }

    public TargetBytes() {
    }

    public synchronized byte[] getIP() {
        return Arrays.copyOf(this.IP, this.IP.length);
    }

    public synchronized byte[] getPort() {
        return Arrays.copyOf(this.port, this.port.length);
    }

    private synchronized void get(final String host, final int port)
            throws java.net.UnknownHostException {
        String IPString = InetAddress.getByName(host).getHostAddress();
        this.IP[0] = (byte) Integer.parseInt(IPString.substring(0, IPString.indexOf('.')));
        IPString = IPString.substring(IPString.indexOf('.') + ".".length());
        this.IP[1] = (byte) Integer.parseInt(IPString.substring(0, IPString.indexOf('.')));
        IPString = IPString.substring(IPString.indexOf('.') + ".".length());
        this.IP[2] = (byte) Integer.parseInt(IPString.substring(0, IPString.indexOf('.')));
        IPString = IPString.substring(IPString.indexOf('.') + ".".length());
        this.IP[3] = (byte) Integer.parseInt(IPString);
        this.port[0] = (byte) (port / 256);
        this.port[1] = (byte) (port - port / 256 * 256);
    }

    @Override
    public synchronized int hashCode() {
        int hash = 3;
        hash = 79 * hash + Arrays.hashCode(this.IP);
        hash = 79 * hash + Arrays.hashCode(this.port);
        return hash;
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public synchronized boolean equals(final Object obj) { // other.IP/port not synchronized, meaning equals result not guaranteed to be correct, but behaviour is acceptable
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TargetBytes other = (TargetBytes) obj;
        if (!Arrays.equals(this.IP, other.IP)) {
            return false;
        }
        return Arrays.equals(this.port, other.port);
    }

    @Override
    public synchronized String toString() {
        StringBuilder returnStringBuilder = new StringBuilder("IP=");
        returnStringBuilder.append((int) this.IP[0] & 0xFF).append('.').append((int) this.IP[1] & 0xFF).append('.').append((int) this.IP[2] & 0xFF).append('.').
                append((int) this.IP[3] & 0xFF).append(" port=").append(((int) this.port[0] & 0xFF) * 256 + ((int) this.port[1] & 0xFF));

        return returnStringBuilder.toString();
    }
}
