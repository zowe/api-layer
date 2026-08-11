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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApiDocRetrievalServiceRestTest {

    @Nested
    @SpringBootTest
    class Certificate {

        @Autowired
        private ApiDocRetrievalServiceRest apiDocRetrievalServiceRest;

        @Autowired
        private WebClient webClient;

        @Autowired
        private WebClient webClientClientCert;

        @Test
        void givenApiDocRetrievalServiceRest_whenOutboundCall_thenUsingClientCertificate() {
            var webclient = (WebClient) ReflectionTestUtils.getField(apiDocRetrievalServiceRest, "webClientClientCert");
            assertSame(webclient, webClientClientCert);
            assertNotSame(webclient, webClient);
        }

    }

}
