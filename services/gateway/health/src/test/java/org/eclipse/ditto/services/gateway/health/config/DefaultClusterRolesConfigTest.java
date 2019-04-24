/*
 * Copyright (c) 2017-2018 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.gateway.health.config;

import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.assertj.core.api.JUnitSoftAssertions;
import org.eclipse.ditto.services.gateway.health.config.HealthCheckConfig.ClusterRolesConfig.ClusterRolesConfigValue;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit test for {@link org.eclipse.ditto.services.gateway.health.config.DefaultClusterRolesConfig}.
 */
public final class DefaultClusterRolesConfigTest {

    private static Config clusterRolesTestConfig;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @BeforeClass
    public static void initTestFixture() {
        clusterRolesTestConfig = ConfigFactory.load("cluster-roles-test");
    }

    @Test
    public void assertImmutability() {
        assertInstancesOf(DefaultClusterRolesConfig.class, areImmutable());
    }

    @Test
    public void testHashCodeAndEquals() {
        EqualsVerifier.forClass(DefaultClusterRolesConfig.class)
                .usingGetClass()
                .verify();
    }

    @Test
    public void testSerializationAndDeserialization() throws IOException, ClassNotFoundException {
        final DefaultClusterRolesConfig underTest = DefaultClusterRolesConfig.of(clusterRolesTestConfig);

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ObjectOutput objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(underTest);
        objectOutputStream.close();

        final byte[] underTestSerialized = byteArrayOutputStream.toByteArray();

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(underTestSerialized);
        final ObjectInput objectInputStream = new ObjectInputStream(byteArrayInputStream);
        final Object underTestDeserialized = objectInputStream.readObject();

        softly.assertThat(underTestDeserialized).isEqualTo(underTest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void underTestReturnsDefaultValuesIfBaseConfigWasEmpty() {
        final DefaultClusterRolesConfig underTest = DefaultClusterRolesConfig.of(ConfigFactory.empty());

        softly.assertThat(underTest.isEnabled())
                .as(ClusterRolesConfigValue.ENABLED.getConfigPath())
                .isEqualTo(ClusterRolesConfigValue.ENABLED.getDefaultValue());
        softly.assertThat(underTest.getExpectedClusterRoles())
                .as(ClusterRolesConfigValue.EXPECTED.getConfigPath())
                .containsOnlyElementsOf((Iterable) ClusterRolesConfigValue.EXPECTED.getDefaultValue());
    }

    @Test
    public void underTestReturnsValuesOfConfigFile() {
        final DefaultClusterRolesConfig underTest = DefaultClusterRolesConfig.of(clusterRolesTestConfig);

        softly.assertThat(underTest.isEnabled())
                .as(ClusterRolesConfigValue.ENABLED.getConfigPath())
                .isTrue();
        softly.assertThat(underTest.getExpectedClusterRoles())
                .as(ClusterRolesConfigValue.EXPECTED.getConfigPath())
                .containsOnly("foo", "bar", "baz");
    }

}