package info.fmro.shared.objects;

import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.stream.cache.market.MarketCache;
import info.fmro.shared.stream.cache.order.OrderCache;
import info.fmro.shared.utility.AlreadyPrintedMap;
import info.fmro.shared.utility.Generic;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("UtilityClass")
public final class SharedStatics {
    public static final boolean notPlacingOrders = true; // hard stop for order placing; true for testing, false enables order placing
    public static final boolean reserveCanDecrease = true; // set as true only for testing, as it affects reserve protection
    public static final boolean noReserve = true; // set as true only for testing, as it removes reserve protection
    public static final long EXECUTOR_KEEP_ALIVE = 10_000L, PROGRAM_START_TIME = System.currentTimeMillis();
    public static final AtomicReference<ProgramName> programName = new AtomicReference<>();
    public static final AlreadyPrintedMap alreadyPrintedMap = new AlreadyPrintedMap();
    public static final AtomicBoolean mustStop = new AtomicBoolean(), mustWriteObjects = new AtomicBoolean(), needSessionToken = new AtomicBoolean(), denyBetting = new AtomicBoolean();
    public static final AtomicReference<String> appKey = new AtomicReference<>();
    public static final AtomicLong timeLastSaveToDisk = new AtomicLong();
    public static final TimeStamps timeStamps = new TimeStamps();
    @SuppressWarnings("PublicStaticCollectionField")
    public static final LinkedBlockingQueue<Runnable> linkedBlockingQueue = new LinkedBlockingQueue<>();
    public static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(64, 64, EXECUTOR_KEEP_ALIVE, TimeUnit.MILLISECONDS, linkedBlockingQueue);
    @SuppressWarnings("PublicStaticCollectionField")
    public static final LinkedBlockingQueue<Runnable> linkedBlockingQueueImportant = new LinkedBlockingQueue<>();
    public static final ThreadPoolExecutor threadPoolExecutorImportant = new ThreadPoolExecutor(64, 64, EXECUTOR_KEEP_ALIVE, TimeUnit.MILLISECONDS, linkedBlockingQueueImportant);
    public static final MarketCache marketCache = new MarketCache();
    public static final OrderCache orderCache = new OrderCache();
    public static final SessionTokenObject sessionTokenObject = new SessionTokenObject();

    private SharedStatics() {
    }

    static {
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        threadPoolExecutorImportant.allowCoreThreadTimeOut(true);
    }

    public static boolean programHasRecentlyStarted() {
        return programHasRecentlyStarted(System.currentTimeMillis());
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public static boolean programHasRecentlyStarted(final long currentTime) {
        return currentTime - PROGRAM_START_TIME <= Generic.MINUTE_LENGTH_MILLISECONDS;
    }
}
