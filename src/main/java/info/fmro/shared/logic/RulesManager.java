package info.fmro.shared.logic;

import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.enums.PrefSide;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.Utils;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import info.fmro.shared.utility.SynchronizedSet;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({"WeakerAccess", "ClassWithTooManyMethods", "OverlyComplexClass", "NonPrivateFieldAccessedInSynchronizedContext", "FieldAccessedSynchronizedAndUnsynchronized"})
public class RulesManager
        implements Serializable, StreamObjectInterface {
    private static final Logger logger = LoggerFactory.getLogger(RulesManager.class);
    @Serial
    private static final long serialVersionUID = -3496383465286913313L;
    public static final long fullCheckPeriod = Generic.MINUTE_LENGTH_MILLISECONDS;
    public transient ListOfQueues listOfQueues = new ListOfQueues();
    public final ManagedEventsMap events = new ManagedEventsMap(); // managedEvents are permanently stored only here
    public final SynchronizedMap<String, ManagedMarket> markets = new SynchronizedMap<>(); // managedMarkets are permanently stored only here
    //    public final SynchronizedSafeSet<RulesManagerStringObject> marketsToCheck = new SynchronizedSafeSet<>();
    @SuppressWarnings({"TypeMayBeWeakened", "RedundantSuppression"})
    public final SynchronizedSet<String> addManagedRunnerCommands = new SynchronizedSet<>();
    public transient AtomicBoolean newAddManagedRunnerCommand = new AtomicBoolean();
    public transient AtomicBoolean newOrderMarketCreated = new AtomicBoolean();
    public transient AtomicBoolean marketsToCheckExist = new AtomicBoolean();
    public transient AtomicBoolean marketsMapModified = new AtomicBoolean();
    //    public transient AtomicLong marketsToCheckStamp = new AtomicLong();
    public transient AtomicLong addManagedMarketsForExistingOrdersStamp = new AtomicLong();
    public transient AtomicBoolean rulesHaveChanged = new AtomicBoolean();
    //    public transient AtomicBoolean orderCacheHasReset = new AtomicBoolean();
    public transient AtomicBoolean newMarketsOrEventsForOutsideCheck = new AtomicBoolean();
    public transient SynchronizedSet<String> marketsForOutsideCheck = new SynchronizedSet<>();
    public transient SynchronizedSet<String> eventsForOutsideCheck = new SynchronizedSet<>();
    public final MarketsToCheckMap marketsToCheck = new MarketsToCheckMap(this.marketsToCheckExist);
    private transient long timeLastFullCheck;

    private Integer testMarker; // this variable should be the last declared and not be primitive, to attempt to have it serialized last

    @Serial
    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();

        this.newAddManagedRunnerCommand = new AtomicBoolean();
        this.newOrderMarketCreated = new AtomicBoolean();
        this.marketsToCheckExist = new AtomicBoolean();
        this.marketsMapModified = new AtomicBoolean();
//        this.marketsToCheckStamp = new AtomicLong();
        this.addManagedMarketsForExistingOrdersStamp = new AtomicLong();
        this.rulesHaveChanged = new AtomicBoolean();
//        this.orderCacheHasReset = new AtomicBoolean();
        this.newMarketsOrEventsForOutsideCheck = new AtomicBoolean();
        this.marketsForOutsideCheck = new SynchronizedSet<>();
        this.eventsForOutsideCheck = new SynchronizedSet<>();

        this.timeLastFullCheck = 0L;
    }

    public synchronized RulesManager getCopy() {
        return SerializationUtils.clone(this);
    }

    public synchronized boolean copyFromStream(final RulesManager other) {
        return copyFrom(other, true);
    }

    public synchronized boolean copyFrom(final RulesManager other) {
        return copyFrom(other, false);
    }

    public synchronized boolean copyFrom(final RulesManager other, final boolean isReadingFromStream) {
        final boolean readSuccessful;
        if (!isReadingFromStream && (!this.events.isEmpty() || !this.markets.isEmpty())) {
            logger.error("not empty map in RulesManager copyFrom: {}", Generic.objectToString(this));
            readSuccessful = false;
        } else {
            if (other == null) {
                logger.error("null other in copyFrom for: {} {}", isReadingFromStream, Generic.objectToString(this));
                readSuccessful = false;
            } else {
//                Generic.updateObject(this, other);

                this.events.copyFrom(other.events, isReadingFromStream);
                this.markets.clear();
                this.markets.putAll(other.markets.copy());

                this.marketsToCheck.clear();
                this.marketsToCheck.putAll(other.marketsToCheck);
                this.addManagedRunnerCommands.addAll(other.addManagedRunnerCommands.copy());

                this.setTestMarker(other.testMarker);

//                if (Statics.resetTestMarker) {
//                    logger.error("resetTestMarker {} , will still exit the program after reset", this.testMarker);
//                    final boolean objectModified = this.setTestMarker(Statics.TEST_MARKER);
//                    if (objectModified) {
//                        VarsIO.writeSettings();
//                    }
//                    readSuccessful = false; // in order to exit the program after reset
//                } else {
//                    readSuccessful = this.testMarker != null && this.testMarker == Statics.TEST_MARKER; // testMarker needs to have a certain value
//                }

                if (this.markets.isEmpty() && this.events.isEmpty()) { // maps still empty, no modification was made
                } else {
                    this.rulesHaveChanged.set(true);
                    this.marketsMapModified.set(true);
                    if (this.marketsForOutsideCheck.addAll(this.markets.keySetCopy()) || this.eventsForOutsideCheck.addAll(this.events.keySetCopy())) {
                        this.newMarketsOrEventsForOutsideCheck.set(true);
                    }
                }

                readSuccessful = true;
            }
        }
//        associateMarketsWithEvents( marketCataloguesMap);

        final int nQueues = this.listOfQueues.size();
        if (nQueues == 0) { // normal case, nothing to be done
        } else {
            logger.error("existing queues during RulesManager.copyFrom: {} {}", nQueues, Generic.objectToString(this));
            if (isReadingFromStream) {
                this.listOfQueues.clear();
            } else {
                this.listOfQueues.send(this.getCopy());
            }
        }

        return readSuccessful;
    }

    public synchronized Integer getTestMarker() {
        return this.testMarker;
    }

    public synchronized boolean setTestMarker(final Integer newValue) {
        final boolean modified;
        if (this.testMarker != null && this.testMarker.equals(newValue)) {
            modified = false;
        } else if (this.testMarker == null && newValue == null) {
            modified = false;
        } else {
            this.testMarker = newValue;
            modified = true;
        }

        return modified;
    }

    public synchronized void updateMarketNameFromNewMarketCatalogueAdded(@NotNull final String marketId, @NotNull final MarketCatalogue marketCatalogue) {
        if (this.markets.containsKey(marketId)) {
            final String marketName = Formulas.getMarketCatalogueName(marketCatalogue);
            setMarketName(marketId, marketName);
        } else { // no such managedMarket, won't update anything
        }
    }

    public synchronized void setMarketName(final String marketId, final String marketName) {
        if (marketId == null || marketName == null) {
            logger.error("null parameters in setMarketName for: {} {}", marketId, marketName);
        } else {
            final ManagedMarket managedMarket = this.markets.get(marketId);
            if (managedMarket == null) {
                logger.info("null managedMarket in setMarketName for: {} {}", marketId, marketName);
            } else {
                managedMarket.setMarketName(marketName, this.listOfQueues);
            }
        }
    }

    public synchronized void setMarketEnabled(final String marketId, @NotNull final Boolean marketEnabled) {
        if (marketId == null) {
            logger.error("null marketId in setMarketEnabled for: {}", marketEnabled);
        } else {
            final ManagedMarket managedMarket = this.markets.get(marketId);
            if (managedMarket == null) {
                logger.error("null managedMarket in setMarketEnabled for: {} {}", marketId, marketEnabled);
            } else {
                managedMarket.setEnabledMarket(marketEnabled, this);
            }
        }
    }

    public synchronized void setMarketMandatoryPlace(final String marketId, @NotNull final Boolean mandatoryPlace) {
        if (marketId == null) {
            logger.error("null marketId in setMarketMandatoryPlace for: {}", mandatoryPlace);
        } else {
            final ManagedMarket managedMarket = this.markets.get(marketId);
            if (managedMarket == null) {
                logger.error("null managedMarket in setMarketMandatoryPlace for: {} {}", marketId, mandatoryPlace);
            } else {
                managedMarket.setMandatoryPlace(mandatoryPlace, this);
            }
        }
    }

    public synchronized void setRunnerMandatoryPlace(final String marketId, @NotNull final RunnerId runnerId, @NotNull final Boolean mandatoryPlace) {
        if (marketId == null) {
            logger.error("null marketId in setRunnerMandatoryPlace for: {} {}", runnerId, mandatoryPlace);
        } else {
            final ManagedMarket managedMarket = this.markets.get(marketId);
            if (managedMarket == null) {
                logger.error("null managedMarket in setRunnerMandatoryPlace for: {} {} {}", marketId, runnerId, mandatoryPlace);
            } else {
                managedMarket.setRunnerMandatoryPlace(runnerId, mandatoryPlace, this);
            }
        }
    }

    public synchronized void setMarketKeepAtInPlay(final String marketId, @NotNull final Boolean keepAtInPlay) {
        if (marketId == null) {
            logger.error("null marketId in setMarketKeepAtInPlay for: {}", keepAtInPlay);
        } else {
            final ManagedMarket managedMarket = this.markets.get(marketId);
            if (managedMarket == null) {
                logger.error("null managedMarket in setMarketKeepAtInPlay for: {} {}", marketId, keepAtInPlay);
            } else {
                managedMarket.setKeepAtInPlay(keepAtInPlay, this);
            }
        }
    }

    public synchronized void setRunnerPrefSide(final String marketId, @NotNull final RunnerId runnerId, @NotNull final PrefSide prefSide) {
        if (marketId == null) {
            logger.error("null marketId in setRunnerPrefSide for: {} {}", runnerId, prefSide);
        } else {
            final ManagedMarket managedMarket = this.markets.get(marketId);
            if (managedMarket == null) {
                logger.error("null managedMarket in setRunnerPrefSide for: {} {} {}", marketId, runnerId, prefSide);
            } else {
                managedMarket.setRunnerPrefSide(runnerId, prefSide, this);
            }
        }
    }

    public synchronized void setEventName(final String eventId, final String eventName) {
        if (eventId == null || eventName == null) {
            logger.error("null parameters in setEventName for: {} {}", eventId, eventName);
        } else {
            final ManagedEvent managedEvent = this.events.get(eventId);
            if (managedEvent == null) {
                logger.info("null managedEvent in setEventName for: {} {}", eventId, eventName); // this actually happens in Client, when the command to modify the name is sent before the command to create the ManagedEvent
            } else {
                managedEvent.setEventName(eventName, this.listOfQueues);
            }
        }
    }

    public synchronized boolean setMarketAmountLimit(@NotNull final String marketId, @NotNull final Double amountLimit, @NotNull final ExistingFunds safetyLimits) {
        final boolean success;
        final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket == null) {
            logger.error("null managedMarket in setMarketAmountLimit for: {} {}", marketId, amountLimit);
            success = false;
        } else {
            success = managedMarket.setAmountLimit(amountLimit, this, safetyLimits);
        }

        return success;
    }

    public synchronized ManagedEvent removeManagedEvent(final String eventId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedEvent, eventId));
        @Nullable final ManagedEvent managedEvent = this.events.remove(eventId);
        if (managedEvent != null) {
            @NotNull final HashSet<String> marketIds = managedEvent.marketIds.clear();
            for (final String marketId : marketIds) {
                removeManagedMarket(marketId, marketCataloguesMap);
            }
            this.rulesHaveChanged.set(true);
            logger.info("removed managedEvent: {}", eventId);
        } else {
            logger.error("trying to removeManagedEvent that doesn't exist: {}", eventId); // this also covers the case where the removed element is null, but this should never happen
        }
        return managedEvent;
    }

    public synchronized void attachEvents(@NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                          final @NotNull Iterable<? extends Event> addedEvents) {
        for (final Event event : addedEvents) {
            if (event == null) {
                logger.error("null event in attachEvents for: {}", Generic.objectToString(addedEvents));
            } else {
                final String eventId = event.getId();
                for (final ManagedEvent managedEvent : this.events.valuesCopy()) {
                    if (managedEvent == null) {
                        logger.error("null managedEvent in rulesManager in attachEvents");
                        for (final Map.Entry<String, ManagedEvent> entry : this.events.entrySetCopy()) {
                            final String potentiallyNullEventId = entry.getKey();
                            final ManagedEvent potentialNullManagedEvent = entry.getValue();
                            if (potentialNullManagedEvent == null) {
                                logger.error("null managedEvent id: {}", potentiallyNullEventId);
                                removeManagedEvent(potentiallyNullEventId, marketCataloguesMap);
                            } else { // not null, nothing to be done
                            }
                        }
                    } else {
                        final String managedEventId = managedEvent.getId();
                        if (Objects.equals(eventId, managedEventId)) {
                            managedEvent.attachEvent(eventsMap, this.listOfQueues);
                        } else { // not the event I'm looking for, nothing to be done
                        }
                    }
                }
            }
        }
    }

    public synchronized void checkManagedMarketsHaveParents(@NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
//        logger.info("checkManagedMarketsHaveParents start");
        for (final ManagedMarket managedMarket : this.markets.valuesCopy()) {
            if (managedMarket == null) {
                logger.error("null managedMarket in rulesManager in checkManagedMarketsHaveParents");
                for (final Map.Entry<String, ManagedMarket> entry : this.markets.entrySetCopy()) {
                    final String marketId = entry.getKey();
                    final ManagedMarket potentialNullManagedMarket = entry.getValue();
                    if (potentialNullManagedMarket == null) {
                        logger.error("null managedMarket id: {}", marketId);
                        removeManagedMarket(marketId, marketCataloguesMap);
                    } else { // not null, nothing to be done
                    }
                }
            } else {
                final ManagedEvent managedEvent = managedMarket.getParentEvent(marketCataloguesMap, this);
                if (managedEvent == null) {
                    final String parentEventId = managedMarket.getParentEventId(marketCataloguesMap, this.rulesHaveChanged);
                    if (parentEventId == null) { // I can't do anything with no parentId
//                        logger.info("checkManagedMarketsHaveParents parentEventId null for: {}", managedMarket.getId());
                    } else {
//                        logger.info("checkManagedMarketsHaveParents addManagedEvent {} null for: {}", parentEventId, managedMarket.getId());
                        addManagedEvent(parentEventId, eventsMap, marketCataloguesMap);
                    }
                } else { // I have the managedEvent, nothing more to be done in looking for it
                }
            }
        }
    }

    @NotNull
    protected synchronized ManagedEvent addManagedEvent(@NotNull final String eventId, @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap,
                                                        @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        @NotNull final ManagedEvent managedEvent;
        if (this.events.containsKey(eventId)) {
            final ManagedEvent existingManagedEvent = this.events.get(eventId);
            if (existingManagedEvent == null) {
                logger.error("null managedEvent found in rulesManager for: {} {}", eventId, Generic.objectToString(this.events));
                removeManagedEvent(eventId, marketCataloguesMap);
                managedEvent = new ManagedEvent(eventId, eventsMap, this);
                this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedEvent, eventId, managedEvent));
                this.events.put(eventId, managedEvent, true);
                this.rulesHaveChanged.set(true);
                if (this.eventsForOutsideCheck.add(eventId)) {
                    this.newMarketsOrEventsForOutsideCheck.set(true);
                }
            } else {
                managedEvent = existingManagedEvent;
            }
        } else {
            managedEvent = new ManagedEvent(eventId, eventsMap, this);
            this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedEvent, eventId, managedEvent));
            this.events.put(eventId, managedEvent, true);
            this.rulesHaveChanged.set(true);
            if (this.eventsForOutsideCheck.add(eventId)) {
                this.newMarketsOrEventsForOutsideCheck.set(true);
            }
            if (SharedStatics.programName.get() == ProgramName.SERVER) {
                logger.info("created managedEvent: {}", eventId);
            } else { // only logging on server
            }
        }
        return managedEvent;
    }

    public synchronized boolean addManagedEvent(@NotNull final String eventId, final ManagedEvent managedEvent) {
        final boolean success;
        if (this.events.containsKey(eventId)) {
            final ManagedEvent existingManagedEvent = this.events.get(eventId);
            logger.error("trying to add managedEvent over existing one: {} {} {}", eventId, Generic.objectToString(existingManagedEvent), Generic.objectToString(managedEvent));
            success = false;
        } else if (managedEvent == null) {
            logger.error("trying to add null managedEvent: {}", eventId);
            success = false;
        } else {
            this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedEvent, eventId, managedEvent));
            this.events.put(eventId, managedEvent, true);
            this.rulesHaveChanged.set(true);
            success = true;
            if (this.eventsForOutsideCheck.add(eventId)) {
                this.newMarketsOrEventsForOutsideCheck.set(true);
            }
            if (SharedStatics.programName.get() == ProgramName.SERVER) {
                logger.info("added managedEvent: {}", eventId);
            } else { // only logging on server
            }
        }
        return success;
    }

    public synchronized ManagedEvent setEventAmountLimit(final String eventId, final Double newAmount, @NotNull final ExistingFunds safetyLimits, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        // newAmount == null resets the amount to default -1d value
        @NotNull final ManagedEvent managedEvent = this.events.get(eventId);
        if (managedEvent != null) {
            managedEvent.setAmountLimit(newAmount == null ? -1d : newAmount, this, safetyLimits, marketCataloguesMap);
        } else {
            logger.error("trying to setEventAmountLimit on an event that doesn't exist: {} {}", eventId, newAmount); // this also covers the case where the element is null, but this should never happen
        }
        return managedEvent;
    }

    protected synchronized ManagedMarket addManagedMarket(@NotNull final String marketId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                                          @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        return addManagedMarket(marketId, marketCataloguesMap, eventsMap, true); // by default markets are enabled
    }

    protected synchronized ManagedMarket addManagedMarket(@NotNull final String marketId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                                          @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, final boolean enabledMarket) {
        final ManagedMarket managedMarket;
        if (this.markets.containsKey(marketId)) {
            managedMarket = this.markets.get(marketId);
        } else {
            managedMarket = new ManagedMarket(marketId, this, marketCataloguesMap);
            managedMarket.setEnabledMarket(enabledMarket, this, false);
            final String eventId = managedMarket.getParentEventId(marketCataloguesMap, this.rulesHaveChanged);
            if (eventId == null) { // can be normal, the market or the parent event of the market doesn't exist in my maps, nothing to be done
            } else {
                addManagedEvent(eventId, eventsMap, marketCataloguesMap);
            }
            this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedMarket, marketId, managedMarket));
            this.markets.put(marketId, managedMarket, true);
