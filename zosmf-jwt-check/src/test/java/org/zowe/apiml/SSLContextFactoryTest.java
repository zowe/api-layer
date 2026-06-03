/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SSLContextFactoryTest {

    private static final String TRUSTSTORE_PATH = "../keystore/localhost/localhost.truststore.p12";
    private static final String KEYSTORE_PATH = "../keystore/localhost/localhost.keystore.p12";
    private static final String PASSWORD = "password";

    @Nested
    class InitSSLContextWithRealStores {

        @Test
        void createsNonNullSSLContext() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            SSLContextFactory factory = SSLContextFactory.initSSLContext(stores);

            assertNotNull(factory);
            assertNotNull(factory.getSslContext());
        }

        @Test
        void sslContextProtocolIsTLS() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            SSLContextFactory factory = SSLContextFactory.initSSLContext(stores);

            assertEquals("TLSv1.2", factory.getSslContext().getProtocol());
        }

        @Test
        void worksWithKeystoreAndTruststore() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(KEYSTORE_PATH, PASSWORD, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            SSLContextFactory factory = SSLContextFactory.initSSLContext(stores);

            assertNotNull(factory.getSslContext());
        }

        @Test
        void worksWithoutKeystore() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            SSLContextFactory factory = SSLContextFactory.initSSLContext(stores);

            SSLContext ctx = factory.getSslContext();
            assertNotNull(ctx.getSocketFactory());
        }
    }

    @Nested
    class InitTrustAllSSLContext {

        @Test
        void createsNonNullSSLContext() throws Exception {
            SSLContextFactory factory = SSLContextFactory.initTrustAllSSLContext();

            assertNotNull(factory);
            assertNotNull(factory.getSslContext());
        }

        @Test
        void sslContextProtocolIsTLS() throws Exception {
            SSLContextFactory factory = SSLContextFactory.initTrustAllSSLContext();

            assertEquals("TLSv1.2", factory.getSslContext().getProtocol());
        }

        @Test
        void socketFactoryIsAvailable() throws Exception {
            SSLContextFactory factory = SSLContextFactory.initTrustAllSSLContext();

            assertNotNull(factory.getSslContext().getSocketFactory());
        }
    }

    private ZosmfJwtCheckConfig createConf(String keyStore, String keyStorePassword,
                                           String trustStore, String trustStorePassword,
                                           String storeType) {
        ZosmfJwtCheckConfig conf = mock(ZosmfJwtCheckConfig.class);
        when(conf.getKeyStore()).thenReturn(keyStore);
        when(conf.getKeyStorePassword()).thenReturn(keyStorePassword);
        when(conf.getKeyStoreType()).thenReturn(storeType);
        when(conf.getTrustStore()).thenReturn(trustStore);
        when(conf.getTrustStorePassword()).thenReturn(trustStorePassword);
        when(conf.getTrustStoreType()).thenReturn(storeType);
        return conf;
    }
}
