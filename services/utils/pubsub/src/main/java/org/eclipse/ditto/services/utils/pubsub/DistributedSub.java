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
package org.eclipse.ditto.services.utils.pubsub;

import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import org.eclipse.ditto.model.base.acks.AcknowledgementLabel;
import org.eclipse.ditto.services.utils.ddata.DistributedDataConfig;
import org.eclipse.ditto.services.utils.pubsub.actors.AbstractUpdater;

import akka.actor.ActorRef;

/**
 * Access point for Ditto pub-sub subscribers.
 */
public interface DistributedSub {

    /**
     * Subscribe for a collection of topics with a local topic filter.
     *
     * @param topics the topics.
     * @param subscriber who is subscribing.
     * @param filter a local topic filter.
     * @return a future that completes after subscription becomes effective on all nodes.
     */
    CompletionStage<AbstractUpdater.SubAck> subscribeWithFilterAndAck(Collection<String> topics,
            ActorRef subscriber, Predicate<Collection<String>> filter);

    /**
     * Subscribe for a collection of topics.
     *
     * @param topics the topics.
     * @param subscriber who is subscribing.
     * @return a future that completes after subscription becomes effective on all nodes.
     */
    CompletionStage<AbstractUpdater.SubAck> subscribeWithAck(Collection<String> topics, ActorRef subscriber);

    /**
     * Unsubscribe for a collection of topics.
     *
     * @param topics the topics.
     * @param subscriber who is unsubscribing.
     * @return a future that completes when the unsubscriber stops receiving messages on the given topics.
     */
    CompletionStage<AbstractUpdater.SubAck> unsubscribeWithAck(Collection<String> topics, ActorRef subscriber);

    /**
     * Subscribe for topics without waiting for acknowledgement.
     *
     * @param topics the topics.
     * @param subscriber who is subscribing.
     */
    void subscribeWithoutAck(Collection<String> topics, ActorRef subscriber);

    /**
     * Unsubscribe for topics without waiting for acknowledgement.
     *
     * @param topics the topics.
     * @param subscriber who is unsubscribing.
     */
    void unsubscribeWithoutAck(Collection<String> topics, ActorRef subscriber);

    /**
     * Remove a subscriber without waiting for acknowledgement.
     *
     * @param subscriber who is being removed.
     */
    void removeSubscriber(final ActorRef subscriber);

    /**
     * Declare labels of acknowledgements that a subscriber may send.
     * Each subscriber's declared acknowledgment labels must be different from the labels declared by other subscribers.
     * Subscribers relinquish their declared labels when they terminate.
     *
     * @param acknowledgementLabels the acknowledgement labels to declare.
     * @param subscriber the subscriber.
     * @return a future SubAck if the declaration succeeded, or a failed future if it failed.
     */
    CompletionStage<AbstractUpdater.SubAck> declareAcknowledgementLabels(
            final Collection<AcknowledgementLabel> acknowledgementLabels,
            final ActorRef subscriber);

    /**
     * Create subscription access from an already-started sub-supervisor and a distributed data config.
     *
     * @param config the distributed-data config.
     * @param subSupervisor the sub-supervisor.
     * @return the subscription access.
     */
    static DistributedSub of(final DistributedDataConfig config, final ActorRef subSupervisor) {
        return new DistributedSubImpl(config, subSupervisor);
    }
}
