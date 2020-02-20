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
package org.eclipse.ditto.model.base.headers;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.eclipse.ditto.json.JsonArray;
import org.eclipse.ditto.model.base.exceptions.DittoHeaderInvalidException;
import org.eclipse.ditto.model.base.headers.entitytag.EntityTag;
import org.eclipse.ditto.model.base.headers.entitytag.EntityTagMatchers;

/**
 * Enumeration of definitions of well known Ditto Headers including their key and Java type.
 */
public enum DittoHeaderDefinition implements HeaderDefinition {

    /**
     * Header definition for Authorization Subjects value.
     * <p>
     * Key: {@code "ditto-auth-subjects"}, Java type: {@link JsonArray}.
     * </p>
     */
    AUTHORIZATION_SUBJECTS("ditto-auth-subjects", JsonArray.class, false, false),

    /**
     * Header definition for correlation Id value.
     * <p>
     * Key: {@code "correlation-id"}, Java type: String.
     * </p>
     */
    CORRELATION_ID("correlation-id", String.class, true, true),

    /**
     * Header definition for schema version value.
     * <p>
     * Key: {@code "version"}, Java type: {@code int}.
     * </p>
     */
    SCHEMA_VERSION("version", int.class, true, true),

    /**
     * Header definition for response required value.
     * <p>
     * Key: {@code "response-required"}, Java type: {@code boolean}.
     * </p>
     */
    RESPONSE_REQUIRED("response-required", boolean.class, true, true),

    /**
     * Header definition for dry run value.
     * <p>
     * Key: {@code "ditto-dry-run"}, Java type: {@code boolean}.
     * </p>
     */
    DRY_RUN("ditto-dry-run", boolean.class, false, false),

    /**
     * Header definition for read subjects value.
     * <p>
     * Key: {@code "read-subjects"}, Java type: {@link JsonArray}.
     * </p>
     */
    READ_SUBJECTS("ditto-read-subjects", JsonArray.class, false, false),

    /**
     * Header definition for subjects with revoked READ subjects.
     *
     * <p>
     * Key: {@code "read-revoked-subjects"}, Java type: {@link JsonArray}.
     * </p>
     * @since 1.1.0
     */
    READ_REVOKED_SUBJECTS("ditto-read-revoked-subjects", JsonArray.class, false, false),

    /**
     * Header definition for a signal's content-type.
     * <p>
     * Key: {@code "content-type"}, Java type: String.
     * </p>
     */
    CONTENT_TYPE("content-type", String.class, true, true),

    /**
     * Header definition for channel value meaning distinguishing between live/twin.
     * <p>
     * Key: {@code "ditto-channel"}, Java type: {@link String}.
     * </p>
     */
    CHANNEL("ditto-channel", String.class, false, false),

    /**
     * Header definition for origin value that is set to the id of the originating session.
     * <p>
     * Key: {@code "ditto-origin"}, Java type: {@link String}.
     * </p>
     */
    ORIGIN("ditto-origin", String.class, false, false),

    /**
     * Header definition for "ETag".
     * <p>
     * Key: {@code "ETag"}, Java type: {@link String}.
     * </p>
     */
    ETAG("ETag", EntityTag.class, String.class, false, true),

    /**
     * Header definition for "If-Match".
     * <p>
     * Key: {@code "If-Match"}, Java type: {@link String}.
     * </p>
     */
    IF_MATCH("If-Match", EntityTagMatchers.class, String.class, true, false),

    /**
     * Header definition for "If-None-Match".
     * <p>
     * Key: {@code "If-None-Match"}, Java type: {@link String}.
     * </p>
     */
    IF_NONE_MATCH("If-None-Match", EntityTagMatchers.class, String.class, true, false),

    /**
     * Header definition for the internal header "ditto-reply-target". This header is evaluated for responses to be
     * published.
     * <p>
     * Key: {@code "ditto-reply-target"}, Java type: {@link java.lang.Integer}.
     * </p>
     */
    REPLY_TARGET("ditto-reply-target", Integer.class, false, false),

