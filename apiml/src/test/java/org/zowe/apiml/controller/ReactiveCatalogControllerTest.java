/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ReactiveCatalogControllerTest {

    private ReactiveCatalogController controller;

    @BeforeEach
    void setUp() {
        controller = new ReactiveCatalogController();
    }

    @Test
    void testCatalogApi_redirect() {
        StepVerifier.create(controller.catalogApi())
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatusCode.valueOf(308), responseEntity.getStatusCode());
                assertEquals(List.of( "/apicatalog/api/v1/"), responseEntity.getHeaders().get(HttpHeaders.LOCATION));
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testCatalogApiIndex_redirect() {
        StepVerifier.create(controller.catalogApiIndex())
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatusCode.valueOf(308), responseEntity.getStatusCode());
                assertEquals(List.of("/apicatalog/api/v1/index.html"), responseEntity.getHeaders().get(HttpHeaders.LOCATION));
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testCatalogLogin_redirect() {
        StepVerifier.create(controller.catalogLogin())
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatusCode.valueOf(308), responseEntity.getStatusCode());
                assertEquals(List.of("/gateway/api/v1/auth/login"), responseEntity.getHeaders().get(HttpHeaders.LOCATION));
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testCatalogLogout_redirect() {
        StepVerifier.create(controller.catalogLogout())
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatusCode.valueOf(308), responseEntity.getStatusCode());
                assertEquals(List.of("/gateway/api/v1/auth/logout"), responseEntity.getHeaders().get(HttpHeaders.LOCATION));
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testCatalogQuery_redirect() {
        StepVerifier.create(controller.catalogQuery())
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatusCode.valueOf(308), responseEntity.getStatusCode());
                assertEquals(List.of("/gateway/api/v1/auth/query"), responseEntity.getHeaders().get(HttpHeaders.LOCATION));
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testCatalogUi_redirect() {
        StepVerifier.create(controller.catalogUi())
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatusCode.valueOf(308), responseEntity.getStatusCode());
                assertEquals(List.of("/apicatalog/ui/v1/"), responseEntity.getHeaders().get(HttpHeaders.LOCATION));
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testCatalogUiIndex_redirect() {
        StepVerifier.create(controller.catalogUiIndex())
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatusCode.valueOf(308), responseEntity.getStatusCode());
                assertEquals(List.of("/apicatalog/ui/v1/index.html"), responseEntity.getHeaders().get(HttpHeaders.LOCATION));
                return true;
            })
            .verifyComplete();
    }

}
