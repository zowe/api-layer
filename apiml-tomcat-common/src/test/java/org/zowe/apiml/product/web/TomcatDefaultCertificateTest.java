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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TomcatDefaultCertificateTest {

    private final SSLHostConfigCertificate cert = mock(SSLHostConfigCertificate.class);
    private final SSLHostConfig sslHostConfig = new SSLHostConfig();
    private final Connector connector = mock(Connector.class);

    @BeforeEach
    void setUp() {
        doReturn(new SSLHostConfig[]{sslHostConfig}).when(connector).findSslHostConfigs();
    }

    @Nested
    class NoCertStructure {

        @Test
        void thenDoNotUpdate() {
            new TomcatDefaultCertificate().customize(connector);
            assertNull(ReflectionTestUtils.getField(sslHostConfig, "defaultCertificate"));
        }

    }

    @Nested
    class InvalidDefaultCertStructure {

        @BeforeEach
        void setUp() {
            sslHostConfig.getCertificates().add(cert);
        }

        @Test
        void whenSSLHostConfigContainsACertificate() {
            assertNull(ReflectionTestUtils.getField(sslHostConfig, "defaultCertificate"));
            new TomcatDefaultCertificate().customize(connector);
            assertSame(cert, ReflectionTestUtils.getField(sslHostConfig, "defaultCertificate"));
        }

    }

}