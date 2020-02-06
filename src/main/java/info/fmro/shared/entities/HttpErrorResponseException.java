package info.fmro.shared.entities;

@SuppressWarnings({"NonExceptionNameEndsWithException", "SpellCheckingInspection"})
public class HttpErrorResponseException {
    private String exceptionname;
    private info.fmro.shared.entities.APINGException APINGException;

    public synchronized String getExceptionname() {
        return this.exceptionname;
    }

    public synchronized void setExceptionname(final String exceptionname) {
        this.exceptionname = exceptionname;
    }

    public synchronized APINGException getAPINGException() {
        return this.APINGException;
    }

    public synchronized void setAPINGException(final APINGException APINGException) {
        this.APINGException = APINGException;
    }
}
