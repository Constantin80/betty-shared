package info.fmro.shared.utility;

import org.jetbrains.annotations.Contract;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

@SuppressWarnings("ClassOnlyUsedInOneModule")
public class TargetBytes
        implements Serializable {
    private static final long serialVersionUID = 8094499526857901587L;
    private final byte[] IPBytes = new byte[4], portBytes = new byte[2];

    public TargetBytes(final String host, final int port)
            throws java.net.UnknownHostException {
        this.get(host, port);
    }

    public synchronized byte[] getIPBytes() {
        return Arrays.copyOf(this.IPBytes, this.IPBytes.length);
    }

    public synchronized byte[] getPortBytes() {
        return Arrays.copyOf(this.portBytes, this.portBytes.length);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private synchronized void get(final String host, final int port)
            throws java.net.UnknownHostException {
        String IPString = InetAddress.getByName(host).getHostAddress();
        this.IPBytes[0] = (byte) Integer.parseInt(IPString.substring(0, IPString.indexOf('.')));
        IPString = IPString.substring(IPString.indexOf('.') + ".".length());
        this.IPBytes[1] = (byte) Integer.parseInt(IPString.substring(0, IPString.indexOf('.')));
        IPString = IPString.substring(IPString.indexOf('.') + ".".length());
        this.IPBytes[2] = (byte) Integer.parseInt(IPString.substring(0, IPString.indexOf('.')));
        IPString = IPString.substring(IPString.indexOf('.') + ".".length());
        this.IPBytes[3] = (byte) Integer.parseInt(IPString);
        this.portBytes[0] = (byte) (port / 256);
        this.portBytes[1] = (byte) (port - (port / 256 << 8)); // 256 * 256
    }

    @Override
    public synchronized int hashCode() {
        int hash = 3;
        hash = 79 * hash + Arrays.hashCode(this.IPBytes);
        hash = 79 * hash + Arrays.hashCode(this.portBytes);
        return hash;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
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
        if (!Arrays.equals(this.IPBytes, other.IPBytes)) {
            return false;
        }
        return Arrays.equals(this.portBytes, other.portBytes);
    }

    @Override
    public synchronized String toString() {
        return "IP=" + (this.IPBytes[0] & 0xFF) + '.' + (this.IPBytes[1] & 0xFF) + '.' + (this.IPBytes[2] & 0xFF) + '.' + (this.IPBytes[3] & 0xFF) + " port=" + (((this.portBytes[0] & 0xFF) << 8) + (this.portBytes[1] & 0xFF)); // * 256
    }
}
