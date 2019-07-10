package info.fmro.shared.utility;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.IDN;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class URIVars
        implements Serializable, Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(URIVars.class);
    private static final long serialVersionUID = 3895525534915953109L;
    // http://en.wikipedia.org/wiki/Uniform_Resource_Locator
    // it will work for http type URLs:
    // scheme://username:password@domain:port/path?query_string#fragment_id
    @SuppressWarnings("FieldNotUsedInToString")
    private int port;
    @Nullable
    private String protocol, username, password, host, path, fragmentId;

    public URIVars(final String url)
            throws java.net.UnknownHostException {
        this.initialize(url);
    }

    @Nullable
    public synchronized String getProtocol() {
        return this.protocol;
    }

    public synchronized void setProtocol(final String protocol) {
        this.setProtocol(protocol, true);
    }

    public synchronized void setProtocol(final String protocol, final boolean initialize) {
        this.protocol = protocol;
        this.setDefaultPort(initialize);
    }

    @Nullable
    public synchronized String getHost() {
        return this.host;
    }

    @Nullable
    public synchronized String getPath() {
        return this.path;
    }

    public synchronized void setPath(@Nullable final String newValue) {
        this.path = newValue;
    }

    public synchronized int getPort() {
        return this.port;
    }

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
    public synchronized URIVars modify(final String argumentUrl, final boolean initialize)
            throws java.net.UnknownHostException {
        String url = argumentUrl;

        if (url != null) {
            final String initialUrl = url; // this remains unchanged
            url = Generic.specialCharParser(url).trim();

            if (url.indexOf('\r') >= 0) {
                url = url.substring(0, url.indexOf('\r'));
            }
            if (url.indexOf('\n') >= 0) {
                url = url.substring(0, url.indexOf('\n'));
            }

            if (url.indexOf('"') == 0 || url.indexOf('\'') == 0) {
                url = url.substring(1);
            }
            if (url.indexOf('"') >= 0) {
                url = url.substring(0, url.indexOf('"'));
            }
            if (url.indexOf('\'') >= 0) {
                url = url.substring(0, url.indexOf('\''));
            }

            url = url.trim();
            if (url.indexOf(' ') >= 0) {
                url = url.substring(0, url.indexOf(' '));
            }

            if (url.contains("://")) {
                this.setProtocol(new String(url.substring(0, url.indexOf("://")).toLowerCase(Locale.ENGLISH)), initialize);
                url = url.substring(url.indexOf("://") + "://".length());
            } else if (initialize) {
                this.setProtocol("http", initialize);
            } else {
                // no protocol present, nothing to be done
            }

            if (url.indexOf('@') >= 0) {
                this.username = new String(url.substring(0, url.indexOf('@')));
                if (this.username.indexOf(':') >= 0) {
                    this.password = new String(this.username.substring(this.username.indexOf(':') + ":".length()));
                    this.username = new String(this.username.substring(0, this.username.indexOf(':')));
                } else if (initialize) {
                    this.password = null; // default
                } else {
                    // no password present, nothing to be done
                }

                url = url.substring(url.indexOf('@') + "@".length());
            } else if (initialize) {
                // default
                this.username = null;
                this.password = null;
            } else {
                // no username or password present, nothing to be done
            }

            final boolean noSlashAndColonBeforeQuestionMark = url.indexOf('/') < 0 && url.indexOf(':') < url.indexOf('?');
            if (url.indexOf(':') >= 0 && (url.indexOf(':') < url.indexOf('/') ||
                                          noSlashAndColonBeforeQuestionMark ||
                                          (url.indexOf('/') < 0 && url.indexOf('?') < 0))) {
                final String portString;

                if (url.indexOf(':') < url.indexOf('/')) {
                    portString = url.substring(url.indexOf(':') + ":".length(), url.indexOf('/'));
                    url = url.substring(0, url.indexOf(':')) + url.substring(url.indexOf('/'));
                } else if (noSlashAndColonBeforeQuestionMark) {
                    portString = url.substring(url.indexOf(':') + ":".length(), url.indexOf('?'));
                    url = url.substring(0, url.indexOf(':')) + "/" + url.substring(url.indexOf('?'));
                } else { // the case when: url.indexOf ("/") < 0 && url.indexOf ("?") < 0
                    portString = url.substring(url.indexOf(':') + ":".length());
                    url = url.substring(0, url.indexOf(':')) + "/";
                }

                try {
                    this.port = Integer.parseInt(portString);
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable throwable) {
                    // very rare error, found for "about:blank", with "blank" as port
                    // logger.error ("ERROR inside URIVars.modify() Integer.parseInt: {}", new Object[] {this, url, portString, initialize}, exception);
                    // no port present, nothing to be done, port has already been initialized with the protocol initialization
                }
            } else {
                // no port present, nothing to be done, port has already been initialized with the protocol initialization
            }

            if (url.indexOf('#') >= 0) {
                this.fragmentId = new String(url.substring(url.indexOf('#') + "#".length()));
                url = url.substring(0, url.indexOf('#'));
            } else if (initialize) {
                this.fragmentId = null; // default
            } else {
                // no fragmentId present, nothing to be done
            }

            url = url.trim();
            String tempHost;
            final String tempPath;

            if (url.indexOf('/') >= 0) {
                tempHost = url.substring(0, url.indexOf('/'));
                tempPath = url.substring(url.indexOf('/'));
            } else if (url.indexOf('?') >= 0) {
                tempHost = url.substring(0, url.indexOf('?'));
                tempPath = "/" + url.substring(url.indexOf('?'));
            } else {
                tempHost = url;
                tempPath = "/";
            }

            if (Generic.goodDomain(tempHost)) {
                tempHost = URLDecoder.decode(tempHost, StandardCharsets.UTF_8);

                try {
                    tempHost = IDN.toASCII(tempHost, IDN.ALLOW_UNASSIGNED);
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception exception) {
                    // this error should never happen, as this is already managed earlier in Generic.goodDomain ()
                    logger.error("SPOOKY ERROR inside URIVars.modify() IDN.toASCII: {}", new Object[]{url, tempHost}, exception);
                }

                this.host = new String(tempHost.toLowerCase(Locale.ENGLISH).trim());
                this.path = new String(tempPath);
            } else if (initialize) {
                logger.warn("domain is not good inside URIVars.modify(), initialize branch: {} {} {}", tempHost, url, this);
                // this.host = "www.google.com"; // default, leaving an invalid host is not an option, on second thought it is an option if I throw an exception
                this.path = "/"; // default

                throw new java.net.UnknownHostException(tempHost);
            } else { // maybe the remaining url represents a path instead of a host
                if (!url.isEmpty() && url.charAt(0) == '/') {
                    // url is absolute path
                    this.path = new String(url);
                } else if (url.isEmpty()) {
                    // nothing to be done on this branch
                } else if (this.path == null || !(!this.path.isEmpty() && this.path.charAt(0) == '/')) {
                    // this.path is invalid
                    this.path = "/" + url;
                } else if (url.charAt(0) == '?') {
                    if (this.path.indexOf('?') >= 0) {
                        this.path = this.path.substring(0, this.path.indexOf('?')) + url;
                    } else {
                        this.path += url;
                    }
                } else {
                    // this.path is valid, and url is relative path
                    this.path = this.path.substring(0, this.path.lastIndexOf('/') + "/".length()) + url;
                }

                if (initialUrl.indexOf("://") > 0) {
                    // protocol is present, but host/IP are not good
                    // rather rare error, usually related to local network IPs
                    // if (Statics.debugger.getDebugLevel() >= 2) {
                    //     String writeString = "domain is not good inside URIVars.modify: " + tempHost + " " + url + " " + this.path + " " + initialUrl + "\r\n";
                    //     Statics.debugVarsSynchronizedWriter.write (writeString, Statics.ENCRYPTION_KEY);
                    // }

                    throw new java.net.UnknownHostException(tempHost);
                }
            }
        } else if (initialize) {
            logger.warn("url is null inside URIVars.modify(), not changing anything: {} {}", this, initialize); // url == null, nothing to be done
        } else {
            // url == null, nothing to be done
        }

        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    private synchronized URIVars initialize(final String url)
            throws java.net.UnknownHostException {
        return this.modify(url, true);
    }

    public synchronized URIVars newPage(final String url)
            throws java.net.UnknownHostException {
        return this.modify(url, false);
    }

    public synchronized void setDefaultPort(final boolean initialize) {
        this.setDefaultPort(this.protocol, initialize);
    }

    public synchronized void setDefaultPort(final String protocol, final boolean initialize) {
        try {
            this.port = new URL(protocol, "", "").getDefaultPort();
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable throwable) {
            logger.error("ERROR inside URIVars.setDefaultPort: {} {}", protocol, initialize, throwable);

            if (initialize) {
                this.port = 80; // default
            } else {
                // port won't be changed, no default port can be found
            }
        }
    }

    @Override
    public synchronized String toString() {
        final StringBuilder returnStringBuilder = new StringBuilder(this.protocol != null ? this.protocol.toLowerCase(Locale.ENGLISH) : "");
        returnStringBuilder.append("://");

        if (this.username != null) {
            returnStringBuilder.append(this.username);
            if (this.password != null) {
                returnStringBuilder.append(':').append(this.password);
            }
            returnStringBuilder.append('@');
        }
        returnStringBuilder.append(this.host != null ? this.host.toLowerCase(Locale.ENGLISH) : "").append(this.path);
        if (this.fragmentId != null) {
            returnStringBuilder.append('#').append(this.fragmentId);
        }

        return returnStringBuilder.toString();
    }

    @Override
    public synchronized URIVars clone() {
        URIVars resultURIVars = null;
        try {
            resultURIVars = (URIVars) super.clone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            logger.error("cloneNotSupportedException inside URIVars.clone()", cloneNotSupportedException);
        }

        return resultURIVars;
    }
}
