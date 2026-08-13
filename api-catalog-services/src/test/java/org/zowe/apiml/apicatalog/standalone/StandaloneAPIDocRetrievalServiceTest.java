/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.functional.ApiCatalogFunctionalTest;
import org.zowe.apiml.apicatalog.services.status.APIDocRetrievalService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StandaloneAPIDocRetrievalServiceTest {

    private final StandaloneAPIDocRetrievalService standaloneAPIDocRetrievalService = new StandaloneAPIDocRetrievalService();

    @Nested
    class ThenNothing {

        @Test
        void whenRetrieveApiDoc() {
            assertNull(standaloneAPIDocRetrievalService.retrieveApiDoc("service", null));
        }

        @Test
        void whenRetrieveDefaultApiDoc() {
            assertNull(standaloneAPIDocRetrievalService.retrieveDefaultApiDoc("service"));
        }

        @Test
        void whenRetrieveApiVersions() {
            List<String> apiVersions = standaloneAPIDocRetrievalService.retrieveApiVersions("service");
            assertTrue(apiVersions.isEmpty());
        }

        @Test
        void whenRetrieveDefaultApiVersion() {
            assertNull(standaloneAPIDocRetrievalService.retrieveDefaultApiVersion("service"));
        }
    }

    @Nested
    class ThenItDoesntAcceptNullValues {

        @Test
        void whenRetrieveApiDoc() {
            assertThrows(NullPointerException.class, () -> standaloneAPIDocRetrievalService.retrieveApiDoc(null, null));
        }

        @Test
        void whenRetrieveDefaultApiDoc() {
            assertThrows(NullPointerException.class, () -> standaloneAPIDocRetrievalService.retrieveDefaultApiDoc(null));
        }

        @Test
        void whenRetrieveApiVersions() {
            assertThrows(NullPointerException.class, () -> standaloneAPIDocRetrievalService.retrieveApiVersions((String) null));
        }

        @Test
        void whenRetrieveDefaultApiVersion() {
            assertThrows(NullPointerException.class, () -> standaloneAPIDocRetrievalService.retrieveDefaultApiVersion((String )null));
        }
    }

    @Nested
    class Certificate extends ApiCatalogFunctionalTest {

        @Autowired
        private APIDocRetrievalService apiDocRetrievalService;

        @Autowired
        @Qualifier("secureHttpClientWithKeystore")
        private CloseableHttpClient secureHttpClient;

        @Autowired
        @Qualifier("secureHttpClientWithoutKeystore")
        private CloseableHttpClient secureHttpClientWithoutKeystore;

        @Test
        void givenApiDocRetrievalServiceRest_whenOutboundCall_thenUsingClientCertificate() {
            CloseableHttpClient usedHttpClient = (CloseableHttpClient) ReflectionTestUtils.getField(apiDocRetrievalService, "secureHttpClient");
            assertSame(usedHttpClient, secureHttpClient);
            assertNotSame(usedHttpClient, secureHttpClientWithoutKeystore);
        }

    }

}
