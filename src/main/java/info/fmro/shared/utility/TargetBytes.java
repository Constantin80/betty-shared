package info.fmro.shared.utility;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

@SuppressWarnings("ClassOnlyUsedInOneModule")
class TargetBytes
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 8094499526857901587L;
    private final byte[] IPBytes = new byte[4], portBytes = new byte[2];

    TargetBytes(final String host, final int port)
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
    public boolean equals(final Object obj) { // other.IP/port not synchronized, meaning equals result not guaranteed to be correct, but behaviour is acceptable
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TargetBytes that = (TargetBytes) obj;
        return Arrays.equals(this.IPBytes, that.IPBytes) &&
               Arrays.equals(this.portBytes, that.portBytes);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(this.IPBytes);
        result = 31 * result + Arrays.hashCode(this.portBytes);
        return result;
    }

    @Override
    public String toString() {
        return "IP=" + (this.IPBytes[0] & 0xFF) + '.' + (this.IPBytes[1] & 0xFF) + '.' + (this.IPBytes[2] & 0xFF) + '.' + (this.IPBytes[3] & 0xFF) + " port=" + (((this.portBytes[0] & 0xFF) << 8) + (this.portBytes[1] & 0xFF)); // * 256
    }
}
