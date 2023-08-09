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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@AcceptanceTest
public class ValidateAPIControllerTest {

    @Autowired
    private ValidateAPIController validateAPIController;

    @MockBean
    private VerificationOnboardService verificationOnboardService;

    @MockBean
    private DiscoveryClient discoveryClient;


    ResponseEntity<String> result;


    @BeforeEach
    void setup() {
        MessageService messageService = new YamlMessageService("/gateway-log-messages.yml");
        validateAPIController = new ValidateAPIController(messageService, verificationOnboardService, discoveryClient);
        standaloneSetup(validateAPIController);
        when(discoveryClient.getServices()).thenReturn(new ArrayList<>(Collections.singleton("OnboardedService")));


        result = new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT); // Here only in case we forget to reassign result
    }


    @Nested
    class GivenWrongServiceId {

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
        void whenServiceIdTooLong_thenNonconformant() {
            String testString = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop";
            result = validateAPIController.checkConformance(testString);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The serviceId is longer than 64 characters"));
        }

        @Test
        void whenServiceIdTooLongAndSymbols_thenNonconformant() {
            String testString = "qwertyuiopqwertyuiop--qwertyuiopqwertyuio-pqwertyuio-pqwertyuiopqwertyuiop";
            result = validateAPIController.checkConformance(testString);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The serviceId is longer than 64 characters"));
            assertTrue(result.getBody().contains("The serviceId contains symbols or upper case letters"));

        }

        @ParameterizedTest
        @ValueSource(strings = {"test-test", "TEST", "Test"})
        void whenServiceIdNonAlphaNumeric_thenNonconformant(String testString) {
            result = validateAPIController.checkConformance(testString);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The serviceId contains symbols or upper case letters"));
        }

        @Test
        void notInvalidTextFormat() {
            String testString = "test";
            result = validateAPIController.checkConformance(testString);
            assertNotNull(result.getBody());
            assertFalse(result.getBody().contains("Message service is requested to create a message with an invalid text format"));
        }
    }

    @Nested
    class ServiceNotOnboarded {
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
        void whenServiceNotOboarded_thenError() {
            String testString = "notonboarded";
            result = validateAPIController.checkConformance(testString);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The service is not registered"));
        }

        @Test
        void legacyWhenServiceNotOboarded_thenError() {
            String testString = "notonboarded";
            result = validateAPIController.checkValidateLegacy(testString);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The service is not registered"));

        }
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

