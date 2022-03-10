/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.thingsearch.service.persistence.write.streaming;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import org.eclipse.ditto.internal.models.signalenrichment.CachingSignalEnrichmentFacade;
import org.eclipse.ditto.internal.models.signalenrichment.CachingSignalEnrichmentFacadeProvider;
import org.eclipse.ditto.internal.utils.cache.Cache;
import org.eclipse.ditto.internal.utils.cache.CacheFactory;
import org.eclipse.ditto.internal.utils.cache.config.CacheConfig;
import org.eclipse.ditto.internal.utils.cache.entry.Entry;
import org.eclipse.ditto.internal.utils.cacheloaders.EnforcementCacheKey;
import org.eclipse.ditto.internal.utils.cacheloaders.PolicyEnforcer;
import org.eclipse.ditto.internal.utils.cacheloaders.PolicyEnforcerCacheLoader;
import org.eclipse.ditto.internal.utils.cacheloaders.config.AskWithRetryConfig;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonRuntimeException;
import org.eclipse.ditto.policies.model.PolicyId;
import org.eclipse.ditto.policies.model.PolicyIdInvalidException;
import org.eclipse.ditto.policies.model.enforcers.Enforcer;
import org.eclipse.ditto.things.model.Thing;
import org.eclipse.ditto.things.model.ThingId;
import org.eclipse.ditto.things.model.signals.events.ThingDeleted;
import org.eclipse.ditto.things.model.signals.events.ThingEvent;
import org.eclipse.ditto.thingsearch.service.common.config.StreamCacheConfig;
import org.eclipse.ditto.thingsearch.service.common.config.StreamConfig;
import org.eclipse.ditto.thingsearch.service.persistence.write.mapping.EnforcedThingMapper;
import org.eclipse.ditto.thingsearch.service.persistence.write.model.AbstractWriteModel;
import org.eclipse.ditto.thingsearch.service.persistence.write.model.Metadata;
import org.eclipse.ditto.thingsearch.service.persistence.write.model.ThingDeleteModel;
import org.eclipse.ditto.thingsearch.service.updater.actors.SearchUpdateObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import akka.japi.Pair;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SubSource;

/**
 * Converts Thing changes into write models by retrieving data and applying enforcement via an enforcer cache.
 */
final class EnforcementFlow {

    private static final Source<Entry<Enforcer>, NotUsed> ENFORCER_NONEXISTENT = Source.single(Entry.nonexistent());

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final CachingSignalEnrichmentFacade thingsFacade;
    private final Cache<EnforcementCacheKey, Entry<Enforcer>> policyEnforcerCache;
    private final Duration cacheRetryDelay;
    private final int maxArraySize;
    private final SearchUpdateObserver searchUpdateObserver;

    private EnforcementFlow(final ActorSystem actorSystem,
            final ActorRef thingsShardRegion,
            final Cache<EnforcementCacheKey, Entry<Enforcer>> policyEnforcerCache,
            final AskWithRetryConfig askWithRetryConfig,
            final StreamCacheConfig thingCacheConfig,
            final int maxArraySize,
            final Executor thingCacheDispatcher) {

        thingsFacade = createThingsFacade(actorSystem, thingsShardRegion, askWithRetryConfig.getAskTimeout(),
                thingCacheConfig, thingCacheDispatcher);
        this.policyEnforcerCache = policyEnforcerCache;
        searchUpdateObserver = SearchUpdateObserver.get(actorSystem);
        cacheRetryDelay = thingCacheConfig.getRetryDelay();
        this.maxArraySize = maxArraySize;
    }

