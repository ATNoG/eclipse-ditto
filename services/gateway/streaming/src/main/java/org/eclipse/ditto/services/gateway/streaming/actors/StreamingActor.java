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

import java.util.Optional;
import java.util.stream.StreamSupport;

import org.eclipse.ditto.model.base.auth.AuthorizationContext;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.headers.WithDittoHeaders;
import org.eclipse.ditto.model.jwt.ImmutableJsonWebToken;
import org.eclipse.ditto.model.jwt.JsonWebToken;
import org.eclipse.ditto.services.gateway.security.authentication.jwt.JwtAuthenticationFactory;
import org.eclipse.ditto.services.gateway.security.authentication.jwt.JwtAuthorizationContextProvider;
import org.eclipse.ditto.services.gateway.security.authentication.jwt.JwtValidator;
import org.eclipse.ditto.services.gateway.streaming.Connect;
import org.eclipse.ditto.services.gateway.streaming.DefaultStreamingConfig;
import org.eclipse.ditto.services.gateway.streaming.InvalidJwt;
import org.eclipse.ditto.services.gateway.streaming.Jwt;
import org.eclipse.ditto.services.gateway.streaming.RefreshSession;
import org.eclipse.ditto.services.gateway.streaming.StartStreaming;
import org.eclipse.ditto.services.gateway.streaming.StopStreaming;
import org.eclipse.ditto.services.gateway.streaming.StreamingConfig;
import org.eclipse.ditto.services.models.concierge.pubsub.DittoProtocolSub;
import org.eclipse.ditto.services.utils.akka.actors.ModifyConfigBehavior;
import org.eclipse.ditto.services.utils.akka.actors.RetrieveConfigBehavior;
import org.eclipse.ditto.services.utils.akka.logging.DittoDiagnosticLoggingAdapter;
import org.eclipse.ditto.services.utils.akka.logging.DittoLoggerFactory;
import org.eclipse.ditto.services.utils.metrics.DittoMetrics;
import org.eclipse.ditto.services.utils.metrics.instruments.gauge.Gauge;
import org.eclipse.ditto.signals.base.Signal;

import com.typesafe.config.Config;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;

/**
 * Parent Actor for {@link StreamingSessionActor}s delegating most of the messages to a specific session.
 * Manages WebSocket configuration.
 */
