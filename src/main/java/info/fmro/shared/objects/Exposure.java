package info.fmro.shared.objects;

import info.fmro.shared.stream.enums.Side;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;

@SuppressWarnings({"WeakerAccess", "ClassWithTooManyMethods", "OverlyComplexClass"})
public class Exposure
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Exposure.class);
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
    private double backPotentialUnmatchedProfit, layPotentialUnmatchedProfit, backPotentialTempProfit, layPotentialTempProfit;
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
        this.backPotentialTempProfit = 0d;
        this.layPotentialTempProfit = 0d;
    }

    public synchronized void resetMatchedAndUnmatchedExposure() {
        this.backMatchedExposure = 0d;
        this.layMatchedExposure = 0d;
        this.backUnmatchedExposure = 0d;
        this.layUnmatchedExposure = 0d;
        this.backPotentialUnmatchedProfit = 0d;
        this.layPotentialUnmatchedProfit = 0d;
    }

    public synchronized boolean hasExposure() {
        return this.backMatchedExposure > 0d || this.layMatchedExposure > 0d || this.backUnmatchedExposure > 0d || this.layUnmatchedExposure > 0d || this.backTempExposure > 0d || this.layTempExposure > 0d || this.backTempCancelExposure > 0d ||
               this.layTempCancelExposure > 0d || this.backPotentialTempProfit > 0d || this.layPotentialTempProfit > 0d || this.backPotentialUnmatchedProfit > 0d || this.layPotentialUnmatchedProfit > 0d;
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
    public synchronized double getTotalExposure(final Side side) {
        final double result;
        if (side == Side.B) {
            result = getBackTotalExposure();
        } else if (side == Side.L) {
            result = getLayTotalExposure();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
    }

    public synchronized double getBackTotalExposure() {
//        final boolean noOrderPlacing = SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get();
//        return this.backMatchedExposure + this.backUnmatchedExposure + (noOrderPlacing ? 0d : this.backTempExposure);
        return this.backMatchedExposure + this.getBackUnmatchedExposure();
    }

    public synchronized double getLayTotalExposure() {
//        final boolean noOrderPlacing = SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get();
//        return this.layMatchedExposure + this.layUnmatchedExposure + (noOrderPlacing ? 0d : this.layTempExposure);
        return this.layMatchedExposure + this.getLayUnmatchedExposure();
    }

    public synchronized double getTotalExposureConsideringCanceled(final Side side) {
        final double result;
        if (side == Side.B) {
            result = getBackTotalExposureConsideringCanceled();
        } else if (side == Side.L) {
            result = getLayTotalExposureConsideringCanceled();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
    }

    public synchronized double getBackTotalExposureConsideringCanceled() {
        return this.backMatchedExposure + this.getBackUnmatchedExposureConsideringCanceled();
    }

    public synchronized double getLayTotalExposureConsideringCanceled() {
        return this.layMatchedExposure + this.getLayUnmatchedExposureConsideringCanceled();
    }

    public synchronized double getMatchedExposure(final Side side) {
        final double result;
        if (side == Side.B) {
            result = getBackMatchedExposure();
        } else if (side == Side.L) {
            result = getLayMatchedExposure();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
    }

    public synchronized double getBackMatchedExposure() {
        return this.backMatchedExposure;
    }

    public synchronized double getLayMatchedExposure() {
        return this.layMatchedExposure;
    }

    public synchronized double getUnmatchedExposure(final Side side) {
        final double result;
        if (side == Side.B) {
            result = getBackUnmatchedExposure();
        } else if (side == Side.L) {
            result = getLayUnmatchedExposure();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
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

    public synchronized double getBackPotentialUnmatchedProfit() {
        return this.backPotentialUnmatchedProfit + this.backPotentialTempProfit;
    }

    public synchronized double getLayPotentialUnmatchedProfit() {
        return this.layPotentialUnmatchedProfit + this.layPotentialTempProfit;
    }

    public synchronized void setBackPotentialUnmatchedProfit(final double profit) {
        this.backPotentialUnmatchedProfit = profit;
    }

    public synchronized void setLayPotentialUnmatchedProfit(final double profit) {
        this.layPotentialUnmatchedProfit = profit;
    }

    public synchronized double rawBackPotentialUnmatchedProfit() {
        return this.backPotentialUnmatchedProfit;
    }

    public synchronized double rawLayPotentialUnmatchedProfit() {
        return this.layPotentialUnmatchedProfit;
    }

    public synchronized double getBackPotentialTempProfit() {
        return this.backPotentialTempProfit;
    }

    public synchronized double getLayPotentialTempProfit() {
        return this.layPotentialTempProfit;
    }

    public synchronized void setBackPotentialTempProfit(final double profit) {
        this.backPotentialTempProfit = profit;
    }

    public synchronized void setLayPotentialTempProfit(final double profit) {
        this.layPotentialTempProfit = profit;
    }

    public synchronized double getUnmatchedExposureConsideringCanceled(final Side side) {
        final double result;
        if (side == Side.B) {
            result = getBackUnmatchedExposureConsideringCanceled();
        } else if (side == Side.L) {
            result = getLayUnmatchedExposureConsideringCanceled();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
    }

    public synchronized double getBackUnmatchedExposureConsideringCanceled() {
        return this.backUnmatchedExposure - getBackTempCancelExposure() + this.backTempExposure;
    }

    public synchronized double getLayUnmatchedExposureConsideringCanceled() {
        return this.layUnmatchedExposure - getLayTempCancelExposure() + this.layTempExposure;
    }

    public synchronized double rawUnmatchedExposure(final Side side) {
        final double result;
        if (side == Side.B) {
            result = rawBackUnmatchedExposure();
        } else if (side == Side.L) {
            result = rawLayUnmatchedExposure();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
    }

    public synchronized double rawBackUnmatchedExposure() {
        return this.backUnmatchedExposure;
    }

    public synchronized double rawLayUnmatchedExposure() {
        return this.layUnmatchedExposure;
    }

    public synchronized double rawTempExposure(final Side side) {
        final double result;
        if (side == Side.B) {
            result = rawBackTempExposure();
        } else if (side == Side.L) {
            result = rawLayTempExposure();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
    }

    public synchronized double rawBackTempExposure() {
        return this.backTempExposure;
    }

    public synchronized double rawLayTempExposure() {
        return this.layTempExposure;
    }

    public synchronized double totalTempExposure() {
        return rawBackTempExposure() + rawLayTempExposure();
    }

    public synchronized double getTempCancelExposure(final Side side) {
        final double result;
        if (side == Side.B) {
            result = getBackTempCancelExposure();
        } else if (side == Side.L) {
            result = getLayTempCancelExposure();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
    }

    public synchronized double getBackTempCancelExposure() {
        return Math.min(this.backTempCancelExposure, this.backUnmatchedExposure);
    }

    public synchronized double getLayTempCancelExposure() {
        return Math.min(this.layTempCancelExposure, this.layUnmatchedExposure);
    }

    public synchronized void addMatchedExposure(final double exposure, final Side side) {
        if (side == Side.B) {
            addBackMatchedExposure(exposure);
        } else if (side == Side.L) {
            addLayMatchedExposure(exposure);
        } else {
            logger.error("STRANGE unknown side: {}", side);
        }
    }

    public synchronized void addBackMatchedExposure(final double exposure) {
        this.backMatchedExposure += exposure;
    }

    public synchronized void addLayMatchedExposure(final double exposure) {
        this.layMatchedExposure += exposure;
    }

    public synchronized void addMatchedProfit(final double exposure, final Side side) {
        if (side == Side.B) {
            addBackMatchedProfit(exposure);
        } else if (side == Side.L) {
            addLayMatchedProfit(exposure);
        } else {
            logger.error("STRANGE unknown side: {}", side);
        }
    }

    public synchronized void addBackMatchedProfit(final double profit) {
        addLayMatchedExposure(-profit);
    }

    public synchronized void addLayMatchedProfit(final double profit) {
        addBackMatchedExposure(-profit);
    }

//    public synchronized void addUnmatchedExposure(final double exposure, final Side side) {
//        if (side == Side.B) {
//            addBackUnmatchedExposure(exposure);
//        } else if (side == Side.L) {
//            addLayUnmatchedExposure(exposure);
//        } else {
//            logger.error("STRANGE unknown side: {}", side);
//        }
//    }

    public synchronized void addBackUnmatchedExposure(final double exposure) {
        this.backUnmatchedExposure += exposure;
    }

    public synchronized void addLayUnmatchedExposure(final double exposure) {
        this.layUnmatchedExposure += exposure;
    }

    public synchronized void addBackPotentialUnmatchedProfit(final double profit) {
        this.backPotentialUnmatchedProfit += profit;
    }

    public synchronized void addLayPotentialUnmatchedProfit(final double profit) {
        this.layPotentialUnmatchedProfit += profit;
    }

//    public synchronized void addTempExposure(final double exposure, final Side side) {
//        if (side == Side.B) {
//            addBackTempExposure(exposure);
//        } else if (side == Side.L) {
//            addLayTempExposure(exposure);
//        } else {
//            logger.error("STRANGE unknown side: {}", side);
//        }
//    }

    public synchronized void addBackTempExposure(final double exposure) {
        this.backTempExposure += exposure;
    }

    public synchronized void addLayTempExposure(final double exposure) {
        this.layTempExposure += exposure;
    }

    public synchronized void addBackPotentialTempProfit(final double profit) {
        this.backPotentialTempProfit += profit;
    }

    public synchronized void addLayPotentialTempProfit(final double profit) {
        this.layPotentialTempProfit += profit;
    }

    public synchronized void addTempCancelExposure(final double exposure, final Side side) {
        if (side == Side.B) {
            addBackTempCancelExposure(exposure);
        } else if (side == Side.L) {
            addLayTempCancelExposure(exposure);
        } else {
            logger.error("STRANGE unknown side: {}", side);
        }
    }

    public synchronized void addBackTempCancelExposure(final double exposure) {
        this.backTempCancelExposure += exposure;
    }

    public synchronized void addLayTempCancelExposure(final double exposure) {
        this.layTempCancelExposure += exposure;
    }

    public synchronized void setMatchedExposure(final double exposure, final Side side) {
        if (side == Side.B) {
            setBackMatchedExposure(exposure);
        } else if (side == Side.L) {
            setLayMatchedExposure(exposure);
        } else {
            logger.error("STRANGE unknown side: {}", side);
        }
    }

    synchronized void setBackMatchedExposure(final double exposure) {
        this.backMatchedExposure = exposure;
    }

    synchronized void setLayMatchedExposure(final double exposure) {
        this.layMatchedExposure = exposure;
    }

    public synchronized void setUnmatchedExposure(final double exposure, final Side side) {
        if (side == Side.B) {
            setBackUnmatchedExposure(exposure);
        } else if (side == Side.L) {
            setLayUnmatchedExposure(exposure);
        } else {
            logger.error("STRANGE unknown side: {}", side);
        }
    }

    synchronized void setBackUnmatchedExposure(final double exposure) {
        this.backUnmatchedExposure = exposure;
    }

    synchronized void setLayUnmatchedExposure(final double exposure) {
        this.layUnmatchedExposure = exposure;
    }

    public synchronized void setTempExposure(final double exposure, final Side side) {
        if (side == Side.B) {
            setBackTempExposure(exposure);
        } else if (side == Side.L) {
            setLayTempExposure(exposure);
        } else {
            logger.error("STRANGE unknown side: {}", side);
        }
    }

    synchronized void setBackTempExposure(final double exposure) {
        this.backTempExposure = exposure;
    }

    synchronized void setLayTempExposure(final double exposure) {
        this.layTempExposure = exposure;
    }

    public synchronized void setTempCancelExposure(final double exposure, final Side side) {
        if (side == Side.B) {
            setBackTempCancelExposure(exposure);
        } else if (side == Side.L) {
            setLayTempCancelExposure(exposure);
        } else {
            logger.error("STRANGE unknown side: {}", side);
        }
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
        destination.setBackPotentialUnmatchedProfit(this.backPotentialUnmatchedProfit);
        destination.setLayPotentialUnmatchedProfit(this.layPotentialUnmatchedProfit);
        destination.setBackTempExposure(this.backTempExposure);
        destination.setLayTempExposure(this.layTempExposure);
        destination.setBackPotentialTempProfit(this.backPotentialTempProfit);
        destination.setLayPotentialTempProfit(this.layPotentialTempProfit);
        destination.setBackTempCancelExposure(this.backTempCancelExposure);
        destination.setLayTempCancelExposure(this.layTempCancelExposure);
        destination.timeStamp(this.timeStamp);
    }
}