    /**
     * Header definition for "ditto-inbound-payload-mapper".
     * <p>
     * Key: {@code "ditto-inbound-payload-mapper"}, Java type: {@link String}.
     * </p>
     */
    INBOUND_PAYLOAD_MAPPER("ditto-inbound-payload-mapper", String.class, false, false),

    /**
     * Header definition for the authorization subject that caused an event.
     * External header of the same name is always discarded.
     * <p>
     * Key: {@code "ditto-originator"}, Java type: {@link String}.
     * </p>
     */
    ORIGINATOR("ditto-originator", String.class, false, true),

    /**
     * Header definition for defining which acknowledgements ("ack") are requested for a command processed by Ditto.
     * <p>
     * Key: {@code "requested-acks"}, Java type: {@link JsonArray}.
     * </p>
     * @since 1.1.0
     */
    REQUESTED_ACKS("requested-acks", JsonArray.class, true, true),

    /**
     * Header definition for the timeout of a command or message.
     * <p>
     * Key: {@code "timeout"}, Java type: {@code String}.
     * </p>
     * @since 1.1.0
     */
    TIMEOUT("timeout", DittoDuration.class, String.class,true, true) {
        @SuppressWarnings({"squid:S2201", "ResultOfMethodCallIgnored"})
        @Override
        public void validateValue(@Nullable final CharSequence value) {
            super.validateValue(value);
            try {
                DittoDuration.fromTimeoutString(null != value ? value : "null");
            } catch (final NumberFormatException e) {
                throw DittoHeaderInvalidException.newInvalidTypeBuilder(TIMEOUT.key, String.valueOf(value), "duration").build();
            }
        }
    }
    ;

    /**
     * Map to speed up lookup of header definition by key.
     */
    private static final Map<CharSequence, DittoHeaderDefinition> VALUES_BY_KEY = Arrays.stream(values())
            .collect(Collectors.toMap(DittoHeaderDefinition::getKey, Function.identity()));

    private final String key;
    private final Class<?> type;
    private final Class<?> serializationType;
    private final boolean readFromExternalHeaders;
    private final boolean writeToExternalHeaders;

    /**
     * @param theKey the key used as key for header map.
     * @param theType the Java type of the header value which is associated with this definition's key.
     * @param readFromExternalHeaders whether Ditto reads this header from headers sent by externals.
     * @param writeToExternalHeaders whether Ditto publishes this header to externals.
     */
    DittoHeaderDefinition(final String theKey, final Class<?> theType, final boolean readFromExternalHeaders,
            final boolean writeToExternalHeaders) {
        this(theKey, theType, theType, readFromExternalHeaders, writeToExternalHeaders);
    }

    /**
     * @param theKey the key used as key for header map.
     * @param theType the Java type of the header value which is associated with this definition's key.
     * @param serializationType the type to which this header value should be serialized.
     * @param readFromExternalHeaders whether Ditto reads this header from headers sent by externals.
     * @param writeToExternalHeaders whether Ditto publishes this header to externals.
     */
    DittoHeaderDefinition(final String theKey, final Class<?> theType, final Class<?> serializationType,
            final boolean readFromExternalHeaders, final boolean writeToExternalHeaders) {
        key = theKey.toLowerCase();
        type = theType;
        this.serializationType = serializationType;
        this.readFromExternalHeaders = readFromExternalHeaders;
        this.writeToExternalHeaders = writeToExternalHeaders;
    }

    /**
     * Finds an appropriate {@code DittoHeaderKey} for the specified key.
     *
     * @param key the key to look up.
     * @return the DittoHeaderKey or an empty Optional.
     */
    public static Optional<HeaderDefinition> forKey(@Nullable final CharSequence key) {
        return Optional.ofNullable(VALUES_BY_KEY.get(key));
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Class getJavaType() {
        return type;
    }

    @Override
    public Class getSerializationType() {
        return serializationType;
    }

    @Override
    public boolean shouldReadFromExternalHeaders() {
        return readFromExternalHeaders;
    }

    @Override
    public boolean shouldWriteToExternalHeaders() {
        return writeToExternalHeaders;
    }

    @Override
    public String toString() {
        return getKey();
    }

}