    /**
     * Create an EnforcementFlow object.
     *
     * @param actorSystem the actor system for loading the {@link CachingSignalEnrichmentFacadeProvider}
     * @param updaterStreamConfig configuration of the updater stream.
     * @param thingsShardRegion the shard region to retrieve things from.
     * @param policiesShardRegion the shard region to retrieve policies from.
     * @param scheduler the scheduler to use for retrying timed out asks for the policy enforcer cache loader.
     * @return an EnforcementFlow object.
     */
    public static EnforcementFlow of(final ActorSystem actorSystem,
            final StreamConfig updaterStreamConfig,
            final ActorRef thingsShardRegion,
            final ActorRef policiesShardRegion,
            final Scheduler scheduler) {

        final var askWithRetryConfig = updaterStreamConfig.getAskWithRetryConfig();
        final var policyCacheConfig = updaterStreamConfig.getPolicyCacheConfig();
        final var policyCacheDispatcher = actorSystem.dispatchers()
                .lookup(policyCacheConfig.getDispatcherName());

        final AsyncCacheLoader<EnforcementCacheKey, Entry<PolicyEnforcer>> policyEnforcerCacheLoader =
                new PolicyEnforcerCacheLoader(askWithRetryConfig, scheduler, policiesShardRegion);
        final Cache<EnforcementCacheKey, Entry<Enforcer>> policyEnforcerCache =
                CacheFactory.createCache(policyEnforcerCacheLoader, policyCacheConfig,
                                "things-search_enforcementflow_enforcer_cache_policy", policyCacheDispatcher)
                        .projectValues(PolicyEnforcer::project, PolicyEnforcer::embed);

        final var thingCacheConfig = updaterStreamConfig.getThingCacheConfig();
        final var thingCacheDispatcher = actorSystem.dispatchers()
                .lookup(thingCacheConfig.getDispatcherName());
        return new EnforcementFlow(actorSystem, thingsShardRegion, policyEnforcerCache, askWithRetryConfig,
                thingCacheConfig, updaterStreamConfig.getMaxArraySize(), thingCacheDispatcher);
    }

    private static EnforcementCacheKey getPolicyCacheKey(final PolicyId policyId) {
        return EnforcementCacheKey.of(policyId);
    }

    /**
     * Decide whether to reload an enforcer entry.
     * An entry should be reloaded if it is out-of-date, nonexistent, or corresponds to a nonexistent entity.
     *
     * @param entry the enforcer cache entry
     * @param metadata the metadata
     * @param iteration how many times cache read was attempted
     * @return whether to reload the cache
     */
    private static boolean shouldReloadCache(@Nullable final Entry<?> entry, final Metadata metadata,
            final int iteration) {

        if (iteration <= 0) {
            return metadata.shouldInvalidatePolicy() || entry == null || !entry.exists() ||
                    entry.getRevision() < metadata.getPolicyRevision().orElse(Long.MAX_VALUE);
        } else {
            // never attempt to reload cache more than once
            return false;
        }
    }

    /**
     * Create a flow from Thing changes to write models by retrieving data from Things shard region and enforcer cache.
     *
     * @param parallelism how many thing retrieves to perform in parallel to the caching facade.
     * @param maxBulkSize the maximum configured bulk size which is used in this context to create this amount of
     * subSources.
     * @return the flow.
     */
    public Flow<Map<ThingId, Metadata>, SubSource<AbstractWriteModel, NotUsed>, NotUsed> create(
            final int parallelism,
            final int maxBulkSize) {

        return Flow.<Map<ThingId, Metadata>>create()
                .map(changeMap -> {
                    log.info("Updating search index for <{}> changed things", changeMap.size());
                    return Source.fromIterator(changeMap.values()::iterator)
                            .groupBy(maxBulkSize, m -> Math.floorMod(m.getThingId().hashCode(), maxBulkSize))
                            .flatMapMerge(parallelism, changedMetadata ->
                                    retrieveThingFromCachingFacade(changedMetadata.getThingId(), changedMetadata)
                                            .async(MongoSearchUpdaterFlow.DISPATCHER_NAME, parallelism)
                                            .map(pair -> {
                                                final Metadata metadataRef = changeMap.get(pair.first());
                                                final JsonObject thing = pair.second();
                                                searchUpdateObserver.process(metadataRef, thing);
                                                return computeWriteModel(metadataRef, thing)
                                                        .async(MongoSearchUpdaterFlow.DISPATCHER_NAME, parallelism);
                                            })
                            ).flatMapConcat(source -> source);
                });

    }

    private Source<Pair<ThingId, JsonObject>, NotUsed> retrieveThingFromCachingFacade(final ThingId thingId,
            final Metadata metadata) {
        ConsistencyLag.startS3RetrieveThing(metadata);
        final CompletionStage<JsonObject> thingFuture;
        if (metadata.shouldInvalidateThing()) {
            thingFuture = thingsFacade.retrieveThing(thingId, List.of(), -1);
        } else {
            thingFuture = thingsFacade.retrieveThing(thingId, metadata.getEvents(), metadata.getThingRevision());
        }

        return Source.completionStage(thingFuture)
                .filter(thing -> !thing.isEmpty())
                .map(thing -> Pair.create(thingId, thing))
                .recoverWithRetries(1, new PFBuilder<Throwable, Source<Pair<ThingId, JsonObject>, NotUsed>>()
                        .match(Throwable.class, error -> {
                            log.error("Unexpected response for SudoRetrieveThing via cache: <{}>", thingId, error);
                            return Source.empty();
                        })
                        .build());
    }

