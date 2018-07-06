/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 *
 */
package org.eclipse.ditto.services.things.persistence.actors.strategies.events;

import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.signals.events.things.FeaturePropertyModified;

/**
 * TODO javadoc
 */
final class FeaturePropertyModifiedStrategy implements HandleStrategy<FeaturePropertyModified> {

    @Override
    public Thing handle(final FeaturePropertyModified event, final Thing thing, final long revision) {
        return thing.toBuilder()
                .setFeatureProperty(event.getFeatureId(), event.getPropertyPointer(), event.getPropertyValue())
                .setRevision(revision)
                .setModified(event.getTimestamp().orElse(null))
                .build();
    }

}
