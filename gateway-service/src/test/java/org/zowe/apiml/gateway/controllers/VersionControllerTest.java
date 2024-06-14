/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.version.VersionInfo;
import org.zowe.apiml.product.version.VersionInfoDetails;
import org.zowe.apiml.product.version.VersionService;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = VersionController.class, excludeAutoConfiguration = { ReactiveSecurityAutoConfiguration.class })
@MockBean(MessageService.class)
class VersionControllerTest {

    @MockBean
    private VersionService versionService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void givenSpecificVersions_whenVersionEndpointCalled_thenVersionInfoShouldBeGivenInSuccessfulResponse() throws Exception {
        when(versionService.getVersion()).thenReturn(getDummyVersionInfo());
        this.webTestClient
            .get()
            .uri("/gateway/version")
            .exchange()
            .expectStatus()
                .is2xxSuccessful()
            .expectBody()
                .jsonPath("$.zowe.version").isEqualTo("0.0.0")
                .jsonPath("$.zowe.buildNumber").isEqualTo("000")
                .jsonPath("$.zowe.commitHash").isEqualTo("1a3b5c7")
                .jsonPath("$.apiml.version").isEqualTo("0.0.0")
                .jsonPath("$.apiml.buildNumber").isEqualTo("000")
                .jsonPath("$.apiml.commitHash").isEqualTo("1a3b5c7");
    }

    private VersionInfo getDummyVersionInfo() {
        VersionInfo versionInfo = new VersionInfo();
        VersionInfoDetails versionInfoDetails = new VersionInfoDetails("0.0.0", "000", "1a3b5c7");
        versionInfo.setZowe(versionInfoDetails);
        versionInfo.setApiml(versionInfoDetails);
        return versionInfo;
    }

}
