package info.fmro.shared.objects;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

@SuppressWarnings("WeakerAccess")
public class Exposure
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 281818769916579900L;
    public static final long RECENT_PERIOD = 1_000L;
    public static final double HUGE_AMOUNT = 1_000_000_000d;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private double backMatchedExposure, layMatchedExposure, backUnmatchedExposure, layUnmatchedExposure;
    @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized", "FieldHasSetterButNoGetter", "RedundantSuppression"})
    private double backTempExposure, layTempExposure;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private double backTempCancelExposure, layTempCancelExposure;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private long timeStamp;

//    public Exposure() {
//    }

//    @SuppressWarnings("CopyConstructorMissesField")
//    private Exposure(@NotNull final Exposure origin) {
//        //noinspection ThisEscapedInObjectConstruction
//        Generic.updateObject(this, origin);
//    }

    public synchronized void resetExposure() {
        resetMatchedAndUnmatchedExposure();
        resetTempExposure();
//        this.timeStamp = 0L;
    }

    public synchronized void resetTempExposure() {
        this.backTempExposure = 0d;
        this.layTempExposure = 0d;
        this.backTempCancelExposure = 0d;
        this.layTempCancelExposure = 0d;
    }

    public synchronized void resetMatchedAndUnmatchedExposure() {
        this.backMatchedExposure = 0d;
        this.layMatchedExposure = 0d;
        this.backUnmatchedExposure = 0d;
        this.layUnmatchedExposure = 0d;
    }

    public synchronized boolean hasExposure() {
        return this.backMatchedExposure > 0d || this.layMatchedExposure > 0d || this.backUnmatchedExposure > 0d || this.layUnmatchedExposure > 0d || this.backTempExposure > 0d || this.layTempExposure > 0d || this.backTempCancelExposure > 0d ||
               this.layTempCancelExposure > 0d;
    }

//    public synchronized void updateExposure(final Exposure source) {
//        if (source == null) {
//            logger.error("null source in Exposure update for: {}", Generic.objectToString(this));
//        } else {
//            this.backMatchedExposure = source.backMatchedExposure;
//            this.layMatchedExposure = source.layMatchedExposure;
//            this.backUnmatchedExposure = source.backUnmatchedExposure;
//            this.layUnmatchedExposure = source.layUnmatchedExposure;
//            this.backTempExposure = source.backTempExposure;
//            this.layTempExposure = source.layTempExposure;
//            this.timeStamp = source.timeStamp;
//        }
//    }

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

    //    @SuppressWarnings("UnusedReturnValue")
