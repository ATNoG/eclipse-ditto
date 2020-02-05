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
package org.eclipse.ditto.protocoladapter;

import org.eclipse.ditto.signals.base.Signal;
import org.eclipse.ditto.signals.commands.base.Command;
import org.eclipse.ditto.signals.commands.base.CommandResponse;
import org.eclipse.ditto.signals.commands.messages.MessageCommand;
import org.eclipse.ditto.signals.commands.messages.MessageCommandResponse;
import org.eclipse.ditto.signals.commands.policies.PolicyCommandResponse;
import org.eclipse.ditto.signals.commands.policies.PolicyErrorResponse;
import org.eclipse.ditto.signals.commands.policies.modify.PolicyModifyCommand;
import org.eclipse.ditto.signals.commands.policies.modify.PolicyModifyCommandResponse;
import org.eclipse.ditto.signals.commands.policies.query.PolicyQueryCommand;
import org.eclipse.ditto.signals.commands.policies.query.PolicyQueryCommandResponse;
import org.eclipse.ditto.signals.commands.things.ThingCommandResponse;
import org.eclipse.ditto.signals.commands.things.ThingErrorResponse;
import org.eclipse.ditto.signals.commands.things.modify.ThingModifyCommand;
import org.eclipse.ditto.signals.commands.things.modify.ThingModifyCommandResponse;
import org.eclipse.ditto.signals.commands.things.query.ThingQueryCommand;
import org.eclipse.ditto.signals.commands.things.query.ThingQueryCommandResponse;
import org.eclipse.ditto.signals.events.base.Event;
import org.eclipse.ditto.signals.events.things.ThingEvent;

/**
 * A protocol adapter provides methods for mapping {@link Signal} instances to an {@link Adaptable}.
 */
public interface ProtocolAdapter {

    /**
     * Maps the given {@code Adaptable} to the corresponding {@code Signal}, which can be a {@code Command},
     * {@code CommandResponse} or an {@code Event}.
     *
     * @param adaptable the adaptable.
     * @return the Signal.
     */
    Signal<?> fromAdaptable(Adaptable adaptable);

    /**
     * Maps the given {@code Signal} to an {@code Adaptable}.
     *
     * @param signal the signal.
     * @return the adaptable.
     * @throws UnknownSignalException if the passed Signal was not supported by the ProtocolAdapter
     */
    Adaptable toAdaptable(Signal<?> signal);

