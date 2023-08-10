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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@AcceptanceTest
public class ValidateAPIControllerTest {

    @InjectMocks
    private ValidateAPIController validateAPIController;

    @Mock
    private VerificationOnboardService verificationOnboardService;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private MessageService messageService;


    ResponseEntity<String> result;


    private static final String WRONG_SERVICE_ID_KEY = "org.zowe.apiml.gateway.verifier.wrongServiceId";
    private static final String NO_METADATA_KEY = "org.zowe.apiml.gateway.verifier.noMetadata";
    private static final String NON_CONFORMANT_KEY = "org.zowe.apiml.gateway.verifier.nonConformant";

    @BeforeEach
    void setup() {
        when(discoveryClient.getServices()).thenReturn(new ArrayList<>(Collections.singleton("OnboardedService")));

        MessageService realService = new YamlMessageService("/gateway-log-messages.yml");

        when(messageService.createMessage(WRONG_SERVICE_ID_KEY, "ThisWillBeRemoved"))
            .thenReturn(realService.createMessage(WRONG_SERVICE_ID_KEY, "ThisWillBeRemoved"));
        when(messageService.createMessage(NO_METADATA_KEY, "ThisWillBeRemoved"))
            .thenReturn(realService.createMessage(NO_METADATA_KEY, "ThisWillBeRemoved"));
        when(messageService.createMessage(NON_CONFORMANT_KEY, "ThisWillBeRemoved"))
            .thenReturn(realService.createMessage(NON_CONFORMANT_KEY, "ThisWillBeRemoved"));

        result = null;
    }




    @Nested
    class GivenMetadata {
        @Test
        void whenEmpty_thenCorrectResponse() {
            HashMap<String, String> metadata = new HashMap<>();
            assertEquals("Cannot Retrieve MetaData", validateAPIController.metaDataCheck(metadata));
        }

        @Test
        void whenNotEmpty_thenCorrectResponse() {
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("key", "value");
            assertEquals("", validateAPIController.metaDataCheck(metadata));
        }
    }


    @Nested
    class GivenInstanceList {
        @Test
        void whenEmpty_thenCorrectResponse() {
            List<ServiceInstance> list = new ArrayList<>();
            assertTrue(validateAPIController.instanceCheck(list).contains("Cannot retrieve metadata"));
        }

    }


    @Nested
    class GivenValidEverything {

        @Mock
        ServiceInstance serviceInstance;

        @AfterEach
        void checkValidJson() {
            ObjectMapper mapper = new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            boolean valid;
            try {
                mapper.readTree(result.getBody());
                valid = true;
            } catch (JsonProcessingException e) {
                valid = false;
            }
            assertTrue(valid);
        }


        @Test
        void thenOkResponse() {
            String serviceId = "testservice";
            HashMap<String, String> mockMetadata = new HashMap<>();
            mockMetadata.put("key", "value");
            when(verificationOnboardService.checkOnboarding(serviceId)).thenReturn(true);
            when(discoveryClient.getInstances(serviceId)).thenReturn(new ArrayList<>(Collections.singleton(serviceInstance)));
            when(serviceInstance.getMetadata()).thenReturn(mockMetadata);
            result = validateAPIController.checkConformance(serviceId);
            assertEquals(HttpStatus.OK, result.getStatusCode());
        }

    }
}

