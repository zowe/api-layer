/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiCatalogUiSecurityHeaderTest {

    @Autowired
    private WebTestClient webTestClient;
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    @Test
    @DisplayName("Should serve Content-Security-Policy header on API Catalog UI index page")
    void shouldReturnContentSecurityPolicyHeaderForUiIndex() {
        webTestClient.get()
            .uri("/apicatalog/ui/v1/index.html")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueMatches(CONTENT_SECURITY_POLICY, ".*default-src 'self'.*")
            .expectHeader().valueEquals("X-Frame-Options", "SAMEORIGIN")
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff");
    }

    @Test
    @DisplayName("Should serve Content-Security-Policy header on API Catalog UI root path")
    void shouldReturnContentSecurityPolicyHeaderForUiRootPath() {
        webTestClient.get()
            .uri("/apicatalog/ui/v1/")
            .exchange()
            .expectHeader().valueMatches(CONTENT_SECURITY_POLICY, ".*default-src 'self'.*");
    }
}
