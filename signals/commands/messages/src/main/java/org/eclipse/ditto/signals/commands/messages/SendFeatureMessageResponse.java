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
package org.eclipse.ditto.signals.commands.messages;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonField;
import org.eclipse.ditto.json.JsonFieldDefinition;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonObjectBuilder;
import org.eclipse.ditto.model.base.common.HttpStatusCode;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.FieldType;
import org.eclipse.ditto.model.base.json.JsonParsableCommandResponse;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;
import org.eclipse.ditto.model.messages.Message;
import org.eclipse.ditto.model.things.ThingId;
import org.eclipse.ditto.signals.base.WithFeatureId;
import org.eclipse.ditto.signals.commands.base.CommandResponseJsonDeserializer;

/**
 * Command to send a response {@link Message} <em>FROM</em> a Feature answering to a {@link SendFeatureMessage}.
 *
 * @param <T> the type of the message's payload.
 */
@JsonParsableCommandResponse(type = SendFeatureMessageResponse.TYPE)
public final class SendFeatureMessageResponse<T>
        extends AbstractMessageCommandResponse<T, SendFeatureMessageResponse<T>>
        implements WithFeatureId {

    /**
     * The name of the {@code Message} wrapped by this {@code MessageCommand}.
     */
    public static final String NAME = "featureResponseMessage";

    /**
     * Type of this command.
     */
    public static final String TYPE = TYPE_PREFIX + NAME;

    private static final JsonFieldDefinition<String> JSON_FEATURE_ID =
            JsonFactory.newStringFieldDefinition("featureId", FieldType.REGULAR, JsonSchemaVersion.V_1,
                    JsonSchemaVersion.V_2);

    private final String featureId;

    private SendFeatureMessageResponse(final ThingId thingId,
            final String featureId,
            final Message<T> message,
            final HttpStatusCode responseStatusCode,
            final DittoHeaders dittoHeaders) {

        super(TYPE, thingId, message, responseStatusCode, dittoHeaders);
        this.featureId = requireNonNull(featureId, "The featureId cannot be null.");
    }

    @Override
    public SendFeatureMessageResponse setDittoHeaders(final DittoHeaders dittoHeaders) {
        return of(getThingEntityId(), getFeatureId(), getMessage(), getStatusCode(), dittoHeaders);
    }

    /**
     * Creates a new instance of {@code SendFeatureMessageResponse}.
     *
     * @param thingId the ID of the Thing to send the message from.
     * @param featureId the ID of the Feature to send the message from.
     * @param message the response message to send from the Thing.
     * @param responseStatusCode the optional status code of this response.
     * @param dittoHeaders the DittoHeaders of this message.
     * @param <T> the type of the message's payload.
     * @return new instance of {@code SendFeatureMessageResponse}.
     * @throws NullPointerException if any argument is {@code null}.
     * @deprecated Thing ID is now typed. Use
     * {@link #of(org.eclipse.ditto.model.things.ThingId, String, org.eclipse.ditto.model.messages.Message, org.eclipse.ditto.model.base.common.HttpStatusCode, org.eclipse.ditto.model.base.headers.DittoHeaders)}
     * instead.
     */
    @Deprecated
    public static <T> SendFeatureMessageResponse<T> of(final String thingId,
            final String featureId,
            final Message<T> message,
            final HttpStatusCode responseStatusCode,
            final DittoHeaders dittoHeaders) {

        return of(ThingId.of(thingId), featureId, message, responseStatusCode, dittoHeaders);
    }

    /**
     * Creates a new instance of {@code SendFeatureMessageResponse}.
     *
     * @param thingId the ID of the Thing to send the message from.
     * @param featureId the ID of the Feature to send the message from.
     * @param message the response message to send from the Thing.
     * @param responseStatusCode the optional status code of this response.
     * @param dittoHeaders the DittoHeaders of this message.
     * @param <T> the type of the message's payload.
     * @return new instance of {@code SendFeatureMessageResponse}.
     * @throws NullPointerException if any argument is {@code null}.
     */
    public static <T> SendFeatureMessageResponse<T> of(final ThingId thingId,
            final String featureId,
            final Message<T> message,
            final HttpStatusCode responseStatusCode,
            final DittoHeaders dittoHeaders) {

        return new SendFeatureMessageResponse<>(thingId, featureId, message, responseStatusCode, dittoHeaders);
    }

    /**
     * Creates a new {@code SendClaimMessageResponse} from a JSON string.
     *
     * @param jsonString the JSON string of which the SendClaimMessageResponse is to be created.
     * @param dittoHeaders the headers.
     * @return the command.
     * @throws NullPointerException if {@code jsonString} is {@code null}.
     * @throws IllegalArgumentException if {@code jsonString} is empty.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonString} was not in the expected
     * format.
     */
    public static SendFeatureMessageResponse<?> fromJson(final String jsonString, final DittoHeaders dittoHeaders) {
        return fromJson(JsonFactory.newObject(jsonString), dittoHeaders);
    }

    /**
     * Creates a new {@code SendClaimMessageResponse} from a JSON object.
     *
     * @param jsonObject the JSON object of which the SendClaimMessageResponse is to be created.
     * @param dittoHeaders the headers.
     * @return the command.
     * @throws NullPointerException if {@code jsonObject} is {@code null}.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonObject} was not in the expected
     * format.
     */
    public static SendFeatureMessageResponse<?> fromJson(final JsonObject jsonObject, final DittoHeaders dittoHeaders) {
        return new CommandResponseJsonDeserializer<SendFeatureMessageResponse<?>>(TYPE, jsonObject).deserialize(
                statusCode -> {
                    final String extractedThingId =
                            jsonObject.getValueOrThrow(MessageCommandResponse.JsonFields.JSON_THING_ID);
                    final ThingId thingId = ThingId.of(extractedThingId);
                    final String featureId = jsonObject.getValueOrThrow(JSON_FEATURE_ID);
                    final Message<?> message = deserializeMessageFromJson(jsonObject);

                    return of(thingId, featureId, message, statusCode, dittoHeaders);
                });
    }

    @Override
    public String getFeatureId() {
        return featureId;
    }

    @Override
    protected void appendPayload(final JsonObjectBuilder jsonObjectBuilder, final JsonSchemaVersion schemaVersion,
            final Predicate<JsonField> predicate) {

        super.appendPayload(jsonObjectBuilder, schemaVersion, predicate);

        jsonObjectBuilder.remove(MessageCommand.JsonFields.JSON_THING_ID);
        final JsonObject superBuild = jsonObjectBuilder.build();
        jsonObjectBuilder.removeAll();
        jsonObjectBuilder.set(MessageCommand.JsonFields.JSON_THING_ID, getThingEntityId().toString());
        jsonObjectBuilder.set(JSON_FEATURE_ID, getFeatureId());
        jsonObjectBuilder.setAll(superBuild);
    }

    @SuppressWarnings("squid:S109")
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hashCode(featureId);
        return result;
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SendFeatureMessageResponse<?> that = (SendFeatureMessageResponse<?>) obj;
        return that.canEqual(this) && Objects.equals(featureId, that.featureId) && super.equals(that);
    }

    @Override
    protected boolean canEqual(@Nullable final Object other) {
        return other instanceof SendFeatureMessageResponse;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + super.toString() + ", featureId=" + featureId + "]";
    }

}