    /**
     * Maps the given {@code CommandResponse} to an {@code Adaptable} assuming {@link TopicPath.Channel#TWIN}.
     *
     * @param commandResponse the response.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed CommandResponse was not supported by the ProtocolAdapter
     */
    default Adaptable toAdaptable(CommandResponse<?> commandResponse) {
        return toAdaptable(commandResponse, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code CommandResponse} to an {@code Adaptable}.
     *
     * @param commandResponse the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed CommandResponse was not supported by the ProtocolAdapter
     */
    Adaptable toAdaptable(CommandResponse<?> commandResponse, TopicPath.Channel channel);

    /**
     * Maps the given {@code ThingCommandResponse} to an {@code Adaptable}.
     *
     * @param thingCommandResponse the response.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed ThingCommandResponse was not supported by the
     * ProtocolAdapter
     */
    default Adaptable toAdaptable(ThingCommandResponse<?> thingCommandResponse) {
        return toAdaptable(thingCommandResponse, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code ThingCommandResponse} to an {@code Adaptable}.
     *
     * @param thingCommandResponse the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed ThingCommandResponse was not supported by the
     * ProtocolAdapter
     */
    Adaptable toAdaptable(ThingCommandResponse<?> thingCommandResponse, TopicPath.Channel channel);

    /**
     * Maps the given {@code messageCommand} to an {@code Adaptable}.
     *
     * @param messageCommand the messageCommand.
     * @return the adaptable.
     * @throws UnknownCommandException if the passed MessageCommand was not supported by the ProtocolAdapter
     */
    default Adaptable toAdaptable(MessageCommand<?, ?> messageCommand) {
        return toAdaptable(messageCommand, TopicPath.Channel.LIVE);
    }

    /**
     * Maps the given {@code messageCommandResponse} to an {@code Adaptable}.
     *
     * @param messageCommandResponse the messageCommandResponse.
     * @return the adaptable.
     * @throws UnknownCommandException if the passed MessageCommandResponse was not supported by the ProtocolAdapter
     */
    default Adaptable toAdaptable(MessageCommandResponse<?, ?> messageCommandResponse) {
        return toAdaptable(messageCommandResponse, TopicPath.Channel.LIVE);
    }

    /**
     * Maps the given {@code command} to an {@code Adaptable}.
     *
     * @param command the command.
     * @return the adaptable.
     * @throws UnknownCommandException if the passed Command was not supported by the ProtocolAdapter
     */
    Adaptable toAdaptable(Command<?> command);

    /**
     * Maps the given {@code command} to an {@code Adaptable}.
     *
     * @param command the command.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandException if the passed Command was not supported by the ProtocolAdapter
     */
    Adaptable toAdaptable(Command<?> command, TopicPath.Channel channel);

    /**
     * Maps the given {@code thingModifyCommand} to an {@code Adaptable}.
     *
     * @param thingModifyCommand the command.
     * @return the adaptable.
     */
    default Adaptable toAdaptable(ThingModifyCommand<?> thingModifyCommand) {
        return toAdaptable(thingModifyCommand, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code thingModifyCommand} to an {@code Adaptable}.
     *
     * @param thingModifyCommand the command.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     */
    Adaptable toAdaptable(ThingModifyCommand<?> thingModifyCommand, TopicPath.Channel channel);

    /**
     * Maps the given {@code thingModifyCommandResponse} to an {@code Adaptable}.
     *
     * @param thingModifyCommandResponse the response.
     * @return the adaptable.
     */
    default Adaptable toAdaptable(ThingModifyCommandResponse<?> thingModifyCommandResponse) {
        return toAdaptable(thingModifyCommandResponse, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code thingModifyCommandResponse} to an {@code Adaptable}.
     *
     * @param thingModifyCommandResponse the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     */
    Adaptable toAdaptable(ThingModifyCommandResponse<?> thingModifyCommandResponse,
            TopicPath.Channel channel);

    /**
     * Maps the given {@code thingQueryCommand} to an {@code Adaptable}.
     *
     * @param thingQueryCommand the command.
     * @return the adaptable.
     */
    default Adaptable toAdaptable(ThingQueryCommand<?> thingQueryCommand) {
        return toAdaptable(thingQueryCommand, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code thingQueryCommand} to an {@code Adaptable}.
     *
     * @param thingQueryCommand the command.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     */
    Adaptable toAdaptable(ThingQueryCommand<?> thingQueryCommand, TopicPath.Channel channel);

    /**
     * Maps the given {@code thingQueryCommandResponse} to an {@code Adaptable}.
     *
     * @param thingQueryCommandResponse the response.
     * @return the adaptable.
     */
    default Adaptable toAdaptable(ThingQueryCommandResponse<?> thingQueryCommandResponse) {
        return toAdaptable(thingQueryCommandResponse, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code thingQueryCommandResponse} to an {@code Adaptable}.
     *
     * @param thingQueryCommandResponse the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     */
    Adaptable toAdaptable(ThingQueryCommandResponse<?> thingQueryCommandResponse,
            TopicPath.Channel channel);

    /**
     * Maps the given {@code thingErrorResponse} to an {@code Adaptable}.
     *
     * @param thingErrorResponse the error response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     */
    Adaptable toAdaptable(ThingErrorResponse thingErrorResponse, TopicPath.Channel channel);

    /**
     * Maps the given {@code event} to an {@code Adaptable}.
     *
     * @param event the event.
     * @return the adaptable.
     * @throws UnknownEventException if the passed Event was not supported by the ProtocolAdapter
     */
    default Adaptable toAdaptable(Event<?> event) {
        return toAdaptable(event, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code event} to an {@code Adaptable}.
     *
     * @param event the event.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownEventException if the passed Event was not supported by the ProtocolAdapter
     */
    Adaptable toAdaptable(Event<?> event, TopicPath.Channel channel);

    /**
     * Maps the given {@code thingEvent} to an {@code Adaptable}.
     *
     * @param thingEvent the event.
     * @return the adaptable.
     */
    default Adaptable toAdaptable(ThingEvent<?> thingEvent) {
        return toAdaptable(thingEvent, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code thingEvent} to an {@code Adaptable}.
     *
     * @param thingEvent the event.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     */
    Adaptable toAdaptable(ThingEvent<?> thingEvent, TopicPath.Channel channel);

    /**
     * Maps the given {@code PolicyQueryCommand} to an {@code Adaptable}.
     *
     * @param policyQueryCommand the command.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed PolicyQueryCommand was not supported by the ProtocolAdapter
     */
    default Adaptable toAdaptable(PolicyQueryCommand<?> policyQueryCommand) {
        return toAdaptable(policyQueryCommand, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code PolicyQueryCommand} to an {@code Adaptable}.
     *
     * @param policyQueryCommand the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed PolicyQueryCommand was not supported by the ProtocolAdapter
     */
    Adaptable toAdaptable(PolicyQueryCommand<?> policyQueryCommand, TopicPath.Channel channel);

    /**
     * Maps the given {@code PolicyCommandResponse} to an {@code Adaptable}.
     *
     * @param policyCommandResponse the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed PolicyCommandResponse was not supported by the
     * ProtocolAdapter
     */
    Adaptable toAdaptable(PolicyCommandResponse<?> policyCommandResponse, TopicPath.Channel channel);

    /**
     * Maps the given {@code PolicyQueryCommandResponse} to an {@code Adaptable}.
     *
     * @param policyQueryCommandResponse the response.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed PolicyQueryCommandResponse was not supported by the
     * ProtocolAdapter
     */
    default Adaptable toAdaptable(PolicyQueryCommandResponse<?> policyQueryCommandResponse) {
        return toAdaptable(policyQueryCommandResponse, TopicPath.Channel.TWIN);
    }

    /**
     * Maps the given {@code PolicyQueryCommandResponse} to an {@code Adaptable}.
     *
     * @param policyQueryCommandResponse the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed PolicyQueryCommandResponse was not supported by the
     * ProtocolAdapter
     */
    Adaptable toAdaptable(PolicyQueryCommandResponse<?> policyQueryCommandResponse, TopicPath.Channel channel);

    /**
     * Maps the given {@code PolicyModifyCommand} to an {@code Adaptable}.
     *
     * @param policyModifyCommand the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed PolicyModifyCommand was not supported by the
     * ProtocolAdapter
     */
    Adaptable toAdaptable(PolicyModifyCommand<?> policyModifyCommand, TopicPath.Channel channel);

    /**
     * Maps the given {@code PolicyModifyCommandResponse} to an {@code Adaptable}.
     *
     * @param policyModifyCommandResponse the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed PolicyModifyCommandResponse was not supported by the
     * ProtocolAdapter
     */
    Adaptable toAdaptable(PolicyModifyCommandResponse<?> policyModifyCommandResponse, TopicPath.Channel channel);

    /**
     * Maps the given {@code PolicyErrorResponse} to an {@code Adaptable}.
     *
     * @param policyErrorResponse the response.
     * @param channel the Channel (Twin/Live) to use.
     * @return the adaptable.
     * @throws UnknownCommandResponseException if the passed PolicyErrorResponse was not supported by the
     * ProtocolAdapter
     */
    Adaptable toAdaptable(PolicyErrorResponse policyErrorResponse, TopicPath.Channel channel);

    /**
     * Retrieve the header translator responsible for this protocol adapter.
     *
     * @return the header translator.
     */
    HeaderTranslator headerTranslator();

    /**
     * Test whether a signal belongs to the live channel.
     *
     * @param signal the signal.
     * @return whether it is a live signal.
     */
    static boolean isLiveSignal(final Signal<?> signal) {
        return signal.getDittoHeaders().getChannel().filter(TopicPath.Channel.LIVE.getName()::equals).isPresent();
    }
}
