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
package org.eclipse.ditto.model.connectivity;

import java.net.URI;
import java.text.MessageFormat;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.model.base.common.HttpStatusCode;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeExceptionBuilder;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.JsonParsableException;

/**
 * Thrown if sending of an {@code Signal} to an external system failed.
 */
@Immutable
@JsonParsableException(errorCode = MessageSendingFailedException.ERROR_CODE)
public final class MessageSendingFailedException extends DittoRuntimeException implements ConnectivityException {

    /**
     * Error code of this exception.
     */
    public static final String ERROR_CODE = ERROR_CODE_PREFIX + "message.sending.failed";

    private static final HttpStatusCode DEFAULT_STATUS_CODE = HttpStatusCode.SERVICE_UNAVAILABLE;
    private static final String MESSAGE_TEMPLATE = "Failed to send message: {0}";
    private static final String DEFAULT_MESSAGE = "Failed to send message.";
    private static final String DEFAULT_DESCRIPTION = "Sending the message to an external system failed, " +
            "please check if your connection is configured properly and the target system is available and consuming " +
            "messages.";

    private MessageSendingFailedException(final DittoHeaders dittoHeaders,
            final HttpStatusCode statusCode,
            @Nullable final String message,
            @Nullable final String description,
            @Nullable final Throwable cause,
            @Nullable final URI href) {
        super(ERROR_CODE, statusCode, dittoHeaders, message, description, cause, href);
    }

    /**
     * A mutable builder for a {@code MessageSendingFailedException}.
     *
     * @return the builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Constructs a new {@code MessageSendingFailedException} object with given message.
     *
     * @param message detail message. This message can be later retrieved by the {@link #getMessage()} method.
     * @param dittoHeaders the headers of the command which resulted in this exception.
     * @return the new MessageSendingFailedException.
     */
    public static MessageSendingFailedException fromMessage(final String message, final DittoHeaders dittoHeaders) {
        return new Builder()
                .dittoHeaders(dittoHeaders)
                .message(message)
                .build();
    }

    /**
     * Constructs a new {@code MessageSendingFailedException} object with the exception message extracted from the given
     * JSON object.
     *
     * @param jsonObject the JSON to read the {@link JsonFields#MESSAGE} field from.
     * @param dittoHeaders the headers of the command which resulted in this exception.
     * @return the new MessageSendingFailedException.
     * @throws org.eclipse.ditto.json.JsonMissingFieldException if the {@code jsonObject} does not have the {@link
     * JsonFields#MESSAGE} field.
     */
    public static MessageSendingFailedException fromJson(final JsonObject jsonObject, final DittoHeaders dittoHeaders) {
        return new Builder()
                .dittoHeaders(dittoHeaders)
                .message(readMessage(jsonObject))
                .description(readDescription(jsonObject).orElse(DEFAULT_DESCRIPTION))
                .href(readHRef(jsonObject).orElse(null))
                .build();
    }

    /**
     * A mutable builder with a fluent API for a {@link MessageSendingFailedException}.
     */
    @NotThreadSafe
    public static final class Builder extends DittoRuntimeExceptionBuilder<MessageSendingFailedException> {

        private HttpStatusCode statusCode = DEFAULT_STATUS_CODE;

        private Builder() {
        }

        /**
         * Set the status code of this builder.
         *
         * @param statusCode the new status code.
         * @return this builder.
         * @since 1.2.0
         */
        public Builder statusCode(final HttpStatusCode statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public Builder cause(@Nullable final Throwable cause) {
            if (cause == null) {
                message(DEFAULT_MESSAGE);
            } else {
                super.cause(cause);
                message(MessageFormat.format(MESSAGE_TEMPLATE, cause.getMessage()));
            }
            return this;
        }

        @Override
        protected MessageSendingFailedException doBuild(final DittoHeaders dittoHeaders,
                @Nullable final String message,
                @Nullable final String description,
                @Nullable final Throwable cause,
                @Nullable final URI href) {
            return new MessageSendingFailedException(dittoHeaders, statusCode, message, description, cause, href);
        }
    }
}
