package info.fmro.shared.logic;

import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.stream.cache.market.MarketCache;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import org.junit.jupiter.api.Test;

class ManagedMarketsMapTest {
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void put() { // no need for assertions, I'm just manually checking some log messages; this test will pass everytime
        final StreamSynchronizedMap<? super String, ? extends Event> eventsMap = new StreamSynchronizedMap<>(Event.class);
        final RulesManager rulesManager = new RulesManager();
        final ManagedEvent parentEvent = new ManagedEvent("eventId", eventsMap, rulesManager.markets, rulesManager.listOfQueues);
        final ManagedMarketsMap managedMarketsMap = new ManagedMarketsMap(parentEvent);

        final MarketCache marketCache = new MarketCache();
        final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap = new StreamSynchronizedMap<>(MarketCatalogue.class);
        final ManagedMarket managedMarket = new ManagedMarket("marketId", marketCache.markets, rulesManager.listOfQueues, rulesManager.marketsToCheck, rulesManager.events, rulesManager.markets, rulesManager.rulesHaveChanged, marketCataloguesMap,
                                                              0L);

        managedMarketsMap.put("marketId", managedMarket, rulesManager.markets);
    }
}
