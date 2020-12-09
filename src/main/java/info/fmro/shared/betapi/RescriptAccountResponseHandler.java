package info.fmro.shared.betapi;

import info.fmro.shared.entities.AccountAPINGException;
import info.fmro.shared.entities.HttpErrorAccountResponse;
import info.fmro.shared.entities.HttpErrorResponseAccountException;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RescriptAccountResponseHandler
        implements ResponseHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(RescriptAccountResponseHandler.class);
    public static final long defaultPrintExpiry = 10_000L;

    // private boolean tooMuchData;

    // public synchronized boolean isTooMuchData() {
    //     return tooMuchData;
    // }
    // public synchronized void setTooMuchData(boolean tooMuchData) {
    //     this.tooMuchData = tooMuchData;
    // }
    @SuppressWarnings("OverlyNestedMethod")
    @Override
    public synchronized String handleResponse(@NotNull final HttpResponse response)
            throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity httpEntity = response.getEntity();
        @Nullable String httpEntityString = httpEntity == null ? null : EntityUtils.toString(httpEntity, Generic.UTF8_CHARSET);
        final int statusCode = statusLine.getStatusCode();
        if (statusCode == 200) { // there's no error present, httpEntityString will be returned further
        } else {
            // <head><body> This object may be found <a HREF="http://content.betfair.com/content/splash/unplanned/index.asp">here</a> </body>
            // <html><body><b>Http/1.1 Service Unavailable</b></body> </html>
            final boolean tempBetfairError = statusCode == 503 || (httpEntityString != null && (httpEntityString.contains("content.betfair.com/content/splash/unplanned/") || httpEntityString.contains("Http/1.1 Service Unavailable")));
            if (tempBetfairError) {
                SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.WARN, "tempBetfairError: {} {}", httpEntityString, statusLine);
                Generic.threadSleep(100L);
            } else {
                final HttpErrorAccountResponse httpErrorAccountResponse = JsonConverter.convertFromJson(httpEntityString, HttpErrorAccountResponse.class);
                if (httpErrorAccountResponse != null) {
                    final HttpErrorResponseAccountException httpErrorResponseAccountException = httpErrorAccountResponse.getDetail();
                    if (httpErrorResponseAccountException != null) {
                        final AccountAPINGException accountAPINGException = httpErrorResponseAccountException.getAccountAPINGException();

                        if (accountAPINGException != null) {
                            switch (accountAPINGException.getErrorCode()) {
                                case INVALID_SESSION_INFORMATION -> {
                                    SharedStatics.needSessionToken.set(true);
                                    SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.INFO, "needing another session token, INVALID_SESSION_INFORMATION, call to api-ng failed: {} {}", httpEntityString, statusLine);
                                    Generic.threadSleep(100L);
                                }
                                case INVALID_APP_KEY -> { // I see this error if I send an invalid session token
                                    SharedStatics.needSessionToken.set(true);
                                    SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.WARN, "needing another session token, INVALID_APP_KEY, call to api-ng failed: {} {}", httpEntityString, statusLine);
                                    Generic.threadSleep(100L);
                                }
                                case NO_SESSION -> { // might be related to an invalid session token
                                    SharedStatics.needSessionToken.set(true);
                                    SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.WARN, "needing another session token, NO_SESSION, call to api-ng failed: {} {}", httpEntityString, statusLine);
                                    Generic.threadSleep(100L);
                                }
                                case TOO_MANY_REQUESTS -> {
                                    SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.ERROR, "too many concurrent requests, call to api-ng failed: {} {}", httpEntityString, statusLine);
                                    Generic.threadSleep(500L);
                                }
                                case UNEXPECTED_ERROR -> {
                                    SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.WARN, "unexpected server error, call to api-ng failed: {} {}", httpEntityString, statusLine);
                                    Generic.threadSleep(100L);
                                }
                                case NO_APP_KEY -> {
                                    SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.ERROR, "NO_APP_KEY server error, call to api-ng failed: {} {}", httpEntityString, statusLine);
                                    Generic.threadSleep(500L);
                                }
                                default -> {
                                    SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.ERROR, "unsupported accountAPINGException errorCode: {}, call to api-ng failed: {} {}",
                                                                            Generic.objectToString(httpErrorAccountResponse), httpEntityString, statusLine);
                                    Generic.threadSleep(500L);
                                }
                            } // end switch
                        } else { // no error parsing can be done; likely json parser exception
                            SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.ERROR, "accountAPINGException null for: {} {}", httpEntityString, statusLine);
                            Generic.threadSleep(100L);
                        }
                    } else { // no error parsing can be done; likely json parser exception
                        SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.ERROR, "httpErrorResponseAccountException null for: {} {}", httpEntityString, statusLine);
                        Generic.threadSleep(100L);
                    }
                } else { // no error parsing can be done; likely json parser exception
                    SharedStatics.alreadyPrintedMap.logOnce(defaultPrintExpiry, logger, LogLevel.ERROR, "httpErrorResponse null for: {} {}", httpEntityString, statusLine);
                    Generic.threadSleep(100L);
                }
            }

            httpEntityString = null; // error string not returned further, as error is not managed and not expected further
        }

        return httpEntityString;
    }
}
