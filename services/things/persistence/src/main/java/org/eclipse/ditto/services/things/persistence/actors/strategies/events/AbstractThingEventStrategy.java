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
package org.eclipse.ditto.services.things.persistence.actors.strategies.events;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.model.things.Metadata;
import org.eclipse.ditto.model.things.MetadataBuilder;
import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.model.things.ThingBuilder;
import org.eclipse.ditto.services.utils.persistentactors.events.EventStrategy;
import org.eclipse.ditto.signals.events.things.ThingEvent;

/**
 * This abstract implementation of {@code EventStrategy} checks if the Thing to be handled is {@code null}.
 * If the Thing is {@code null} the {@code handle} method returns with {@code null}; otherwise a ThingBuilder will be
 * derived from the Thing with the revision and modified timestamp set.
 * This builder is then passed to the
 * {@link #applyEvent(T, org.eclipse.ditto.model.things.ThingBuilder.FromCopy)}
 * method for further handling.
 * However, sub-classes are free to implement the {@code handle} method directly and thus completely circumvent the
 * {@code applyEvent} method.
 *
 * @param <T> the type of the handled ThingEvent.
 */
@Immutable
abstract class AbstractThingEventStrategy<T extends ThingEvent<T>> implements EventStrategy<T, Thing> {

    /**
     * Constructs a new {@code AbstractThingEventStrategy} object.
     */
    protected AbstractThingEventStrategy() {
        super();
    }

    @Nullable
    @Override
    public Thing handle(final T event, @Nullable final Thing thing, final long revision) {
        if (null != thing) {
            ThingMetadataFactory thingMetadataFactory = new ThingMetadataFactory();
            ThingBuilder.FromCopy thingBuilder = thing.toBuilder()
                    .setRevision(revision)
                    .setModified(event.getTimestamp().orElse(null))
                    .setMetadata(JsonPointer.empty(), thingMetadataFactory.buildFromEvent(event, thing));
            thingBuilder = applyEvent(event, thingBuilder);
            return thingBuilder.build();
        }
        return null;
    }

    /**
     * Apply the specified event to the also specified ThingBuilder. The builder has already the specified revision
     * set as well as the event's timestamp.
     *
     * @param event the ThingEvent to be applied.
     * @param thingBuilder builder which is derived from the {@code event}'s Thing with the revision and event
     * timestamp already set.
     * @return the updated {@code thingBuilder} after applying {@code event}.
     */
    protected ThingBuilder.FromCopy applyEvent(final T event, final ThingBuilder.FromCopy thingBuilder) {
        return thingBuilder;
    }

}
