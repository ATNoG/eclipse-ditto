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
package org.eclipse.ditto.services.gateway.endpoints.directives.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.ditto.services.gateway.endpoints.EndpointTestConstants.KNOWN_CORRELATION_ID;
import static org.eclipse.ditto.services.gateway.endpoints.directives.auth.AuthorizationContextVersioningDirective.mapAuthorizationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.ditto.model.base.auth.AuthorizationContext;
import org.eclipse.ditto.model.base.auth.AuthorizationModelFactory;
import org.eclipse.ditto.model.base.auth.AuthorizationSubject;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;
import org.eclipse.ditto.model.policies.SubjectId;
import org.eclipse.ditto.model.policies.SubjectIssuer;
import org.eclipse.ditto.services.gateway.endpoints.EndpointTestBase;
import org.junit.Test;

import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.testkit.TestRoute;
import akka.http.javadsl.testkit.TestRouteResult;

/**
 * Tests {@link AuthorizationContextVersioningDirective}.
 */
public final class AuthorizationContextVersioningDirectiveTest extends EndpointTestBase {

    private static final String PATH = "/";

    @Test
    public void subjectIdsWithoutPrefixArePrependedForV1() {
        final AuthorizationContext authContextWithPrefixedSubjects = createAuthContextWithPrefixedSubjects();
        final Collection<AuthorizationSubject> expectedAuthorizationSubjects = new ArrayList<>();
        expectedAuthorizationSubjects.addAll(createAuthSubjectsWithoutPrefixes());
        expectedAuthorizationSubjects.addAll(authContextWithPrefixedSubjects.getAuthorizationSubjects());
        final AuthorizationContext expectedAuthorizationContext =
                AuthorizationModelFactory.newAuthContext(expectedAuthorizationSubjects);

        assertMapping(JsonSchemaVersion.V_1, authContextWithPrefixedSubjects, expectedAuthorizationContext);
    }

    @Test
    public void subjectIdsWithoutPrefixAreAppendedForV2() {
        final AuthorizationContext authContextWithPrefixedSubjects = createAuthContextWithPrefixedSubjects();
        final Collection<AuthorizationSubject> expectedAuthorizationSubjects = new ArrayList<>();
        expectedAuthorizationSubjects.addAll(authContextWithPrefixedSubjects.getAuthorizationSubjects());
        expectedAuthorizationSubjects.addAll(createAuthSubjectsWithoutPrefixes());
        final AuthorizationContext expectedAuthorizationContext =
                AuthorizationModelFactory.newAuthContext(expectedAuthorizationSubjects);

        assertMapping(JsonSchemaVersion.V_2, authContextWithPrefixedSubjects, expectedAuthorizationContext);
    }

    private void assertMapping(final JsonSchemaVersion apiVersion,
            final AuthorizationContext authContextWithPrefixedSubjects,
            final AuthorizationContext expectedAuthorizationContext) {

        final int expectedStatusCode = 200;
        final Route root = route(get(
                () -> complete(HttpResponse.create().withEntity(DEFAULT_DUMMY_ENTITY).withStatus(expectedStatusCode))));
        final AtomicReference<AuthorizationContext> mappedRef = new AtomicReference<>();
        final Route wrappedRoute = mapAuthorizationContext(KNOWN_CORRELATION_ID, apiVersion,
                authContextWithPrefixedSubjects,
                mappedAuthContext -> {
                    mappedRef.set(mappedAuthContext);
                    return root;
                });
        final TestRoute testRoute = testRoute(wrappedRoute);
        final TestRouteResult result = testRoute.run(HttpRequest.GET(PATH));

        assertThat(mappedRef.get()).isEqualTo(expectedAuthorizationContext);

        result.assertStatusCode(expectedStatusCode);
        result.assertEntity(DEFAULT_DUMMY_ENTITY);
    }

    private static List<AuthorizationSubject> createAuthSubjectsWithoutPrefixes() {
        return Collections.singletonList(
                AuthorizationSubject.newInstance(createTestSubjectIdWithoutIssuerPrefix(SubjectIssuer.GOOGLE)));
    }

    private static AuthorizationContext createAuthContextWithPrefixedSubjects() {
        final Iterable<AuthorizationSubject> authorizationSubjects = createPrefixedAuthSubjectsForAllIssuers();
        return AuthorizationModelFactory.newAuthContext(authorizationSubjects);
    }

    private static Iterable<AuthorizationSubject> createPrefixedAuthSubjectsForAllIssuers() {
        final SubjectIssuer issuer = SubjectIssuer.GOOGLE;
        final SubjectId subjectId = SubjectId.newInstance(issuer, createTestSubjectIdWithoutIssuerPrefix(issuer));
        return List.of(AuthorizationModelFactory.newAuthSubject(subjectId));
    }

    private static String createTestSubjectIdWithoutIssuerPrefix(final SubjectIssuer issuer) {
        return "test-subject-" + issuer;
    }

}