//            checkMarketsAreAssociatedWithEvents();
            this.rulesHaveChanged.set(true);
            this.marketsMapModified.set(true);
            if (this.marketsForOutsideCheck.add(marketId)) {
                this.newMarketsOrEventsForOutsideCheck.set(true);
            }
            this.marketsToCheck.put(marketId, System.currentTimeMillis());
            if (SharedStatics.programName.get() == ProgramName.SERVER) {
                logger.info("created managedMarket: {} {}", marketId, eventId);
            } else { // only logging on server
            }
        }
        return managedMarket;
    }

    public synchronized boolean addManagedMarket(@NotNull final String marketId, final ManagedMarket managedMarket, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                                 @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        final boolean success;
        if (this.markets.containsKey(marketId)) {
            final ManagedMarket existingManagedMarket = this.markets.get(marketId);
            logger.error("trying to add managedMarket over existing one: {} {} {} {} {} {} {} {} {}", marketId, existingManagedMarket.getMarketId(), existingManagedMarket.simpleGetMarketName(), existingManagedMarket.simpleGetParentEventId(),
                         existingManagedMarket.getSimpleAmountLimit(), managedMarket.getMarketId(), managedMarket.simpleGetMarketName(), managedMarket.simpleGetParentEventId(), managedMarket.getSimpleAmountLimit());
            success = false;
        } else {
            if (managedMarket == null) {
                logger.error("trying to add null managedMarket for: {}", marketId);
                success = false;
            } else {
                final String eventId = managedMarket.getParentEventId(marketCataloguesMap, this.rulesHaveChanged);
                if (eventId == null) { // can be normal, the market or the parent event of the market doesn't exist in my maps, nothing to be done
                } else {
                    addManagedEvent(eventId, eventsMap, marketCataloguesMap);
                }
                this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedMarket, marketId, managedMarket));
                this.markets.put(marketId, managedMarket, true);
//            checkMarketsAreAssociatedWithEvents();
                this.rulesHaveChanged.set(true);
                this.marketsMapModified.set(true);
                if (this.marketsForOutsideCheck.add(marketId)) {
                    this.newMarketsOrEventsForOutsideCheck.set(true);
                }
                this.marketsToCheck.put(marketId, System.currentTimeMillis());
                success = true;
                if (SharedStatics.programName.get() == ProgramName.SERVER) {
                    logger.info("added managedMarket: {} {}", marketId, eventId);
                } else { // only logging on server
                }
            }
        }
        return success;
    }

    public synchronized boolean removeManagedRunner(@NotNull final String marketId, @NotNull final RunnerId runnerId) {
        final boolean success;
        final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket == null) {
            logger.error("null managedMarket in removeManagedRunner for: {} {}", marketId, runnerId);
            success = false;
        } else {
            success = managedMarket.removeRunner(runnerId, this) != null;
        }

        return success;
    }

    public synchronized boolean addManagedRunner(@NotNull final ManagedRunner managedRunner, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                                 @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        final boolean success;
        final String marketId = managedRunner.getMarketId();
        final RunnerId runnerId = managedRunner.getRunnerId();
        if (marketId == null || runnerId == null) {
            logger.error("null ids in addManagedRunner: {} {} {}", marketId, runnerId, Generic.objectToString(managedRunner));
            success = false;
        } else {
            final ManagedMarket managedMarket = this.addManagedMarket(marketId, marketCataloguesMap, eventsMap);
            if (managedMarket == null) {
                logger.error("null managedMarket in addManagedRunner for: {} {} {}", marketId, runnerId, Generic.objectToString(managedRunner));
                success = false;
            } else {
                success = managedMarket.addRunner(runnerId, managedRunner, this);
            }
        }

        return success;
    }

    private synchronized void addManagedRunner(@NotNull final String marketId, final long selectionId, final Double handicap, final double minBackOdds, final double maxLayOdds, final double backAmountLimit, final double layAmountLimit,
                                               final double marketAmountLimit, final double eventAmountLimit, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final ExistingFunds safetyLimits,
                                               @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        final ManagedMarket managedMarket = this.addManagedMarket(marketId, marketCataloguesMap, eventsMap);
        if (managedMarket != null) {
            managedMarket.updateRunner(selectionId, handicap, minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit, this);
            managedMarket.setAmountLimit(marketAmountLimit, this, safetyLimits);
            if (Double.isNaN(eventAmountLimit)) { // nothing to do, checked here for performance reason
            } else {
                ManagedEvent managedEvent = managedMarket.getParentEvent(marketCataloguesMap, this);
                if (managedEvent == null) {
                    final String parentEventId = managedMarket.getParentEventId(marketCataloguesMap, this.rulesHaveChanged);
                    if (parentEventId == null) { // I can't do anything with no parentId, except to print error message
                        logger.error("null parentEventId in addManagedRunner for: {}", marketId);
                    } else {
                        managedEvent = addManagedEvent(parentEventId, eventsMap, marketCataloguesMap);
                    }
                } else { // I have the managedEvent, nothing more to be done in looking for it
                }
                if (managedEvent == null) { // nothing to be done, error message was printed earlier
                } else {
                    managedEvent.setAmountLimit(eventAmountLimit, this, safetyLimits, marketCataloguesMap);
                }
            }
        } else {
            logger.error("null managedMarket in addManagedRunner for: {} {} {} {} {} {} {} {} {}", marketId, selectionId, handicap, minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit, marketAmountLimit, eventAmountLimit);
        }
    }

    public synchronized boolean setRunnerBackAmountLimit(@NotNull final String marketId, @NotNull final RunnerId runnerId, @NotNull final Double amountLimit) {
        final boolean success;
        @Nullable final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket != null) {
            success = managedMarket.setRunnerBackAmountLimit(runnerId, amountLimit, this);
        } else {
            logger.error("trying to setRunnerBackAmountLimit on a market that doesn't exist: {} {} {}", marketId, runnerId, amountLimit); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized boolean setRunnerLayAmountLimit(@NotNull final String marketId, @NotNull final RunnerId runnerId, @NotNull final Double amountLimit) {
        final boolean success;
        @Nullable final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket != null) {
            success = managedMarket.setRunnerLayAmountLimit(runnerId, amountLimit, this);
        } else {
            logger.error("trying to setRunnerLayAmountLimit on a market that doesn't exist: {} {} {}", marketId, runnerId, amountLimit); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized boolean setRunnerMinBackOdds(@NotNull final String marketId, @NotNull final RunnerId runnerId, @NotNull final Double odds) {
        final boolean success;
        @Nullable final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket != null) {
            success = managedMarket.setRunnerMinBackOdds(runnerId, odds, this);
        } else {
            logger.error("trying to setRunnerMinBackOdds on a market that doesn't exist: {} {} {}", marketId, runnerId, odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized boolean setRunnerMaxLayOdds(@NotNull final String marketId, @NotNull final RunnerId runnerId, @NotNull final Double odds) {
        final boolean success;
        @Nullable final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket != null) {
            success = managedMarket.setRunnerMaxLayOdds(runnerId, odds, this);
        } else {
            logger.error("trying to setRunnerMaxLayOdds on a market that doesn't exist: {} {} {}", marketId, runnerId, odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized ManagedMarket removeManagedMarket(final String marketId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedMarket, marketId));
        @Nullable final ManagedMarket managedMarket = this.markets.remove(marketId);
        if (managedMarket != null) {
            final ManagedEvent parentEvent = managedMarket.getParentEvent(marketCataloguesMap, this);
            if (parentEvent == null) {
                logger.info("null parentEvent in removeManagedMarket, probably expired market: {}", marketId);
            } else {
                parentEvent.marketIds.remove(marketId);
            }
            this.rulesHaveChanged.set(true);
            this.marketsMapModified.set(true);
            logger.info("removed managedMarket: {}", marketId);
        } else { // this actually normally happens in client, when I first remove the market locally and send command to the server to remove it, then the server send the command to remove the market back
            logger.info("trying to removeManagedMarket that doesn't exist: {}", marketId); // this also covers the case where the removed element is null, but this should never happen
        }
        return managedMarket;
    }

    public synchronized void executeCommand(@NotNull final String commandString, @NotNull final ExistingFunds safetyLimits, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                            @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        if (commandString.startsWith("event ")) {
            final String eventString = commandString.substring("event ".length()).trim();
            if (eventString.contains(" ")) {
                final String eventId = eventString.substring(0, eventString.indexOf(' '));
                final String eventCommand = eventString.substring(eventString.indexOf(' ') + " ".length()).trim();
                if (eventCommand.startsWith("amountLimit")) {
                    String amountLimit = eventCommand.substring("amountLimit".length()).trim();
                    if (!amountLimit.isEmpty() && amountLimit.charAt(0) == '=') {
                        amountLimit = amountLimit.substring("=".length()).trim();
                    } else { // nothing to do on this branch
                    }
                    double doubleValue;
                    try {
                        doubleValue = Double.parseDouble(amountLimit);
                    } catch (NumberFormatException e) {
                        logger.error("NumberFormatException while getting doubleValue for amountLimit in executeCommand: {} {}", commandString, amountLimit, e);
                        doubleValue = Double.NaN;
                    }
                    if (Double.isNaN(doubleValue)) { // error message was already printed
                    } else {
                        final ManagedEvent managedEvent = addManagedEvent(eventId, eventsMap, marketCataloguesMap);
                        managedEvent.setAmountLimit(doubleValue, this, safetyLimits, marketCataloguesMap);
                    }
                } else {
                    logger.error("unknown eventCommand in executeCommand: {} {}", eventCommand, commandString);
                }
            } else {
                logger.error("strange eventString in executeCommand: {} {}", eventString, commandString);
            }
        } else {
            logger.error("unknown command in executeCommand: {}", commandString);
        }
    }

//    private synchronized boolean isMarketsToCheckStampRecent() { // stamp is updated, but not checked for anything yet
//        return isMarketsToCheckStampRecent(500L); // default period
//    }
//
//    private synchronized boolean isMarketsToCheckStampRecent(final long recentPeriod) {
//        return timeSinceMarketsToCheckStamp() <= recentPeriod;
//    }

//    private synchronized long timeSinceMarketsToCheckStamp() {
//        final long currentTime = System.currentTimeMillis();
//        final long stampTime = this.marketsToCheckStamp.get();
//        return currentTime - stampTime;
//    }

    private synchronized long getTimeLastFullCheck() {
        return this.timeLastFullCheck;
    }

    public synchronized void stampTimeLastFullCheck() {
        setTimeLastFullCheck(System.currentTimeMillis());
    }

    private synchronized void setTimeLastFullCheck(final long time) {
        this.timeLastFullCheck = time;
    }

    private synchronized long timeSinceFullRun() {
        final long currentTime = System.currentTimeMillis();
        return Math.max(0L, currentTime - this.getTimeLastFullCheck());
    }

    public synchronized long timeTillNextFullRun() {
        final long result;
//        if (this.timeLastFullCheck <= 0) {
//            result = 0;
//        } else {
        result = fullCheckPeriod - timeSinceFullRun(); // negative values are acceptable
//        }
        return result;
    }

    public synchronized void calculateMarketLimits(@NotNull final ExistingFunds safetyLimits, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
//        this.marketsMapModified.set(false);
        final double totalLimit = safetyLimits.getTotalLimit();
        Utils.calculateMarketLimits(totalLimit, this.markets.valuesCopy(), true, true, safetyLimits, marketCataloguesMap, this);

        for (final ManagedEvent managedEvent : this.events.valuesCopy()) {
            managedEvent.calculateMarketLimits(this, safetyLimits, marketCataloguesMap);
        }
    }

    public synchronized boolean setMarketCalculatedLimit(final String marketId, final double newLimit, @NotNull final ExistingFunds safetyLimits) {
        final boolean modified;
        final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket == null) {
            logger.error("null managedMarket in setCalculatedLimit for: {} {}", marketId, newLimit);
            modified = false;
        } else {
            modified = managedMarket.setCalculatedLimit(newLimit, true, safetyLimits, this.listOfQueues);
        }
        return modified;
    }

//    public synchronized void calculateMarketLimits(Collection<String> marketIds) {
//        final int nMarketIds = marketIds.size();
//        int nMarketCalculatedLimits = 0;
//        for (ManagedEvent managedEvent : this.events.valuesCopy()) {
//            boolean neededEvent = false;
//            for (String marketId : marketIds) {
//                if (managedEvent.marketIds.contains(marketId)) {
//                    neededEvent = true;
//                    nMarketCalculatedLimits++; // no break, as there might be more than one market attached to the same event and I should count that
//                } else { // marketId not contained in this event, nothing to be done
//                }
//            }
//            if (neededEvent) {
//                managedEvent.calculateMarketLimits();
//            } else { // I don't need to calculateMarketLimits for this event
//            }
//            if (nMarketCalculatedLimits >= nMarketIds) {
//                break;
//            }
//        }
//    }

    public void manageMarket(final ManagedMarket managedMarket, @NotNull final BetFrequencyLimit speedLimit, @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod,
                             @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final boolean emergencyExecute) {
        // intentionally not synchronized; isBeingManaged AtomicBoolean in managedMarket.manage will be used to make sure it only runs once
        if (managedMarket == null) {
            logger.error("null managedMarket to check in RulesManager, marketsToCheck: {}", Generic.objectToString(this.marketsToCheck));
            this.markets.removeValueAll(null);
            this.listOfQueues.send(this.getCopy());
            this.rulesHaveChanged.set(true);
            this.marketsMapModified.set(true);
        } else {
            managedMarket.manage(speedLimit, existingFunds, this, sendPostRequestRescriptMethod, marketCataloguesMap, emergencyExecute);
        }
    }

    public synchronized void addManagedMarketsForExistingOrders(@NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        final long currentTime = System.currentTimeMillis();
        final long stampTime = this.addManagedMarketsForExistingOrdersStamp.get();
        final long timeSinceStamp = currentTime - stampTime;
        if (this.newOrderMarketCreated.getAndSet(false) || timeSinceStamp > Generic.MINUTE_LENGTH_MILLISECONDS * 5L) {
            this.addManagedMarketsForExistingOrdersStamp.set(currentTime);
            final Collection<String> orderMarkets = SharedStatics.orderCache.markets.keySetCopy();
            final Set<String> managedMarkets = this.markets.keySetCopy();
            orderMarkets.removeAll(managedMarkets);
            if (orderMarkets.isEmpty()) { // no new markets; nothing to be done, this should be the taken branch almost all the time
            } else {
                for (final String marketId : orderMarkets) {
                    logger.info("adding new managed market in addManagedMarketsForExistingOrders: {}", marketId);
                    if (marketId == null) {
                        logger.error("null marketId in addManagedMarketsForExistingOrders for: {}", Generic.objectToString(orderMarkets));
                    } else {
                        addManagedMarket(marketId, marketCataloguesMap, eventsMap, false);
                    }
                }
            }
        } else { // I won't run this method too often; nothing to be done
        }

        // also attach OrderMarkets
//        for (final ManagedMarket managedMarket : this.markets.valuesCopy()) {
//            if (managedMarket == null) {
//                logger.error("null managedMarket in addManagedMarketsForExistingOrders for: {}", Generic.objectToString(this.markets.keySetCopy()));
//                for (@NotNull final Map.Entry<String, ManagedMarket> entry : this.markets.entrySetCopy()) {
//                    final String key = entry.getKey();
//                    final ManagedMarket value = entry.getValue();
//                    if (value == null) {
//                        logger.error("null managedMarket in addManagedMarketsForExistingOrders for: {}", key);
//                        removeManagedMarket(key, marketCataloguesMap);
//                    } else { // not null, nothing to be done
//                    }
//                }
//            } else {
//                managedMarket.attachOrderMarket(this, marketCataloguesMap);
//            }
//        }
    }

//    public synchronized void resetOrderCacheObjects() {
//        this.orderCacheHasReset.set(false);
//        for (final ManagedMarket managedMarket : this.markets.valuesCopy()) {
//            managedMarket.resetOrderCacheObjects();
//        }
//    }

    public synchronized void addManagedRunnerCommands(@NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final ExistingFunds safetyLimits,
                                                      @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        this.newAddManagedRunnerCommand.set(false);
        final HashSet<String> setCopy = this.addManagedRunnerCommands.clear();

        for (final String addManagedRunnerCommand : setCopy) {
            parseAddManagedRunnerCommand(addManagedRunnerCommand, marketCataloguesMap, safetyLimits, eventsMap);
        }
    }

    private synchronized void parseAddManagedRunnerCommand(final String addManagedRunnerCommand, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final ExistingFunds safetyLimits,
                                                           @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        // String:marketId long:selectionId Double:handicap(default:null)
        // double optionals:minBackOdds maxLayOdds backAmountLimit layAmountLimit marketAmountLimit eventAmountLimit
        final String[] argumentsArray = Generic.splitStringAroundSpaces(addManagedRunnerCommand);
        final int arrayLength = argumentsArray.length;
        if (arrayLength >= 2 && arrayLength <= 9) {
            final String marketId = argumentsArray[0];
            final String selectionIdString = argumentsArray[1];
            long selectionId = -1L;
            try {
                selectionId = Long.parseLong(selectionIdString);
            } catch (NumberFormatException e) {
                logger.error("NumberFormatException while parsing selectionId in parseAddManagedRunnerCommand for: {} {}", selectionIdString, addManagedRunnerCommand);
            }

            if (selectionId < 0L) { // error message already printed, nothing to be done
            } else {
                @Nullable Double handicap = Double.NaN;
                double minBackOdds = Double.NaN, maxLayOdds = Double.NaN, backAmountLimit = Double.NaN, layAmountLimit = Double.NaN, marketAmountLimit = Double.NaN, eventAmountLimit = Double.NaN;
                boolean errorExists = false;
                for (int i = 2; i < arrayLength; i++) {
                    switch (i) {
                        case 2:
                            if ("null".equals(argumentsArray[i])) {
                                handicap = null;
                            } else {
                                handicap = Generic.parseDouble(argumentsArray[i]);
                                if (Double.isNaN(handicap)) {
                                    errorExists = true;
                                }
                            }
                            break;
                        case 3:
                            minBackOdds = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(minBackOdds)) {
                                errorExists = true;
                            }
                            break;
                        case 4:
                            maxLayOdds = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(maxLayOdds)) {
                                errorExists = true;
                            }
                            break;
                        case 5:
                            backAmountLimit = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(backAmountLimit)) {
                                errorExists = true;
                            }
                            break;
                        case 6:
                            layAmountLimit = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(layAmountLimit)) {
                                errorExists = true;
                            }
                            break;
                        case 7:
                            marketAmountLimit = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(marketAmountLimit)) {
                                errorExists = true;
                            }
                            break;
                        case 8:
                            eventAmountLimit = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(eventAmountLimit)) {
                                errorExists = true;
                            }
                            break;
                        default:
                            logger.error("default switch branch entered inside parseAddManagedRunnerCommand for: {} {} {} {}", i, arrayLength, argumentsArray[i], addManagedRunnerCommand);
                            errorExists = true;
                            break;
                    }
                    if (errorExists) {
                        break;
                    }
                }
                if (errorExists) {
                    logger.error("bogus addManagedRunnerCommand: {} {}", arrayLength, addManagedRunnerCommand);
                } else {
                    addManagedRunner(marketId, selectionId, handicap, minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit, marketAmountLimit, eventAmountLimit, marketCataloguesMap, safetyLimits, eventsMap);
                }
            }
        } else {
            logger.error("wrong arrayLength in parseAddManagedRunnerCommand for: {} {}", arrayLength, addManagedRunnerCommand);
        }
    }

    @Nullable
    private synchronized ManagedMarket getManagedMarket(final String marketId) {
        return this.markets.get(marketId);
    }

    @Nullable
    public synchronized ManagedRunner getManagedRunner(final String marketId, @Nullable final RunnerId runnerId) {
        @Nullable final ManagedRunner managedRunner;
        final ManagedMarket managedMarket = getManagedMarket(marketId);
        if (managedMarket == null) {
            logger.error("null managedMarket in getManagedRunner for: {} {}", marketId, runnerId);
            managedRunner = null;
        } else {
            managedRunner = managedMarket.getManagedRunner(runnerId);
        }
        return managedRunner;
    }

    public synchronized Set<String> getManagedMarketIds() {
        return this.markets.keySetCopy();
    }
}
