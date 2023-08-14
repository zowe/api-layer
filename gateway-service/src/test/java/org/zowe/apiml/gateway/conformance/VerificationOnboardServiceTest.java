/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationOnboardServiceTest {

    @InjectMocks
    private VerificationOnboardService verificationOnboardService;

    @Mock
    private DiscoveryClient discoveryClient;


    @Test
    void whenCheckingOnboardedService() {
        when(discoveryClient.getServices()).thenReturn(new ArrayList<>(Collections.singleton("OnboardedService")));
        assertFalse(verificationOnboardService.checkOnboarding("Test"));
        assertTrue(verificationOnboardService.checkOnboarding("OnboardedService"));
    }

    @Test
    void whenRetrievingSwagger() {
        final String swaggerUrl = "https://hostname/sampleclient/api-doc";
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.api-v2.swaggerUrl", swaggerUrl);
        assertEquals(swaggerUrl, verificationOnboardService.retrieveSwagger(metadata));
    }


    @Test
    void whenRetrievingEmptySwagger() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.api-v2.swaggerUrl", null);
        assertEquals("", verificationOnboardService.retrieveSwagger(metadata));
    }

}

