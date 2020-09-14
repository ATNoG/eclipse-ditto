/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.services.utils.pubsub.actors;

import org.eclipse.ditto.services.utils.akka.LogUtil;
import org.eclipse.ditto.services.utils.pubsub.ddata.DDataWriter;

import akka.actor.AbstractActorWithTimers;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ddata.Replicator;
import akka.event.DiagnosticLoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

/**
 * Remove remote subscriber on cluster event {@link akka.cluster.ClusterEvent.MemberRemoved}.
 */
public final class PubUpdater extends AbstractActorWithTimers {

    /**
     * Prefix of this actor's name.
     */
    public static final String ACTOR_NAME_PREFIX = "pubUpdater";

    // TODO: use thread-safe variant
    private final DiagnosticLoggingAdapter log = LogUtil.obtain(this);

    private final DDataWriter<?> ddataWriter;

    @SuppressWarnings("unused")
    private PubUpdater(final DDataWriter<?> ddataWriter) {
        this.ddataWriter = ddataWriter;
        Cluster.get(getContext().getSystem()).subscribe(getSelf(), ClusterEvent.MemberRemoved.class);
    }

    /**
     * Create Props object for this actor.
     *
     * @param topicsWriter writer of the topics distributed data.
     */
    public static Props props(final DDataWriter topicsWriter) {
        return Props.create(PubUpdater.class, topicsWriter);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ClusterEvent.MemberRemoved.class, this::removeMember)
                .matchAny(this::logUnhandled)
                .build();
    }

    private void removeMember(final ClusterEvent.MemberRemoved memberRemoved) {
        // publisher detected unreachable remote. remove it from local ORMap.
        final Address address = memberRemoved.member().address();
        log.info("Removing subscribers on removed member <{}>", address);
        ddataWriter.removeAddress(address, Replicator.writeLocal()).whenComplete((_void, error) -> {
            if (error != null) {
                log.error(error, "Failed to remove subscribers on removed cluster member <{}>", address);
            }
        });
    }

    private void logUnhandled(final Object message) {
        log.warning("Unhandled: <{}>", message);
    }
}
