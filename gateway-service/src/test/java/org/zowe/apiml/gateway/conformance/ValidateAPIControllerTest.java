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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ValidateAPIControllerTest {

    @InjectMocks
    private ValidateAPIController validateAPIController;

    @Mock
    private VerificationOnboardService verificationOnboardService;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private MessageService messageService;
    @Mock
    ServiceInstance serviceInstance;

    @Mock
    private GatewayClient gatewayClient;
    @Mock
    private AbstractSwaggerValidator swaggerValidator;

    ResponseEntity<String> result;

    private static final String WRONG_SERVICE_ID_KEY = "org.zowe.apiml.gateway.verifier.wrongServiceId";
    private static final String NO_METADATA_KEY = "org.zowe.apiml.gateway.verifier.noMetadata";
    private static final String NON_CONFORMANT_KEY = "org.zowe.apiml.gateway.verifier.nonConformant";

    private static final Message WRONG_SERVICE_ID_MESSAGE = new YamlMessageService("/gateway-log-messages.yml").createMessage(WRONG_SERVICE_ID_KEY, "ThisWillBeRemoved");
    private static final Message NO_METADATA_MESSAGE = new YamlMessageService("/gateway-log-messages.yml").createMessage(NO_METADATA_KEY, "ThisWillBeRemoved");
    private static final Message NON_CONFORMANT_MESSAGE = new YamlMessageService("/gateway-log-messages.yml").createMessage(NON_CONFORMANT_KEY, "ThisWillBeRemoved");

    @AfterEach
    void cleanup() {
        result = null;
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
            when(messageService.createMessage(NON_CONFORMANT_KEY, "ThisWillBeRemoved")).thenReturn(NON_CONFORMANT_MESSAGE);
            String testString = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop";
            result = validateAPIController.checkConformance(testString, null);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The serviceId is longer than 64 characters"));
        }

        @Test
        void whenServiceIdTooLongAndSymbols_thenNonconformant() {
            when(messageService.createMessage(NON_CONFORMANT_KEY, "ThisWillBeRemoved")).thenReturn(NON_CONFORMANT_MESSAGE);
            String testString = "qwertyuiopqwertyuiop--qwertyuiopqwertyuio-pqwertyuio-pqwertyuiopqwertyuiop";
            result = validateAPIController.checkConformance(testString, null);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The serviceId is longer than 64 characters"));
            assertTrue(result.getBody().contains("The serviceId contains symbols or upper case letters"));

        }

        @ParameterizedTest
        @ValueSource(strings = {"test-test", "TEST", "Test"})
        void whenServiceIdNonAlphaNumeric_thenNonconformant(String testString) {
            when(messageService.createMessage(NON_CONFORMANT_KEY, "ThisWillBeRemoved")).thenReturn(NON_CONFORMANT_MESSAGE);
            result = validateAPIController.checkConformance(testString, null);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The serviceId contains symbols or upper case letters"));
        }

        @Test
        void notInvalidTextFormat() {
            when(messageService.createMessage(WRONG_SERVICE_ID_KEY, "ThisWillBeRemoved")).thenReturn(WRONG_SERVICE_ID_MESSAGE);
            String testString = "test";
            result = validateAPIController.checkConformance(testString, null);
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
            when(messageService.createMessage(WRONG_SERVICE_ID_KEY, "ThisWillBeRemoved")).thenReturn(WRONG_SERVICE_ID_MESSAGE);
            String testString = "notonboarded";
            result = validateAPIController.checkConformance(testString, null);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The service is not registered"));
        }

        @Test
        void legacyWhenServiceNotOboarded_thenError() {
            when(messageService.createMessage(WRONG_SERVICE_ID_KEY, "ThisWillBeRemoved")).thenReturn(WRONG_SERVICE_ID_MESSAGE);
            String testString = "notonboarded";
            result = validateAPIController.checkValidateLegacy(testString, null);
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("The service is not registered"));

        }
    }


    @Nested
    class GivenMetadata {
        @Test
        void whenEmpty_thenCorrectResponse() {
            HashMap<String, String> metadata = new HashMap<>();
            ValidationException exception = assertThrows(ValidationException.class, () -> validateAPIController.checkMetadataCanBeRetrieved(metadata));
            assertEquals("Cannot Retrieve MetaData", exception.getMessage());
        }

        @Test
        void whenNotEmpty_thenCorrectResponse() {
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("key", "value");
            assertDoesNotThrow(() -> validateAPIController.checkMetadataCanBeRetrieved(metadata));
        }

        @Test
        void whenEmpty_thenCorrectConformanceResponse() {
            String serviceId = "testservice";
            HashMap<String, String> mockMetadata = new HashMap<>();
            when(verificationOnboardService.checkOnboarding(serviceId)).thenReturn(true);
            when(discoveryClient.getInstances(serviceId)).thenReturn(new ArrayList<>(Collections.singleton(serviceInstance)));
            when(serviceInstance.getMetadata()).thenReturn(mockMetadata);
            when(messageService.createMessage(NO_METADATA_KEY, "ThisWillBeRemoved")).thenReturn(NO_METADATA_MESSAGE);
            result = validateAPIController.checkConformance(serviceId, null);
            assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        }

    }

    @Nested
    class GivenInstanceList {

        @Test
        void whenEmpty_thenCorrectResponse() {
            List<ServiceInstance> list = new ArrayList<>();
            ValidationException exception = assertThrows(ValidationException.class, () -> validateAPIController.checkInstanceCanBeRetrieved(list));
            assertTrue(exception.getMessage().contains("Cannot retrieve metadata"));
        }

    }


    @Nested
    class GivenDifferentMetadata {

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

        @ParameterizedTest
        @ValueSource(strings = {"src/test/resources/api-doc.json", "src/test/resources/api-doc-v2.json"})
        void whenEverythingOk_thenOkResponse(String mockSwaggerFileLocation) throws IOException {
            String serviceId = "testservice";
            HashMap<String, String> mockMetadata = new HashMap<>();
            mockMetadata.put("swaggerUrl", "https://sample.swagger.url");
            mockMetadata.put(EurekaMetadataDefinition.AUTHENTICATION_SSO, "true");

            File mockSwaggerFile = new File(mockSwaggerFileLocation);

            when(verificationOnboardService.checkOnboarding(serviceId)).thenReturn(true);
            when(discoveryClient.getInstances(serviceId)).thenReturn(new ArrayList<>(Collections.singleton(serviceInstance)));
            when(serviceInstance.getMetadata()).thenReturn(mockMetadata);
            when(verificationOnboardService.findSwaggerUrl(mockMetadata)).thenReturn(Optional.of("a"));
            when(gatewayClient.getGatewayConfigProperties()).thenReturn(ServiceAddress.builder().build());

            when(swaggerValidator.getMessages()).thenReturn(new ArrayList<>());
            when(swaggerValidator.getAllEndpoints()).thenReturn(new HashSet<>(Collections.singletonList(new Endpoint(null, null, null, null))));
            when(swaggerValidator.getProblemsWithEndpointUrls()).thenReturn(new ArrayList<>());

            when(verificationOnboardService.getSwagger("a")).thenReturn(new String(Files.readAllBytes(mockSwaggerFile.getAbsoluteFile().toPath())));

            when(verificationOnboardService.testEndpointsByCalling(any(), isNull())).thenReturn(new ArrayList<>());

            try (MockedStatic<ValidatorFactory> validatorFactoryMockedStatic = mockStatic(ValidatorFactory.class)) {
                validatorFactoryMockedStatic.when(() -> ValidatorFactory.parseSwagger(any(), any(), any(), any())).thenReturn(swaggerValidator);
                result = validateAPIController.checkConformance(serviceId, null);
                assertEquals(HttpStatus.OK, result.getStatusCode());
            }
        }

        @Test
        void whenBadMetadata_thenBadMetadataResponse() {
            String serviceId = "testservice";

            HashMap<String, String> mockMetadata = new HashMap<>();

            when(verificationOnboardService.checkOnboarding(serviceId)).thenReturn(true);
            when(discoveryClient.getInstances(serviceId)).thenReturn(new ArrayList<>(Collections.singleton(serviceInstance)));
            when(serviceInstance.getMetadata()).thenReturn(mockMetadata);
            when(messageService.createMessage(NO_METADATA_KEY, "ThisWillBeRemoved")).thenReturn(NO_METADATA_MESSAGE);

            result = validateAPIController.checkConformance(serviceId, null);
            assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
            assertNotNull(result.getBody());
            assertTrue(result.getBody().contains("Cannot Retrieve MetaData"));
        }
    }
}

