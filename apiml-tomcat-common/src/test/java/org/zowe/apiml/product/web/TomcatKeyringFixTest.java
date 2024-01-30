/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TomcatKeyringFixTest {

    private static final String PASSWORD = "password";

    private SSLHostConfig sslHostConfig = mock(SSLHostConfig.class);

    @BeforeEach
    void setUp() {
    }

    private Connector getConnector() {
        Connector connector = mock(Connector.class);
        doReturn(new SSLHostConfig[]{sslHostConfig}).when(connector).findSslHostConfigs();
        return connector;
    }

    private Connector customize(
        String keyStore, char[] keyStorePassword, char[] keyPassword,
        String trustStore, char[] trustStorePassword
    ) {
        TomcatKeyringFix customizer = new TomcatKeyringFix();
        ReflectionTestUtils.setField(customizer, "keyStore", keyStore);
        ReflectionTestUtils.setField(customizer, "keyStorePassword", keyStorePassword);
        ReflectionTestUtils.setField(customizer, "keyPassword", keyPassword);
        ReflectionTestUtils.setField(customizer, "trustStore", trustStore);
        ReflectionTestUtils.setField(customizer, "trustStorePassword", trustStorePassword);

        Connector connector = getConnector();
        customizer.customize(connector);
        return connector;
    }

    @Nested
    class UsingKeyring {

        @Test
        void whenInvalidFormatAndMissingPassword_thenFixIt() {
            customize("safkeyring:///userId/ringIdKs", null, null, "safkeyringpce:////userId/ringIdTs", null);
            verify(sslHostConfig).addCertificate(any(SSLHostConfigCertificate.class));
            verify(sslHostConfig).setTruststoreFile("safkeyringpce://userId/ringIdTs");
            verify(sslHostConfig).setTruststorePassword(PASSWORD);
        }

    }

    @Nested
    class UsingKeystore {

        @Test
        void dontUpdate() {
            customize("/somePath", null, null, "/anotherPath", null);
            verify(sslHostConfig, never()).addCertificate(any());
            verify(sslHostConfig, never()).setTruststoreFile(any());
            verify(sslHostConfig, never()).setTruststorePassword(any());
        }

    }

    @Nested
    class InvalidDefaultCertStructure {

        @Test
        void whenSSLHostConfigContainsACertificate() {
            SSLHostConfigCertificate cert = mock(SSLHostConfigCertificate.class);
            sslHostConfig = new SSLHostConfig();
            sslHostConfig.getCertificates().add(cert);
            assertNull(ReflectionTestUtils.getField(sslHostConfig, "defaultCertificate"));
            customize("safkeyring:///userId/ringIdKs", null, null, "safkeyringpce:////userId/ringIdTs", null);
            assertSame(cert, ReflectionTestUtils.getField(sslHostConfig, "defaultCertificate"));
        }

    }

}
