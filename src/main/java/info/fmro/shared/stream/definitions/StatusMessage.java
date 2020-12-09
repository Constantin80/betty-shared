package info.fmro.shared.stream.definitions;

import info.fmro.shared.stream.enums.ErrorCode;
import info.fmro.shared.stream.enums.StatusCode;

import java.io.Serial;
import java.io.Serializable;

// objects of this class are read from the stream
public class StatusMessage
        extends ResponseMessage
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 7917124223046297484L;
    private Boolean connectionClosed; // Is the connection now closed
    private String connectionId; // The connection id
    private ErrorCode errorCode; // The type of error in case of a failure
    private String errorMessage; // Additional message in case of a failure
    private StatusCode statusCode; // The status of the last request
    private Integer connectionsAvailable; // The number of connections available for this account at this moment in time. Present on responses to Authentication messages only.

    public synchronized Boolean getConnectionClosed() {
        return this.connectionClosed;
    }

    public synchronized void setConnectionClosed(final Boolean connectionClosed) {
        this.connectionClosed = connectionClosed;
    }

    public synchronized String getConnectionId() {
        return this.connectionId;
    }

    public synchronized void setConnectionId(final String connectionId) {
        this.connectionId = connectionId;
    }

    public synchronized ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public synchronized void setErrorCode(final ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public synchronized String getErrorMessage() {
        return this.errorMessage;
    }

    public synchronized void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public synchronized StatusCode getStatusCode() {
        return this.statusCode;
    }

    public synchronized void setStatusCode(final StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public synchronized Integer getConnectionsAvailable() {
        return this.connectionsAvailable;
    }

    public synchronized void setConnectionsAvailable(final Integer connectionsAvailable) {
        this.connectionsAvailable = connectionsAvailable;
    }
}
