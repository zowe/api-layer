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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import java.util.ArrayList;
import java.util.Collections;

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


        result = new ResponseEntity<String>(HttpStatus.I_AM_A_TEAPOT); // Here only in case we forget to reassign result
    }


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


    @Nested
    class GivenWrongServiceId {
        @Test
        public void whenServiceIdTooLong_thenNonconformant() {

            String testString = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop";

            result = validateAPIController.checkConformance(testString);

            assertNotNull(result.getBody());

            assertTrue(result.getBody().contains("The serviceId is longer than 64 characters"));

        }

        @Test
        public void whenServiceIdTooLongAndSymbols_thenNonconformant() {

            String testString = "qwertyuiopqwertyuiop--qwertyuiopqwertyuio-pqwertyuio-pqwertyuiopqwertyuiop";


            result = validateAPIController.checkConformance(testString);

            assertNotNull(result.getBody());

            assertTrue(result.getBody().contains("The serviceId is longer than 64 characters"));
            assertTrue(result.getBody().contains("The serviceId contains symbols or upper case letters"));

        }

        @ParameterizedTest
        @ValueSource(strings = {"test-test", "TEST", "Test"})
        public void whenServiceIdNonAlphaNumeric_thenNonconformant(String testString) {

            result = validateAPIController.checkConformance(testString);

            assertNotNull(result.getBody());

            assertTrue(result.getBody().contains("The serviceId contains symbols or upper case letters"));

        }

        @Test
        public void notInvalidTextFormat() {

            String testString = "test";

            result = validateAPIController.checkConformance(testString);

            assertNotNull(result.getBody());

            assertFalse(result.getBody().contains("Message service is requested to create a message with an invalid text format"));

        }


    }

    @Nested
    class ServiceNotOnboarded {

        @Test
        public void WhenServiceNotOboarded_thenError() {

            String testString = "notOnboarded";

            when(verificationOnboardService.checkOnboarding(testString)).thenReturn(false);

            result = validateAPIController.checkConformance(testString);

            assertNotNull(result.getBody());

            assertTrue(result.getBody().contains("The service is not registered"));

        }


        @Test
        public void LegacyWhenServiceNotOboarded_thenError() {

            String testString = "notOnboarded";

            when(verificationOnboardService.checkOnboarding(testString)).thenReturn(false);

            result = validateAPIController.checkValidateLegacy(testString);


            assertNotNull(result.getBody());

            assertTrue(result.getBody().contains("The service is not registered"));

        }
    }
}

