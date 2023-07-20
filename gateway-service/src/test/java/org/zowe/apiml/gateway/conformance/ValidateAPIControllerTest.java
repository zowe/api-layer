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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@AcceptanceTest
public class ValidateAPIControllerTest {

    @Autowired
    private ValidateAPIController validateAPIController;

    @MockBean
    private VerificationOnboardService verificationOnboardService;

    @BeforeEach
    void setup() {
        MessageService messageService = new YamlMessageService("/gateway-log-messages.yml");
        validateAPIController = new ValidateAPIController(messageService, verificationOnboardService);
        standaloneSetup(validateAPIController);
    }


    @Nested
    class GivenWrongServiceId {
        @Test
        public void whenServiceIdTooLong_thenNonconformant() {

            String testString = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop";

            when(verificationOnboardService.checkOnboarding(testString)).thenReturn(true);
            when(verificationOnboardService.canRetrieveMetaData(testString)).thenReturn(true);

            ResponseEntity<ApiMessage> result = validateAPIController.checkConformance(testString);

            assertNotNull(result.getBody());

            assertTrue(result.getBody().getMessageContent().contains("The serviceId is longer than 64 characters"));

        }

        @Test
        public void whenServiceIdTooLongAndSymbols_thenNonconformant() {

            String testString = "qwertyuiopqwertyuiop--qwertyuiopqwertyuio-pqwertyuio-pqwertyuiopqwertyuiop";

            when(verificationOnboardService.checkOnboarding(testString)).thenReturn(true);
            when(verificationOnboardService.canRetrieveMetaData(testString)).thenReturn(true);

            ResponseEntity<ApiMessage> result = validateAPIController.checkConformance(testString);

            assertNotNull(result.getBody());

            assertTrue(result.getBody().getMessageContent().contains("The serviceId is longer than 64 characters"));
            assertTrue(result.getBody().getMessageContent().contains("The serviceId contains symbols or upper case letters"));

        }

        @ParameterizedTest
        @ValueSource(strings = {"test-test", "TEST", "Test"})
        public void whenServiceIdNonAlphaNumeric_thenNonconformant(String testString) {

            when(verificationOnboardService.checkOnboarding(testString)).thenReturn(true);
            when(verificationOnboardService.canRetrieveMetaData(testString)).thenReturn(true);

            ResponseEntity<ApiMessage> result = validateAPIController.checkConformance(testString);

            assertNotNull(result.getBody());

            assertTrue(result.getBody().getMessageContent().contains("The serviceId contains symbols or upper case letters"));

        }

    }

    @Nested
    class ServiceNotOnboarded {

        @Test
        public void WhenServiceNotOboarded_thenError() {

            String testString = "notOnboarded";

            when(verificationOnboardService.checkOnboarding(testString)).thenReturn(false);

            ResponseEntity<ApiMessage> result = validateAPIController.checkConformance(testString);


            assertNotNull(result.getBody());

            assertTrue(result.getBody().getMessageContent().contains("The service is not registered"));

        }


        @Test
        public void LegacyWhenServiceNotOboarded_thenError() {

            String testString = "notOnboarded";

            when(verificationOnboardService.checkOnboarding(testString)).thenReturn(false);

            ResponseEntity<ApiMessage> result = validateAPIController.checkValidateLegacy(testString);


            assertNotNull(result.getBody());

            assertTrue(result.getBody().getMessageContent().contains("The service is not registered"));

        }
    }
}

