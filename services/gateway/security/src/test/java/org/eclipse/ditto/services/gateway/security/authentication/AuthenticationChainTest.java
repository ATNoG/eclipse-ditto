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
package org.eclipse.ditto.services.gateway.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.eclipse.ditto.model.base.auth.AuthorizationContext;
import org.eclipse.ditto.model.base.auth.AuthorizationSubject;
import org.eclipse.ditto.model.base.auth.DittoAuthorizationContextType;
import org.eclipse.ditto.model.base.common.HttpStatusCode;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.Uri;
import akka.http.javadsl.server.RequestContext;

/**
 * Unit test {@link AuthenticationChain}.
 */
@RunWith(MockitoJUnitRunner.class)
public final class AuthenticationChainTest {

    private static final DittoHeaders KNOWN_DITTO_HEADERS = DittoHeaders.newBuilder()
            .correlationId(UUID.randomUUID().toString())
            .build();
    private static final AuthorizationContext KNOWN_AUTHORIZATION_CONTEXT = AuthorizationContext.newInstance(
            DittoAuthorizationContextType.UNSPECIFIED,
            AuthorizationSubject.newInstance("issuer:subject")
    );

    private static Executor messageDispatcher;

    @Mock
    private AuthenticationProvider authenticationProviderA;
    @Mock
    private AuthenticationProvider authenticationProviderB;
    @Mock
    private AuthenticationFailureAggregator authenticationFailureAggregator;

    @BeforeClass
    public static void initTestFixture() {
        messageDispatcher = Executors.newFixedThreadPool(8);
    }

