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
package org.eclipse.ditto.services.gateway.streaming.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.ditto.json.JsonRuntimeException;
import org.eclipse.ditto.model.base.exceptions.DittoJsonException;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.headers.DittoHeaderDefinition;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.protocoladapter.TopicPath;
import org.eclipse.ditto.services.gateway.streaming.ResponsePublished;
import org.eclipse.ditto.services.utils.akka.LogUtil;
import org.eclipse.ditto.signals.base.Signal;
import org.eclipse.ditto.signals.commands.base.Command;
import org.eclipse.ditto.signals.commands.base.exceptions.GatewayInternalErrorException;
import org.eclipse.ditto.signals.commands.thingsearch.ThingSearchCommand;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.EventStream;
import akka.http.javadsl.model.ws.PeerClosedConnectionException;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.actor.AbstractActorSubscriber;
import akka.stream.actor.ActorSubscriberMessage;
import akka.stream.actor.MaxInFlightRequestStrategy;
import akka.stream.actor.RequestStrategy;

/**
 * Actor handling {@link org.eclipse.ditto.signals.commands.base.Command}s by forwarding it to an passed in {@code
 * delegateActor} applying backpressure. <p> Backpressure can be and is only applied for commands requiring a response:
 * {@link DittoHeaders#isResponseRequired()}. </p>
 */
public final class CommandSubscriber extends AbstractActorSubscriber {

    private final DiagnosticLoggingAdapter logger = LogUtil.obtain(this);

    private final ActorRef delegateActor;
    private final int backpressureQueueSize;
    private final List<String> outstandingCommandCorrelationIds = new ArrayList<>();

    @SuppressWarnings("unused")
    private CommandSubscriber(final ActorRef delegateActor, final int backpressureQueueSize,
            final EventStream eventStream) {
        this.delegateActor = delegateActor;
        this.backpressureQueueSize = backpressureQueueSize;

        eventStream.subscribe(getSelf(), ResponsePublished.class);
    }

    /**
     * Creates Akka configuration object Props for this CommandSubscriber.
     *
     * @param delegateActor the ActorRef of the Actor to which to forward {@link Command}s.
     * @param backpressureQueueSize the max queue size of how many inflight commands a single producer can have.
     * @param eventStream used to subscribe to {@link ResponsePublished} events
     * @return the Akka configuration Props object.
     */
    public static Props props(final ActorRef delegateActor, final int backpressureQueueSize,
            final EventStream eventStream) {

        return Props.create(CommandSubscriber.class, delegateActor, backpressureQueueSize, eventStream);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ActorSubscriberMessage.OnNext.class, on -> on.element() instanceof Signal, onNext -> {
                    final Signal<?> signal = (Signal<?>) onNext.element();
                    final Optional<String> correlationIdOpt = signal.getDittoHeaders().getCorrelationId();
                    if (!isResponseExpected(signal)) {
                        logger.debug("Got new fire-and-forget Signal <{}>", signal.getType());
                        delegateActor.tell(signal, getSelf());
                    } else if (correlationIdOpt.isPresent()) {
                        final String correlationId = correlationIdOpt.get();
                        LogUtil.enhanceLogWithCorrelationId(logger, correlationId);

                        outstandingCommandCorrelationIds.add(correlationId);
                        if (outstandingCommandCorrelationIds.size() > backpressureQueueSize) {
                            // this should be prevented by akka and never happen!
                            throw new IllegalStateException(
                                    "queued too many: " + outstandingCommandCorrelationIds.size() +
                                            " - backpressureQueueSize is: " + backpressureQueueSize);
                        }

                        logger.debug("Got new Signal <{}>, currently outstanding are <{}>", signal.getType(),
                                outstandingCommandCorrelationIds.size());
                        delegateActor.tell(signal, getSelf());
                    } else {
                        logger.debug("Got a response-required Signal <{}> without correlationId, " +
                                        "NOT accepting/forwarding it: {}",
                                signal.getType(), signal);
                    }
                })
                .match(ResponsePublished.class, responded ->
                        outstandingCommandCorrelationIds.remove(responded.getCorrelationId()))
                .match(DittoRuntimeException.class, cre -> handleDittoRuntimeException(delegateActor, cre))
                .match(RuntimeException.class,
                        jre -> handleDittoRuntimeException(delegateActor, new DittoJsonException(jre)))
                .match(ActorSubscriberMessage.OnNext.class,
                        onComplete -> logger.warning("Got unknown element in 'OnNext'"))
                .matchEquals(ActorSubscriberMessage.onCompleteInstance(), onComplete -> {
                    logger.info("Stream completed, stopping myself..");
                    getContext().stop(getSelf());
                })
                .match(ActorSubscriberMessage.OnError.class, onError -> {
                    final Throwable cause = onError.cause();
                    if (cause instanceof PeerClosedConnectionException) {
                        // handle PeerClosedConnectionException and stop actor because WS connection was closed.
                        logger.debug(
                                "Received PeerClosedConnectionException with close code <{}> and close reason <{}>.",
                                ((PeerClosedConnectionException) cause).closeCode(),
                                ((PeerClosedConnectionException) cause).closeReason());
                        getContext().stop(getSelf());
                    } else if (cause instanceof DittoRuntimeException) {
                        handleDittoRuntimeException(delegateActor, (DittoRuntimeException) cause);
                    } else if (cause instanceof JsonRuntimeException) {
                        handleDittoRuntimeException(delegateActor, new DittoJsonException((RuntimeException) cause));
                    } else if (cause instanceof RuntimeException) {
                        logger.error(cause, "Unexpected RuntimeException <{}>: ",
                                cause.getClass().getSimpleName(), cause.getMessage());
                        handleDittoRuntimeException(delegateActor, GatewayInternalErrorException.newBuilder()
                                .cause(cause)
                                .build());
                    } else {
                        logger.warning("Got 'OnError': {} {}", cause.getClass().getName(), cause.getMessage());
                    }
                })
                .matchAny(any -> logger.warning("Got unknown message '{}'", any)).build();
    }

    private boolean isResponseExpected(final Signal<?> signal) {
        return signal instanceof Command &&
                // search commands have no responses
                !(signal instanceof ThingSearchCommand) &&
                // signals without responses cannot be tracked
                signal.getDittoHeaders().isResponseRequired() &&
                // live signals should not be tracked - may lead to mysterious throttling
                signal.getDittoHeaders().getChannel().map(TopicPath.Channel.TWIN.name()::equals).orElse(true);
    }

    private void handleDittoRuntimeException(final ActorRef delegateActor, final DittoRuntimeException cre) {
        LogUtil.enhanceLogWithCorrelationId(logger, cre.getDittoHeaders().getCorrelationId());
        logger.info("Got 'DittoRuntimeException': {} {}", cre.getClass().getName(), cre.getMessage());
        cre.getDittoHeaders().getCorrelationId().ifPresent(outstandingCommandCorrelationIds::remove);
        if (cre.getDittoHeaders().isResponseRequired()) {
            delegateActor.forward(cre, getContext());
        } else {
            logger.debug("Requester did not require response (via DittoHeader '{}') - not sending one",
                    DittoHeaderDefinition.RESPONSE_REQUIRED);
        }
    }

    @Override
    public RequestStrategy requestStrategy() {
        return new MaxInFlightRequestStrategy(backpressureQueueSize) {
            @Override
            public int inFlightInternally() {
                return outstandingCommandCorrelationIds.size();
            }
        };
    }

}
