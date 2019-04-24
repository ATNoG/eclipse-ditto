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
package org.eclipse.ditto.services.connectivity.messaging.config;

import static org.mutabilitydetector.unittesting.AllowedReason.provided;
import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.time.Duration;

import org.assertj.core.api.JUnitSoftAssertions;
import org.eclipse.ditto.services.base.config.supervision.ExponentialBackOffConfig;
import org.eclipse.ditto.services.base.config.supervision.SupervisorConfig;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit test for {@link org.eclipse.ditto.services.connectivity.messaging.config.DefaultConnectionConfig}.
 */
public final class DefaultConnectionConfigTest {

    private static Config connectionTestConf;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @BeforeClass
    public static void initTestFixture() {
        connectionTestConf = ConfigFactory.load("connection-test");
    }

    @Test
    public void assertImmutability() {
        assertInstancesOf(DefaultConnectionConfig.class,
                areImmutable(),
                provided(SupervisorConfig.class).isAlsoImmutable(),
                provided(ConnectionConfig.SnapshotConfig.class).isAlsoImmutable(),
                provided(ConnectionConfig.MqttConfig.class).isAlsoImmutable());
    }

    @Test
    public void testHashCodeAndEquals() {
        EqualsVerifier.forClass(DefaultConnectionConfig.class)
                .usingGetClass()
                .verify();
    }

    @Test
    public void testSerializationAndDeserialization() throws IOException, ClassNotFoundException {
        final DefaultConnectionConfig underTest = DefaultConnectionConfig.of(connectionTestConf);

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

    @Test
    public void underTestReturnsValuesOfConfigFile() {
        final DefaultConnectionConfig underTest = DefaultConnectionConfig.of(connectionTestConf);


        softly.assertThat(underTest.getClientActorAskTimeout())
                .as(ConnectionConfig.ConnectionConfigValue.CLIENT_ACTOR_ASK_TIMEOUT.getConfigPath())
                .isEqualTo(Duration.ofSeconds(10L));

        softly.assertThat(underTest.getFlushPendingResponsesTimeout())
                .as(ConnectionConfig.ConnectionConfigValue.FLUSH_PENDING_RESPONSES_TIMEOUT.getConfigPath())
                .isEqualTo(Duration.ofSeconds(2L));

        softly.assertThat(underTest.getSupervisorConfig())
                .as("supervisorConfig")
                .satisfies(supervisorConfig -> softly.assertThat(supervisorConfig.getExponentialBackOffConfig())
                        .as("exponentialBackOffConfig")
                        .satisfies(exponentialBackOffConfig -> {
                            softly.assertThat(exponentialBackOffConfig.getMin())
                                    .as(ExponentialBackOffConfig.ExponentialBackOffConfigValue.MIN.getConfigPath())
                                    .isEqualTo(Duration.ofSeconds(2L));
                            softly.assertThat(exponentialBackOffConfig.getMax())
                                    .as(ExponentialBackOffConfig.ExponentialBackOffConfigValue.MAX.getConfigPath())
                                    .isEqualTo(Duration.ofSeconds(50L));
                            softly.assertThat(exponentialBackOffConfig.getRandomFactor())
                                    .as(ExponentialBackOffConfig.ExponentialBackOffConfigValue.RANDOM_FACTOR.getConfigPath())
                                    .isEqualTo(0.1D);
                        }));

        softly.assertThat(underTest.getSnapshotConfig())
                .as("snapshotConfig")
                .satisfies(snapshotConfig -> softly.assertThat(snapshotConfig.getThreshold())
                        .as(ConnectionConfig.SnapshotConfig.SnapshotConfigValue.THRESHOLD.getConfigPath())
                        .isEqualTo(20)
                );

        softly.assertThat(underTest.getMqttConfig())
                .as("mqttConfig")
                .satisfies(mqttConfig -> softly.assertThat(mqttConfig.getSourceBufferSize())
                        .as(ConnectionConfig.MqttConfig.MqttConfigValue.SOURCE_BUFFER_SIZE.getConfigPath())
                        .isEqualTo(7)
                );
    }
}
