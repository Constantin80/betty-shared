package info.fmro.shared.objects;

import info.fmro.shared.utility.Generic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class Exposure
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Exposure.class);
    private static final long serialVersionUID = 281818769916579900L;
    public static final long recentPeriod = 5_000L;
    private double backMatchedExposure, layMatchedExposure, backUnmatchedExposure, layUnmatchedExposure, backTotalExposure, layTotalExposure, backTempExposure, layTempExposure,
            backUnmatchedProfit, layUnmatchedProfit, backTempProfit, layTempProfit, backTempCancel, layTempCancel;
    private long timeStamp;

    public synchronized void resetExposure() {
        this.backMatchedExposure = 0d;
        this.layMatchedExposure = 0d;
        this.backTotalExposure = 0d;
        this.layTotalExposure = 0d;
        this.backUnmatchedProfit = 0d;
        this.layUnmatchedProfit = 0d;
        this.backTempCancel = 0d;
        this.layTempCancel = 0d;
        this.backUnmatchedExposure = 0d;
        this.layUnmatchedExposure = 0d;
        this.backTempExposure = 0d;
        this.layTempExposure = 0d;
        this.backTempProfit = 0d;
        this.layTempProfit = 0d;
        this.timeStamp = 0L;
    }

//    private boolean hasDefaultValues() {
//        return this.backMatchedExposure == 0d &&
//               this.layMatchedExposure == 0d &&
//               this.backTotalExposure == 0d &&
//               this.layTotalExposure == 0d &&
//               this.backUnmatchedProfit == 0d &&
//               this.layUnmatchedProfit == 0d &&
//               this.tempBackCancel == 0d &&
//               this.tempLayCancel == 0d &&
//               this.timeStamp == 0L;
//    }

    public synchronized void updateExposure(final Exposure source) {
        if (source == null) {
            logger.error("null source in Exposure update for: {}", Generic.objectToString(this));
        } else {
            this.setBackMatchedExposure(source.getBackMatchedExposure());
            this.setLayMatchedExposure(source.getLayMatchedExposure());
            this.setBackTotalExposure(source.getBackTotalExposure());
            this.setLayTotalExposure(source.getLayTotalExposure());
            this.setBackUnmatchedProfit(source.getBackUnmatchedProfit());
            this.setLayUnmatchedProfit(source.getLayUnmatchedProfit());
            this.setBackTempCancel(source.getBackTempCancel());
            this.setLayTempCancel(source.getLayTempCancel());
            this.setBackUnmatchedExposure(source.getBackUnmatchedExposure());
            this.setLayUnmatchedExposure(source.getLayUnmatchedExposure());
            this.setBackTempExposure(source.getBackTempExposure());
            this.setLayTempExposure(source.getLayTempExposure());
            this.setBackTempProfit(source.getBackTempProfit());
            this.setLayTempProfit(source.getLayTempProfit());
            timeStamp(source.getTimeStamp());
        }
    }

    public synchronized void timeStamp() {
        final long currentTime = System.currentTimeMillis();
        timeStamp(currentTime);
    }

    private synchronized void timeStamp(final long currentTime) {
        this.timeStamp = currentTime;
    }

    private synchronized long getTimeStamp() {
        return this.timeStamp;
    }

    @SuppressWarnings("UnusedReturnValue")
    private synchronized boolean isRecent() {
        final long currentTime = System.currentTimeMillis();
        return isRecent(currentTime);
    }

    public synchronized boolean isRecent(final long currentTime) {
        final long timeSinceUpdate = currentTime - this.timeStamp;
        final boolean isRecent = timeSinceUpdate <= recentPeriod;
        if (!isRecent) {
//            if (hasDefaultValues()) { // values have been reset
//            } else {
            logger.error("non recent Exposure {} for: {}", timeSinceUpdate, Generic.objectToString(this));
//            }
        }

        return isRecent;
    }

    public synchronized double getBackUnmatchedExposure() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.backUnmatchedExposure;
    }

    public synchronized void setBackUnmatchedExposure(final double backUnmatchedExposure) {
        this.backUnmatchedExposure = backUnmatchedExposure;
    }

    public synchronized double getLayUnmatchedExposure() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.layUnmatchedExposure;
    }

    public synchronized void setLayUnmatchedExposure(final double layUnmatchedExposure) {
        this.layUnmatchedExposure = layUnmatchedExposure;
    }

    public synchronized double getBackTempExposure() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.backTempExposure;
    }

    public synchronized void setBackTempExposure(final double backTempExposure) {
        this.backTempExposure = backTempExposure;
    }

    public synchronized double getLayTempExposure() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.layTempExposure;
    }

    public synchronized void setLayTempExposure(final double layTempExposure) {
        this.layTempExposure = layTempExposure;
    }

    protected synchronized double getBackTempProfit() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.backTempProfit;
    }

    public synchronized void setBackTempProfit(final double backTempProfit) {
        this.backTempProfit = backTempProfit;
    }

    protected synchronized double getLayTempProfit() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.layTempProfit;
    }

    public synchronized void setLayTempProfit(final double layTempProfit) {
        this.layTempProfit = layTempProfit;
    }

    public synchronized void setBackMatchedExposure(final double backMatchedExposure) {
        this.backMatchedExposure = backMatchedExposure;
    }

    public synchronized void setLayMatchedExposure(final double layMatchedExposure) {
        this.layMatchedExposure = layMatchedExposure;
    }

    private synchronized void setBackTotalExposure(final double backTotalExposure) {
        this.backTotalExposure = backTotalExposure;
    }

    private synchronized void setLayTotalExposure(final double layTotalExposure) {
        this.layTotalExposure = layTotalExposure;
    }

    public synchronized void setBackUnmatchedProfit(final double backUnmatchedProfit) {
        this.backUnmatchedProfit = backUnmatchedProfit;
    }

    public synchronized void setLayUnmatchedProfit(final double layUnmatchedProfit) {
        this.layUnmatchedProfit = layUnmatchedProfit;
    }

    public synchronized void setBackTempCancel(final double backTempCancel) {
        this.backTempCancel = backTempCancel;
    }

    public synchronized void setLayTempCancel(final double layTempCancel) {
        this.layTempCancel = layTempCancel;
    }

    public synchronized double getBackMatchedExposure() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.backMatchedExposure;
    }

    public synchronized double getLayMatchedExposure() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.layMatchedExposure;
    }

    public synchronized double getBackTotalExposure() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.backTotalExposure;
    }

    public synchronized double getLayTotalExposure() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.layTotalExposure;
    }

    protected synchronized double getBackUnmatchedProfit() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.backUnmatchedProfit;
    }

    protected synchronized double getLayUnmatchedProfit() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.layUnmatchedProfit;
    }

    private synchronized double getBackTempCancel() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.backTempCancel;
    }

    private synchronized double getLayTempCancel() {
        isRecent(); // this prints a warning if not recent, but I'll return the value anyway
        return this.layTempCancel;
    }
}