public final class StreamingActor extends AbstractActorWithTimers
        implements RetrieveConfigBehavior, ModifyConfigBehavior {

    /**
     * The name of this Actor.
     */
    public static final String ACTOR_NAME = "streaming";

    private final DittoProtocolSub dittoProtocolSub;
    private final ActorRef commandRouter;
    private final Gauge streamingSessionsCounter;
    private final JwtValidator jwtValidator;
    private final JwtAuthorizationContextProvider jwtAuthorizationContextProvider;
    private final DittoDiagnosticLoggingAdapter logger = DittoLoggerFactory.getDiagnosticLoggingAdapter(this);

    private final SupervisorStrategy strategy = new OneForOneStrategy(true, DeciderBuilder
            .match(Throwable.class, e -> {
                logger.error(e, "Escalating above actor!");
                return SupervisorStrategy.escalate();
            }).matchAny(e -> {
                logger.error("Unknown message:'{}'! Escalating above actor!", e);
                return SupervisorStrategy.escalate();
            }).build());

    private StreamingConfig streamingConfig;

    @SuppressWarnings("unused")
    private StreamingActor(final DittoProtocolSub dittoProtocolSub,
            final ActorRef commandRouter,
            final JwtAuthenticationFactory jwtAuthenticationFactory,
            final StreamingConfig streamingConfig) {

        this.dittoProtocolSub = dittoProtocolSub;
        this.commandRouter = commandRouter;
        this.streamingConfig = streamingConfig;
        streamingSessionsCounter = DittoMetrics.gauge("streaming_sessions_count");
        jwtValidator = jwtAuthenticationFactory.getJwtValidator();
        jwtAuthorizationContextProvider = jwtAuthenticationFactory.newJwtAuthorizationContextProvider();
        scheduleScrapeStreamSessionsCounter();
    }

    /**
     * Creates Akka configuration object Props for this StreamingActor.
     *
     * @param dittoProtocolSub the Ditto protocol sub access.
     * @param commandRouter the command router used to send signals into the cluster
     * @param streamingConfig the streaming config
     * @return the Akka configuration Props object.
     */
    public static Props props(final DittoProtocolSub dittoProtocolSub,
            final ActorRef commandRouter,
            final JwtAuthenticationFactory jwtAuthenticationFactory,
            final StreamingConfig streamingConfig) {

        return Props.create(StreamingActor.class, dittoProtocolSub, commandRouter, jwtAuthenticationFactory,
                streamingConfig);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                // Handle internal connect/streaming commands
                .match(Connect.class, connect -> {
                    final ActorRef eventAndResponsePublisher = connect.getEventAndResponsePublisher();
                    eventAndResponsePublisher.forward(connect, getContext());
                    final String connectionCorrelationId = connect.getConnectionCorrelationId();
                    getContext().actorOf(
                            StreamingSessionActor.props(connect, dittoProtocolSub, eventAndResponsePublisher,
                                    streamingConfig.getAcknowledgementConfig()),
                            connectionCorrelationId);
                })
                .match(StartStreaming.class,
                        startStreaming -> forwardToSessionActor(startStreaming.getConnectionCorrelationId(),
                                startStreaming)
                )
                .match(StopStreaming.class,
                        stopStreaming -> forwardToSessionActor(stopStreaming.getConnectionCorrelationId(),
                                stopStreaming)
                )
                .match(Jwt.class, this::refreshWebSocketSession)
                .build()
                .orElse(retrieveConfigBehavior())
                .orElse(modifyConfigBehavior())
                .orElse(ReceiveBuilder.create()
                        .match(Signal.class, signal -> {
                            final DittoHeaders dittoHeaders = signal.getDittoHeaders();
                            final Optional<String> originOpt = dittoHeaders.getOrigin();
                            if (originOpt.isPresent()) {
                                final String origin = originOpt.get();
                                final Optional<ActorRef> sessionActor = getContext().findChild(origin);
                                if (sessionActor.isPresent()) {
                                    final ActorRef sender = dittoHeaders.isResponseRequired() ? sessionActor.get() :
                                            ActorRef.noSender();
                                    commandRouter.tell(signal, sender);
                                } else {
                                    logger.withCorrelationId(signal)
                                            .debug("No session actor found for origin <{}>.", origin);
                                }
                            } else {
                                logger.withCorrelationId(signal)
                                        .warning("Signal is missing the required origin header!");
                            }
                        })
                        .matchEquals(Control.RETRIEVE_WEBSOCKET_CONFIG, this::replyWebSocketConfig)
                        .matchEquals(Control.SCRAPE_STREAM_COUNTER, this::updateStreamingSessionsCounter)
                        .match(DittoRuntimeException.class, cre -> {
                            final Optional<String> originOpt = cre.getDittoHeaders().getOrigin();
                            if (originOpt.isPresent()) {
                                forwardToSessionActor(originOpt.get(), cre);
                            } else {
                                logger.withCorrelationId(cre).warning("Unhandled DittoRuntimeException: <{}: {}>",
                                        cre.getClass().getSimpleName(), cre.getMessage());
                            }
                        })
                        .matchAny(any -> logger.warning("Got unknown message: '{}'", any))
                        .build());
    }

    @Override
    public Config getConfig() {
        return streamingConfig.render().getConfig(StreamingConfig.CONFIG_PATH);
    }

    @Override
    public Config setConfig(final Config config) {
        streamingConfig = DefaultStreamingConfig.of(
                config.atKey(StreamingConfig.CONFIG_PATH).withFallback(streamingConfig.render()));
        // reschedule scrapes: interval may have changed.
        scheduleScrapeStreamSessionsCounter();
        return streamingConfig.render();
    }


    private void refreshWebSocketSession(final Jwt jwt) {
        final String connectionCorrelationId = jwt.getConnectionCorrelationId();
        final JsonWebToken jsonWebToken = ImmutableJsonWebToken.fromToken(jwt.toString());
        jwtValidator.validate(jsonWebToken).thenAccept(binaryValidationResult -> {
            if (binaryValidationResult.isValid()) {
                try {
                    final AuthorizationContext authorizationContext =
                            jwtAuthorizationContextProvider.getAuthorizationContext(jsonWebToken);

                    forwardToSessionActor(connectionCorrelationId,
                            new RefreshSession(connectionCorrelationId, jsonWebToken.getExpirationTime(),
                                    authorizationContext));
                } catch (final Exception exception) {
                    logger.info("Got exception when handling refreshed JWT for WebSocket session <{}>: {}",
                            connectionCorrelationId, exception.getMessage());
                    forwardToSessionActor(connectionCorrelationId, InvalidJwt.getInstance());
                }
            } else {
                forwardToSessionActor(connectionCorrelationId, InvalidJwt.getInstance());
            }
        });
    }

    private void forwardToSessionActor(final String connectionCorrelationId, final Object object) {
        if (object instanceof WithDittoHeaders) {
            logger.setCorrelationId((WithDittoHeaders) object);
        }
        logger.debug("Forwarding to session actor '{}': {}", connectionCorrelationId, object);
        logger.discardCorrelationId();
        getContext().actorSelection(connectionCorrelationId).forward(object, getContext());
    }

    private void scheduleScrapeStreamSessionsCounter() {
        getTimers().startPeriodicTimer(Control.SCRAPE_STREAM_COUNTER, Control.SCRAPE_STREAM_COUNTER,
                streamingConfig.getSessionCounterScrapeInterval());
    }

    private void replyWebSocketConfig(final Control trigger) {
        getSender().tell(streamingConfig.getWebsocketConfig(), getSelf());
    }

    private void updateStreamingSessionsCounter(final Control trigger) {
        if (getContext() != null) {
            streamingSessionsCounter.set(
                    StreamSupport.stream(getContext().getChildren().spliterator(), false).count());
        }
    }

    /**
     * Control messages to send in the same actor system.
     */
    public enum Control {

        /**
         * Tell streaming actor to set the stream counter to its current number of child actors.
         */
        SCRAPE_STREAM_COUNTER,

        /**
         * Request the current WebSocket config.
         */
        RETRIEVE_WEBSOCKET_CONFIG
    }

}
