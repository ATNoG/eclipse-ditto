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
package org.eclipse.ditto.thingsearch.service.persistence.write.model;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.conversions.Bson;
import org.eclipse.ditto.thingsearch.service.persistence.PersistenceConstants;
import org.eclipse.ditto.thingsearch.service.updater.actors.MongoWriteModel;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.WriteModel;

/**
 * Interface for write models of Thing changes for MongoDB.
 */
public abstract class AbstractWriteModel {

    /**
     * MongoDB operator for setting a field.
     */
    public static final String SET = "$set";

    private final Metadata metadata;

    /**
     * Initialize metadata of the write model.
     *
     * @param metadata the metadata.
     */
    protected AbstractWriteModel(final Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Convert this description of a Thing search index change into a MongoDB write model.
     *
     * @return MongoDB write model.
     */
    public abstract WriteModel<BsonDocument> toMongo();

    // TODO
    public Optional<MongoWriteModel> toIncrementalMongo(@Nullable final AbstractWriteModel previousWriteModel) {
        return Optional.of(MongoWriteModel.of(this, toMongo(), false));
    }

    /**
     * @return Metadata of this write model.
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Get the filter of this write model.
     *
     * @return filter on search index documents.
     */
    public Bson getFilter() {
        return Filters.eq(PersistenceConstants.FIELD_ID, new BsonString(metadata.getThingId().toString()));
    }

    /**
     * Check whether this update is a patch update based on a specific sequence number.
     *
     * @return Whether this is a patch update.
     */
    public boolean isPatchUpdate() {
        return false;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AbstractWriteModel that = (AbstractWriteModel) o;
        return Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                "metadata=" + metadata;
    }
}
