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
package org.eclipse.ditto.signals.commands.things.exceptions;

import java.net.URI;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.model.base.common.HttpStatusCode;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeExceptionBuilder;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.JsonParsableException;
import org.eclipse.ditto.model.things.ThingException;

/**
 * Thrown if either for a REST POST or PUT request for creating a Thing it was tried to set an explicit {@code thingId}
 * in the JSON body.
 */
@Immutable
@JsonParsableException(errorCode = ThingIdNotExplicitlySettableException.ERROR_CODE)
public final class ThingIdNotExplicitlySettableException extends DittoRuntimeException implements ThingException {

    /**
     * Error code of this exception.
     */
    public static final String ERROR_CODE = ERROR_CODE_PREFIX + "id.notsettable";

    private static final String MESSAGE_TEMPLATE_POST =
            "It is not allowed to provide a Thing ID in the request body for "
                    + "method POST. The method POST will generate the Thing ID by itself.";

    private static final String DEFAULT_DESCRIPTION_POST =
            "To provide your own Thing ID use a PUT request instead.";

    private static final String MESSAGE_TEMPLATE_PUT =
            "The Thing ID in the request body is not equal to the Thing ID in the request URL.";

    private static final String DEFAULT_DESCRIPTION_PUT =
            "Either delete the Thing ID from the request body or use the same Thing ID as in the request URL.";

    private static final String MESSAGE_TEMPLATE_DITTO_PROTOCOL =
            "The Thing ID in the thing JSON is not equal to the Thing ID in the topic path.";

    private static final String DEFAULT_DESCRIPTION_DITTO_PROTOCOL =
            "Either delete the Thing ID from the thing JSON or use the same Thing ID as in the topic path.";

    private static final long serialVersionUID = 5477658033219182854L;

    private ThingIdNotExplicitlySettableException(final DittoHeaders dittoHeaders,
            @Nullable final String message,
            @Nullable final String description,
            @Nullable final Throwable cause,
            @Nullable final URI href) {
        super(ERROR_CODE, HttpStatusCode.BAD_REQUEST, dittoHeaders, message, description, cause, href);
    }

    /**
     * A mutable builder for a {@code ThingIdNotExplicitlySettableException}.
     *
     * @param isPostMethod whether the exception is created for a POST request ({@code true}) or for a PUT request (
     * {@code false}).
     * @return the builder.
     * @deprecated this is legacy use where we only needed to distinguish between put and post. Now whe have a third
     * option "ditto protocol" as well.
     */
    public static Builder newBuilder(final boolean isPostMethod) {
        return isPostMethod ? forPostMethod() : forPutMethod();
    }

    public static Builder forPostMethod() {
        return new Builder(MESSAGE_TEMPLATE_POST, DEFAULT_DESCRIPTION_POST);
    }

    public static Builder forPutMethod() {
        return new Builder(MESSAGE_TEMPLATE_PUT, DEFAULT_DESCRIPTION_PUT);
    }

    public static Builder forDittoProtocol() {
        return new Builder(MESSAGE_TEMPLATE_DITTO_PROTOCOL, DEFAULT_DESCRIPTION_DITTO_PROTOCOL);
    }

    /**
     * Constructs a new {@code ThingIdNotExplicitlySettableException} object with the exception message extracted from
     * the
     * given JSON object.
     *
     * @param jsonObject the JSON to read the {@link JsonFields#MESSAGE} field from.
     * @param dittoHeaders the headers of the command which resulted in this exception.
     * @return the new ThingIdNotExplicitlySettableException.
     * @throws NullPointerException if any argument is {@code null}.
     * @throws org.eclipse.ditto.json.JsonMissingFieldException if the {@code jsonObject} does not have the {@link
     * JsonFields#MESSAGE} field.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonObject} was not in the expected
     * format.
     */
    public static ThingIdNotExplicitlySettableException fromJson(final JsonObject jsonObject,
            final DittoHeaders dittoHeaders) {
        return DittoRuntimeException.fromJson(jsonObject, dittoHeaders, new Builder());
    }

    /**
     * Constructs a new {@code ThingIdNotExplicitlySettableException} object with the given exception message.
     *
     * @param message detail message. This message can be later retrieved by the {@link #getMessage()} method.
     * @param dittoHeaders the headers of the command which resulted in this exception.
     * @return the new ThingIdNotExplicitlySettableException.
     * @deprecated This method will eventually be deleted, because it's not used anywhere.
     */
    public static ThingIdNotExplicitlySettableException fromMessage(final String message,
            final DittoHeaders dittoHeaders) {
        switch (message) {
            case MESSAGE_TEMPLATE_POST:
                return forPostMethod()
                        .dittoHeaders(dittoHeaders)
                        .build();
            case MESSAGE_TEMPLATE_PUT:
                return forPutMethod()
                        .dittoHeaders(dittoHeaders)
                        .build();
            case MESSAGE_TEMPLATE_DITTO_PROTOCOL:
                return forDittoProtocol()
                        .dittoHeaders(dittoHeaders)
                        .build();
            default:
                return new Builder(message, "")
                        .dittoHeaders(dittoHeaders)
                        .build();
        }
    }

    /**
     * A mutable builder with a fluent API for a {@link ThingIdNotExplicitlySettableException}.
     */
    @NotThreadSafe
    public static final class Builder extends DittoRuntimeExceptionBuilder<ThingIdNotExplicitlySettableException> {

        private Builder(final String message, final String description) {
            message(message);
            description(description);
        }

        private Builder() {
        }

        @Override
        protected ThingIdNotExplicitlySettableException doBuild(final DittoHeaders dittoHeaders,
                @Nullable final String message,
                @Nullable final String description,
                @Nullable final Throwable cause,
                @Nullable final URI href) {
            return new ThingIdNotExplicitlySettableException(dittoHeaders, message, description, cause, href);
        }
    }


}