    @Test
    public void getInstanceThrowsIllegalArgumentExceptionWhenProviderListIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () -> AuthenticationChain.getInstance(Collections.emptyList(), authenticationFailureAggregator,
                                messageDispatcher))
                .withMessage("The argument 'authenticationProviders' must not be empty!")
                .withNoCause();
    }

    @Test
    public void authenticate() throws ExecutionException, InterruptedException {
        final RequestContext requestContextMock = mockRequestContextForAuthenticate();
        final String correlationId = getRandomUuid();
        final AuthenticationResult expectedAuthenticationResult =
                DefaultAuthenticationResult.successful(KNOWN_DITTO_HEADERS, KNOWN_AUTHORIZATION_CONTEXT);
        when(authenticationProviderA.isApplicable(requestContextMock)).thenReturn(false);
        when(authenticationProviderB.isApplicable(requestContextMock)).thenReturn(true);
        when(authenticationProviderB.authenticate(requestContextMock, KNOWN_DITTO_HEADERS))
                .thenReturn(expectedAuthenticationResult);
        final AuthenticationChain underTest =
                AuthenticationChain.getInstance(Arrays.asList(authenticationProviderA, authenticationProviderB),
                        authenticationFailureAggregator,
                        messageDispatcher);

        final AuthenticationResult authenticationResult =
                underTest.authenticate(requestContextMock, KNOWN_DITTO_HEADERS).get();

        verify(authenticationProviderA).isApplicable(requestContextMock);
        verify(authenticationProviderB).isApplicable(requestContextMock);
        verify(authenticationProviderA, never()).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        verify(authenticationProviderB).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        assertThat(authenticationResult).isEqualTo(expectedAuthenticationResult);
    }

    @Test
    public void authenticateReturnsFirstAuthenticationResultIfMultipleProvidersAreApplicable()
            throws ExecutionException, InterruptedException {

        final RequestContext requestContextMock = mockRequestContextForAuthenticate();
        final String correlationId = getRandomUuid();
        final AuthenticationResult expectedAuthenticationResult =
                DefaultAuthenticationResult.successful(KNOWN_DITTO_HEADERS, KNOWN_AUTHORIZATION_CONTEXT);
        when(authenticationProviderA.isApplicable(requestContextMock)).thenReturn(true);
        when(authenticationProviderA.authenticate(requestContextMock, KNOWN_DITTO_HEADERS))
                .thenReturn(expectedAuthenticationResult);
        final AuthenticationChain underTest =
                AuthenticationChain.getInstance(Arrays.asList(authenticationProviderA, authenticationProviderB),
                        authenticationFailureAggregator, messageDispatcher);

        final AuthenticationResult authenticationResult =
                underTest.authenticate(requestContextMock, KNOWN_DITTO_HEADERS).get();

        verify(authenticationProviderA).isApplicable(requestContextMock);
        verify(authenticationProviderB, never()).isApplicable(requestContextMock);
        verify(authenticationProviderA).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        verify(authenticationProviderB, never()).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        assertThat(authenticationResult).isEqualTo(expectedAuthenticationResult);
    }

    @Test
    public void authenticateReturnsIllegalStateAuthenticationResultIfCalledWhenNoProvidersAreApplicable()
            throws ExecutionException, InterruptedException {

        final RequestContext requestContextMock = mockRequestContextForAuthenticate();
        final String correlationId = getRandomUuid();
        final IllegalStateException expectedException = new IllegalStateException("No applicable authentication " +
                "provider was found!");
        when(authenticationProviderA.isApplicable(requestContextMock)).thenReturn(false);
        when(authenticationProviderB.isApplicable(requestContextMock)).thenReturn(false);
        final AuthenticationChain underTest =
                AuthenticationChain.getInstance(Arrays.asList(authenticationProviderA, authenticationProviderB),
                        authenticationFailureAggregator,
                        messageDispatcher);

        final AuthenticationResult authenticationResult =
                underTest.authenticate(requestContextMock, KNOWN_DITTO_HEADERS).get();

        verify(authenticationProviderA).isApplicable(requestContextMock);
        verify(authenticationProviderB).isApplicable(requestContextMock);
        verify(authenticationProviderA, never()).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        verify(authenticationProviderB, never()).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        assertThat(authenticationResult.isSuccess()).isFalse();
        assertThat(authenticationResult.getReasonOfFailure()).isInstanceOf(expectedException.getClass());
        assertThat(authenticationResult.getReasonOfFailure()).hasMessage(expectedException.getMessage());
        assertThat(authenticationResult.getReasonOfFailure()).hasNoCause();
    }

    @Test
    public void authenticateReturnsFailedAuthenticationResult() throws ExecutionException, InterruptedException {
        final RequestContext requestContextMock = mockRequestContextForAuthenticate();
        final String correlationId = getRandomUuid();
        final AuthenticationResult expectedAuthenticationResult =
                DefaultAuthenticationResult.failed(KNOWN_DITTO_HEADERS, mock(DittoRuntimeException.class));
        when(authenticationProviderA.isApplicable(requestContextMock)).thenReturn(true);
        when(authenticationProviderB.isApplicable(requestContextMock)).thenReturn(false);
        when(authenticationProviderA.authenticate(requestContextMock, KNOWN_DITTO_HEADERS))
                .thenReturn(expectedAuthenticationResult);
        final AuthenticationChain underTest =
                AuthenticationChain.getInstance(Arrays.asList(authenticationProviderA, authenticationProviderB),
                        authenticationFailureAggregator,
                        messageDispatcher);

        final AuthenticationResult authenticationResult =
                underTest.authenticate(requestContextMock, KNOWN_DITTO_HEADERS).get();

        verify(authenticationProviderA).isApplicable(requestContextMock);
        verify(authenticationProviderB).isApplicable(requestContextMock);
        verify(authenticationProviderA).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        verify(authenticationProviderB, never()).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        assertThat(authenticationResult).isEqualTo(expectedAuthenticationResult);
    }

    @Test
    public void authenticateReturnsSuccessfulAuthenticationResultIfOneProviderSucceeds()
            throws ExecutionException, InterruptedException {

        final RequestContext requestContextMock = mockRequestContextForAuthenticate();
        final String correlationId = getRandomUuid();
        final AuthenticationResult failedAuthenticationResult =
                DefaultAuthenticationResult.failed(KNOWN_DITTO_HEADERS, mock(DittoRuntimeException.class));
        final AuthenticationResult expectedAuthenticationResult =
                DefaultAuthenticationResult.successful(KNOWN_DITTO_HEADERS, KNOWN_AUTHORIZATION_CONTEXT);
        when(authenticationProviderA.isApplicable(requestContextMock)).thenReturn(true);
        when(authenticationProviderB.isApplicable(requestContextMock)).thenReturn(true);
        when(authenticationProviderA.authenticate(requestContextMock, KNOWN_DITTO_HEADERS))
                .thenReturn(failedAuthenticationResult);
        when(authenticationProviderB.authenticate(requestContextMock, KNOWN_DITTO_HEADERS))
                .thenReturn(expectedAuthenticationResult);
        final AuthenticationChain underTest =
                AuthenticationChain.getInstance(Arrays.asList(authenticationProviderA, authenticationProviderB),
                        authenticationFailureAggregator,
                        messageDispatcher);

        final AuthenticationResult authenticationResult =
                underTest.authenticate(requestContextMock, KNOWN_DITTO_HEADERS).get();

        verify(authenticationProviderA).isApplicable(requestContextMock);
        verify(authenticationProviderB).isApplicable(requestContextMock);
        verify(authenticationProviderA).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        verify(authenticationProviderB).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        assertThat(authenticationResult).isEqualTo(expectedAuthenticationResult);
    }

    @Test
    public void authenticateAggregatesFailureDescriptionsIfMultipleProvidersFail()
            throws ExecutionException, InterruptedException {

        final RequestContext requestContextMock = mockRequestContextForAuthenticate();
        final String correlationId = getRandomUuid();

        // Failure A
        final DittoRuntimeException failureA = mock(DittoRuntimeException.class);
        final AuthenticationResult failedAuthenticationResultA =
                DefaultAuthenticationResult.failed(KNOWN_DITTO_HEADERS, failureA);
        when(authenticationProviderA.isApplicable(requestContextMock)).thenReturn(true);
        when(authenticationProviderA.authenticate(requestContextMock, KNOWN_DITTO_HEADERS))
                .thenReturn(failedAuthenticationResultA);
        // Failure B
        final DittoRuntimeException failureB = mock(DittoRuntimeException.class);
        final AuthenticationResult failedAuthenticationResultB =
                DefaultAuthenticationResult.failed(KNOWN_DITTO_HEADERS, failureB);
        when(authenticationProviderB.isApplicable(requestContextMock)).thenReturn(true);

        when(authenticationProviderB.authenticate(requestContextMock, KNOWN_DITTO_HEADERS)).thenReturn(
                failedAuthenticationResultB);
        final AuthenticationChain underTest =
                AuthenticationChain.getInstance(Arrays.asList(authenticationProviderA, authenticationProviderB),
                        authenticationFailureAggregator, messageDispatcher);

        final List<AuthenticationResult> failedAuthenticationResult =
                Arrays.asList(failedAuthenticationResultA, failedAuthenticationResultB);
        when(authenticationFailureAggregator.aggregateAuthenticationFailures(failedAuthenticationResult))
                .thenReturn(DittoRuntimeException.newBuilder("test:exception", HttpStatusCode.UNAUTHORIZED).build());

        final AuthenticationResult authenticationResult =
                underTest.authenticate(requestContextMock, KNOWN_DITTO_HEADERS).get();

        verify(authenticationProviderA).isApplicable(requestContextMock);
        verify(authenticationProviderB).isApplicable(requestContextMock);
        verify(authenticationProviderA).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        verify(authenticationProviderB).authenticate(requestContextMock, KNOWN_DITTO_HEADERS);
        verify(authenticationFailureAggregator).aggregateAuthenticationFailures(failedAuthenticationResult);
        assertThat(authenticationResult.isSuccess()).isFalse();
    }

    private static String getRandomUuid() {
        return UUID.randomUUID().toString();
    }

    private static RequestContext mockRequestContextForAuthenticate() {
        final HttpRequest httpRequest = mock(HttpRequest.class);
        final RequestContext requestContext = mock(RequestContext.class);
        final Uri expectedUri = Uri.create("https://test.org");
        when(httpRequest.getUri()).thenReturn(expectedUri);
        when(requestContext.getRequest()).thenReturn(httpRequest);

        return requestContext;
    }

}
