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
package org.eclipse.ditto.services.base.config;

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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit test for {@link org.eclipse.ditto.services.base.config.DefaultLimitsConfig}.
 */
public class DefaultLimitsConfigTest {

    private static Config limitsTestConf;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @BeforeClass
    public static void initTestFixture() {
        limitsTestConf = ConfigFactory.load("limits-test");
    }

    @Test
    public void assertImmutability() {
        assertInstancesOf(DefaultLimitsConfig.class,
                areImmutable());
    }

    @Test
    public void testHashCodeAndEquals() {
        EqualsVerifier.forClass(DefaultLimitsConfig.class)
                .usingGetClass()
                .verify();
    }

    @Test
    public void testSerializationAndDeserialization() throws IOException, ClassNotFoundException {
        final DefaultLimitsConfig underTest = DefaultLimitsConfig.of(limitsTestConf);

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
    public void underTestReturnsDefaultValuesIfBaseConfigWasEmpty() {
        final DefaultLimitsConfig underTest = DefaultLimitsConfig.of(ConfigFactory.empty());

        softly.assertThat(underTest.getThingsMaxSize())
                .as(LimitsConfig.LimitsConfigValue.THINGS_MAX_SIZE.getConfigPath())
                .isEqualTo(LimitsConfig.LimitsConfigValue.THINGS_MAX_SIZE.getDefaultValue());

        softly.assertThat(underTest.getPoliciesMaxSize())
                .as(LimitsConfig.LimitsConfigValue.POLICIES_MAX_SIZE.getConfigPath())
                .isEqualTo(LimitsConfig.LimitsConfigValue.POLICIES_MAX_SIZE.getDefaultValue());

        softly.assertThat(underTest.getMessagesMaxSize())
                .as(LimitsConfig.LimitsConfigValue.MESSAGES_MAX_SIZE.getConfigPath())
                .isEqualTo(LimitsConfig.LimitsConfigValue.MESSAGES_MAX_SIZE.getDefaultValue());

        softly.assertThat(underTest.getThingsSearchDefaultPageSize())
                .as(LimitsConfig.LimitsConfigValue.THINGS_SEARCH_DEFAULT_PAGE_SIZE.getConfigPath())
                .isEqualTo(LimitsConfig.LimitsConfigValue.THINGS_SEARCH_DEFAULT_PAGE_SIZE.getDefaultValue());

        softly.assertThat(underTest.getThingsSearchMaxPageSize())
                .as(LimitsConfig.LimitsConfigValue.THINGS_SEARCH_MAX_PAGE_SIZE.getConfigPath())
                .isEqualTo(LimitsConfig.LimitsConfigValue.THINGS_SEARCH_MAX_PAGE_SIZE.getDefaultValue());
    }

    @Test
    public void underTestReturnsValuesOfConfigFile() {
        final DefaultLimitsConfig underTest = DefaultLimitsConfig.of(limitsTestConf);

        softly.assertThat(underTest.getThingsMaxSize())
                .as(LimitsConfig.LimitsConfigValue.THINGS_MAX_SIZE.getConfigPath())
                .isEqualTo(204800);

        softly.assertThat(underTest.getPoliciesMaxSize())
                .as(LimitsConfig.LimitsConfigValue.POLICIES_MAX_SIZE.getConfigPath())
                .isEqualTo(204800);

        softly.assertThat(underTest.getMessagesMaxSize())
                .as(LimitsConfig.LimitsConfigValue.MESSAGES_MAX_SIZE.getConfigPath())
                .isEqualTo(358400);

        softly.assertThat(underTest.getThingsSearchDefaultPageSize())
                .as(LimitsConfig.LimitsConfigValue.THINGS_SEARCH_DEFAULT_PAGE_SIZE.getConfigPath())
                .isEqualTo(50);

        softly.assertThat(underTest.getThingsSearchMaxPageSize())
                .as(LimitsConfig.LimitsConfigValue.THINGS_SEARCH_MAX_PAGE_SIZE.getConfigPath())
                .isEqualTo(500);
    }
}
