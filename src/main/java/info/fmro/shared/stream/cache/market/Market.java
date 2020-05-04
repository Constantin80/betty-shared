package info.fmro.shared.stream.cache.market;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.enums.MarketStatus;
import info.fmro.shared.logic.RulesManager;
import info.fmro.shared.stream.definitions.MarketChange;
import info.fmro.shared.stream.definitions.MarketDefinition;
import info.fmro.shared.stream.definitions.RunnerChange;
import info.fmro.shared.stream.definitions.RunnerDefinition;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Market
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Market.class);
    private static final long serialVersionUID = -288902433432290477L;
    private final String marketId;
    private final Map<RunnerId, MarketRunner> marketRunners = new ConcurrentHashMap<>(4); // the only place where MarketRunners are permanently stored
    private MarketDefinition marketDefinition;
    private double tv; // total value traded

    public Market(final String marketId, @NotNull final RulesManager rulesManager) {
        this.marketId = marketId;
        rulesManager.addMarketToCheck(marketId);
    }

    synchronized void onMarketChange(@NotNull final MarketChange marketChange, @NotNull final AtomicDouble currencyRate) {
        //initial image means we need to wipe our data
        final boolean isImage = Boolean.TRUE.equals(marketChange.getImg());
        //market definition changed
        Optional.ofNullable(marketChange.getMarketDefinition()).ifPresent(this::onMarketDefinitionChange);
        //runners changed
        Optional.ofNullable(marketChange.getRc()).ifPresent(l -> l.forEach(p -> onPriceChange(isImage, p, currencyRate)));

        this.tv = Formulas.selectPrice(isImage, this.tv, marketChange.getTv());
    }

    private synchronized void onPriceChange(final boolean isImage, @NotNull final RunnerChange runnerChange, @NotNull final AtomicDouble currencyRate) {
        final MarketRunner marketRunner = getOrAdd(new RunnerId(runnerChange.getId(), runnerChange.getHc()));
        //update runner
        marketRunner.onPriceChange(isImage, runnerChange, currencyRate);
    }

    private synchronized void onMarketDefinitionChange(@NotNull final MarketDefinition newMarketDefinition) {
        this.marketDefinition = newMarketDefinition;
        Optional.ofNullable(newMarketDefinition.getRunners()).ifPresent(rds -> rds.forEach(this::onRunnerDefinitionChange));
    }

    private synchronized void onRunnerDefinitionChange(@NotNull final RunnerDefinition runnerDefinition) {
        final MarketRunner marketRunner = getOrAdd(new RunnerId(runnerDefinition.getId(), runnerDefinition.getHc()));
        //update runner
        marketRunner.onRunnerDefinitionChange(runnerDefinition);
    }

    private synchronized MarketRunner getOrAdd(final RunnerId runnerId) {
        return this.marketRunners.computeIfAbsent(runnerId, k -> new MarketRunner(getMarketId(), k));
    }

    public synchronized MarketRunner getMarketRunner(final RunnerId runnerId) {
        return this.marketRunners.get(runnerId);
    }

    public synchronized HashSet<RunnerId> getRunnerIds() {
        return new HashSet<>(this.marketRunners.keySet());
    }

    @Nullable
    public synchronized String getEventId() {
        return this.marketDefinition == null ? null : this.marketDefinition.getEventId();
    }

    @Contract(pure = true)
    private synchronized double getTv() {
        return this.tv;
    }

    public synchronized double getTvEUR(@NotNull final AtomicDouble currencyRate) {
        return getTv() * currencyRate.get();
    }

    public synchronized String getMarketId() {
        return this.marketId;
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public synchronized boolean isClosed() {
        //whether the market is closed
        return (this.marketDefinition != null && this.marketDefinition.getStatus() == MarketStatus.CLOSED);
    }

    public synchronized MarketDefinition getMarketDefinition() {
        return this.marketDefinition;
    }

    public synchronized int getNRunners() {
        final int nRunners;
        //noinspection ConstantConditions
        if (this.marketRunners == null) {
            logger.error("null marketRunners in getNRunners for: {}", Generic.objectToString(this));
            nRunners = -1;
        } else {
            nRunners = this.marketRunners.size();
        }
        return nRunners;
    }

    public synchronized int getNActiveRunners() {
        int nRunners;
        //noinspection ConstantConditions
        if (this.marketRunners == null) {
            logger.error("null marketRunners in getNRunners for: {}", Generic.objectToString(this));
            nRunners = -1;
        } else {
            nRunners = 0;
            for (final MarketRunner marketRunner : this.marketRunners.values()) {
                if (marketRunner != null && marketRunner.isActive()) {
                    nRunners++;
                } else { // not active, nothing to be done
                }
            }
        }
        return nRunners;
    }

    @Nullable
    public synchronized HashMap<RunnerId, MarketRunner> getMarketRunners() {
        //noinspection ConstantConditions
        return this.marketRunners == null ? null : new HashMap<>(this.marketRunners);
    }
}