//    private synchronized boolean isRecent() {
//        final long currentTime = System.currentTimeMillis();
//        return isRecent(currentTime);
//    }
//
//    public synchronized boolean isRecent(final long currentTime) {
//        final long timeSinceUpdate = currentTime - this.timeStamp;
//        final boolean isRecent = timeSinceUpdate <= recentPeriod;
//        if (!isRecent) {
////            if (hasDefaultValues()) { // values have been reset
////            } else {
//            logger.error("non recent Exposure {} for: {}", timeSinceUpdate, Generic.objectToString(this));
////            }
//        }
//
//        return isRecent;
//    }

    public synchronized double getBackTotalExposure() {
//        final boolean noOrderPlacing = SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get();
//        return this.backMatchedExposure + this.backUnmatchedExposure + (noOrderPlacing ? 0d : this.backTempExposure);
        return this.backMatchedExposure + this.backUnmatchedExposure + this.backTempExposure;
    }

    public synchronized double getLayTotalExposure() {
//        final boolean noOrderPlacing = SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get();
//        return this.layMatchedExposure + this.layUnmatchedExposure + (noOrderPlacing ? 0d : this.layTempExposure);
        return this.layMatchedExposure + this.layUnmatchedExposure + this.layTempExposure;
    }

    public synchronized double getBackMatchedExposure() {
        return this.backMatchedExposure;
    }

    public synchronized double getLayMatchedExposure() {
        return this.layMatchedExposure;
    }

    public synchronized double getBackUnmatchedExposure() {
//        final boolean noOrderPlacing = SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get();
//        return this.backUnmatchedExposure + (noOrderPlacing ? 0d : this.backTempExposure);
        return this.backUnmatchedExposure + this.backTempExposure;
    }

    public synchronized double getLayUnmatchedExposure() {
//        final boolean noOrderPlacing = SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get();
//        return this.layUnmatchedExposure + (noOrderPlacing ? 0d : this.layTempExposure);
        return this.layUnmatchedExposure + this.layTempExposure;
    }

    public synchronized double rawBackUnmatchedExposure() {
        return this.backUnmatchedExposure;
    }

    public synchronized double rawLayUnmatchedExposure() {
        return this.layUnmatchedExposure;
    }

    public synchronized double rawBackTempExposure() {
        return this.backTempExposure;
    }

    public synchronized double rawLayTempExposure() {
        return this.layTempExposure;
    }

    public synchronized double getBackTempCancelExposure() {
        return Math.min(this.backTempCancelExposure, this.backUnmatchedExposure);
    }

    public synchronized double getLayTempCancelExposure() {
        return Math.min(this.layTempCancelExposure, this.layUnmatchedExposure);
    }

    public synchronized void addBackMatchedExposure(final double exposure) {
        this.backMatchedExposure += exposure;
    }

    public synchronized void addLayMatchedExposure(final double exposure) {
        this.layMatchedExposure += exposure;
    }

    public synchronized void addBackMatchedProfit(final double profit) {
        addLayMatchedExposure(-profit);
    }

    public synchronized void addLayMatchedProfit(final double profit) {
        addBackMatchedExposure(-profit);
    }

    public synchronized void addBackUnmatchedExposure(final double exposure) {
        this.backUnmatchedExposure += exposure;
    }

    public synchronized void addLayUnmatchedExposure(final double exposure) {
        this.layUnmatchedExposure += exposure;
    }

    public synchronized void addBackTempExposure(final double exposure) {
        this.backTempExposure += exposure;
    }

    public synchronized void addLayTempExposure(final double exposure) {
        this.layTempExposure += exposure;
    }

    public synchronized void addBackTempCancelExposure(final double exposure) {
        this.backTempCancelExposure += exposure;
    }

    public synchronized void addLayTempCancelExposure(final double exposure) {
        this.layTempCancelExposure += exposure;
    }

    synchronized void setBackMatchedExposure(final double exposure) {
        this.backMatchedExposure = exposure;
    }

    synchronized void setLayMatchedExposure(final double exposure) {
        this.layMatchedExposure = exposure;
    }

    synchronized void setBackUnmatchedExposure(final double exposure) {
        this.backUnmatchedExposure = exposure;
    }

    synchronized void setLayUnmatchedExposure(final double exposure) {
        this.layUnmatchedExposure = exposure;
    }

    synchronized void setBackTempExposure(final double exposure) {
        this.backTempExposure = exposure;
    }

    synchronized void setLayTempExposure(final double exposure) {
        this.layTempExposure = exposure;
    }

    synchronized void setBackTempCancelExposure(final double exposure) {
        this.backTempCancelExposure = exposure;
    }

    synchronized void setLayTempCancelExposure(final double exposure) {
        this.layTempCancelExposure = exposure;
    }
//    @NotNull
//    public synchronized Exposure exposureCopy() {
//        return new Exposure(this);
//    }

    public synchronized void updateArgumentExposure(@NotNull final Exposure destination) {
        destination.setBackMatchedExposure(this.backMatchedExposure);
        destination.setLayMatchedExposure(this.layMatchedExposure);
        destination.setBackUnmatchedExposure(this.backUnmatchedExposure);
        destination.setLayUnmatchedExposure(this.layUnmatchedExposure);
        destination.setBackTempExposure(this.backTempExposure);
        destination.setLayTempExposure(this.layTempExposure);
        destination.setBackTempCancelExposure(this.backTempCancelExposure);
        destination.setLayTempCancelExposure(this.layTempCancelExposure);
        destination.timeStamp(this.timeStamp);
    }
}
