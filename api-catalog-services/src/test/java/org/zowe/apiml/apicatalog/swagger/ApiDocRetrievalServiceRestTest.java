/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.apicatalog.functional.ApiCatalogFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApiDocRetrievalServiceRestTest {

    @Nested
    @TestPropertySource(properties = {
        "apiml.webClientConfig.enabled=true"
    })
    class Certificate extends ApiCatalogFunctionalTest {

        @Autowired
        private ApiDocRetrievalServiceRest apiDocRetrievalServiceRest;

        @Autowired
        private WebClient webClient;

        @Autowired
        @Qualifier("webClientClientCert")
        private WebClient webClientClientCert;

        @Test
        void givenApiDocRetrievalServiceRest_whenOutboundCall_thenUsingClientCertificate() {
            var usedWebClient = (WebClient) ReflectionTestUtils.getField(apiDocRetrievalServiceRest, "webClientClientCert");
            assertSame(usedWebClient, webClientClientCert);
            assertNotSame(usedWebClient, webClient);
        }

    }

}