    private Source<AbstractWriteModel, NotUsed> computeWriteModel(final Metadata metadata,
            @Nullable final JsonObject thing) {

        ConsistencyLag.startS4GetEnforcer(metadata);
        final ThingEvent<?> latestEvent = metadata.getEvents()
                .stream().max(Comparator.comparing(e -> e.getTimestamp().orElseGet(() -> {
                    log.warn("Event <{}> did not contain a timestamp.", e);
                    return Instant.EPOCH;
                })))
                .orElse(null);
        if (latestEvent instanceof ThingDeleted || thing == null) {
            return Source.single(ThingDeleteModel.of(metadata));
        } else {
            return getEnforcer(metadata, thing)
                    .map(entry -> {
                        if (entry.exists()) {
                            try {
                                return EnforcedThingMapper.toWriteModel(thing, entry.getValueOrThrow(),
                                        entry.getRevision(),
                                        maxArraySize,
                                        metadata);
                            } catch (final JsonRuntimeException e) {
                                log.error(e.getMessage(), e);
                                return ThingDeleteModel.of(metadata);
                            }
                        } else {
                            // no enforcer; delete thing from search index
                            return ThingDeleteModel.of(metadata);
                        }
                    });
        }
    }

    /**
     * Get the enforcer of a thing or an empty source if it does not exist.
     *
     * @param metadata metadata of the thing.
     * @param thing the thing
     * @return source of an enforcer or an empty source.
     */
    private Source<Entry<Enforcer>, NotUsed> getEnforcer(final Metadata metadata, final JsonObject thing) {
        try {
            return thing.getValue(Thing.JsonFields.POLICY_ID)
                    .map(PolicyId::of)
                    .map(policyId -> readCachedEnforcer(metadata, getPolicyCacheKey(policyId), 0))
                    .orElse(ENFORCER_NONEXISTENT);
        } catch (final PolicyIdInvalidException e) {
            return ENFORCER_NONEXISTENT;
        }
    }

    private Source<Entry<Enforcer>, NotUsed> readCachedEnforcer(final Metadata metadata,
            final EnforcementCacheKey policyId, final int iteration) {

        final Source<Entry<Enforcer>, ?> lazySource = Source.lazySource(() -> {
            final CompletionStage<Source<Entry<Enforcer>, NotUsed>> enforcerFuture = policyEnforcerCache.get(policyId)
                    .thenApply(optionalEnforcerEntry -> {
                        if (shouldReloadCache(optionalEnforcerEntry.orElse(null), metadata, iteration)) {
                            // invalid entry; invalidate and retry after delay
                            policyEnforcerCache.invalidate(policyId);
                            return readCachedEnforcer(metadata, policyId, iteration + 1)
                                    .initialDelay(cacheRetryDelay);
                        } else {
                            return optionalEnforcerEntry.map(Source::single)
                                    .orElse(ENFORCER_NONEXISTENT);
                        }
                    })
                    .exceptionally(error -> {
                        log.error("Failed to read policyEnforcerCache", error);
                        return ENFORCER_NONEXISTENT;
                    });

            return Source.completionStageSource(enforcerFuture);
        });

        return lazySource.viaMat(Flow.create(), Keep.none());
    }

    private static CachingSignalEnrichmentFacade createThingsFacade(final ActorSystem actorSystem,
            final ActorRef thingsShardRegion,
            final Duration timeout,
            final CacheConfig thingCacheConfig,
            final Executor thingCacheDispatcher) {

        final var sudoRetrieveThingFacade = SudoSignalEnrichmentFacade.of(thingsShardRegion, timeout);
        final var cachingSignalEnrichmentFacadeProvider = CachingSignalEnrichmentFacadeProvider.get(actorSystem);
        return cachingSignalEnrichmentFacadeProvider.getSignalEnrichmentFacade(actorSystem, sudoRetrieveThingFacade,
                thingCacheConfig, thingCacheDispatcher, "things-search_enforcementflow_enforcer_cache_things");
    }

}
