/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.HttpClientMockHelper;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.OIDCAuthSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OIDCExternalMapperTest {

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private TokenCreationService tokenCreationService;

    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

    private OIDCAuthSource authSource;
    private OIDCMapperHelper mapperHelper;
    private OIDCExternalMapper oidcExternalMapper;

    private static final String DISTRIBUTED_ID_1 = "openmainframe";
    private static final String DISTRIBUTED_ID_2 = "zowe";
    private static final String DISTRIBUTED_ID_3 = "apiml";
    private static final List<String> DISTRIBUTED_IDS = List.of(DISTRIBUTED_ID_1, DISTRIBUTED_ID_2, DISTRIBUTED_ID_3);
    private static final String ZOSUSER = "ZOSUSER";
    private static final String ZOSUSER_2 = "ZOSUSER2";

    private static final String SUCCESS_MAPPER_RESPONSE_TEMPLATE = "{" +
        "\"userid\": \"%s\", " +
        "\"returnCode\": 0, " +
        "\"safReturnCode\": 0, " +
        "\"racfReturnCode\": 0, " +
        "\"racfReasonCode\": 0 " +
        "}";

    private static final String FAILURE_MAPPER_RESPONSE = "{" +
        "\"userid\": \"\", " +
        "\"returnCode\": 8, " +
        "\"safReturnCode\": 8, " +
        "\"racfReturnCode\": 8, " +
        "\"racfReasonCode\": 48 " +
        "}";

    @BeforeEach
    void setup() {
        authSource = new OIDCAuthSource("OIDC_access_token");
        authSource.setDistributedId(List.of("distributed_ID"));
        mapperHelper = new OIDCMapperHelper();
        mapperHelper.registry = "test_registry";
        oidcExternalMapper = new OIDCExternalMapper("https://domain.com/mapper", "mapper_user", httpClient, tokenCreationService, authConfigurationProperties, mapperHelper);
    }

    @Nested
    class GivenIdentityMappingExists {

        @Test
        void thenZosUserIsReturned() {
            var responses = mockMapperResponse(httpClient, ZOSUSER);

            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertEquals(ZOSUSER, userId);
            verifyCallForResponse(responses);
        }

        @Test
        void givenMultipleDistributedIds_firstResponseMapped_thenZosUserIsReturned() {
            var responses = mockMapperResponse(httpClient, ZOSUSER);
            authSource.setDistributedId(DISTRIBUTED_IDS);
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertEquals(ZOSUSER, userId);

            verifyCallForResponse(responses);
        }

        @Test
        void givenMultipleDistributedIds_secondResponseMapped_thenZosUserIsReturned() {
            var responses = mockMapperResponse(httpClient, null, ZOSUSER_2);
            authSource.setDistributedId(DISTRIBUTED_IDS);
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertEquals(ZOSUSER_2, userId);

            verifyCallForResponse(responses);
        }
    }

    @Nested
    class GivenNoIdentityMappingExists {

        @Test
        void thenNullIsReturned() {
            mockMapperResponse(httpClient, (String) null);
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
        }

        @Test
        void givenMultipleDistributedIds_thenZosUserIsReturned() {
            var responses = mockMapperResponse(httpClient, null, null, null);
            authSource.setDistributedId(DISTRIBUTED_IDS);
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);

            verifyCallForResponse(responses);
        }
    }

    @Nested
    class GivenConfigurationErrors {

        @Test
        void whenAnotherAuthSourceUsed_thenNullIsReturned() throws IOException {
            JwtAuthSource jwtAuthSource = new JwtAuthSource("source");
            String userId = oidcExternalMapper.mapToMainframeUserId(jwtAuthSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        }

        @Test
        void whenRegistryIsNotProvided_thenNullIsReturned() throws IOException {
            mapperHelper.isConfigError = true;
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        }
    }

    @Nested
    class GivenInvalidDistributedIds {

        @Test
        void whenEmptyListDistributedIdProvided_thenNullIsReturned() throws IOException {
            authSource.setDistributedId(Collections.emptyList());
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        }

        @Test
        void whenNullDistributedIdProvided_thenNullIsReturned() throws IOException {
            authSource.setDistributedId(null);
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        }

        @Test
        void whenBlankDistributedIdProvided_thenNullIsReturned() throws IOException {
            authSource.setDistributedId(List.of("  "));
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        }

    }

    @Nested
    class GivenErrorsInRequest {
        @Mock
        private ObjectMapper mockedMapper;

        @BeforeEach
        void setup() {
            ReflectionTestUtils.setField(ExternalMapper.class, "objectMapper", mockedMapper);
        }

        @AfterEach
        void teardown() {
            ReflectionTestUtils.setField(ExternalMapper.class, "objectMapper", new ObjectMapper());
        }

        @Test
        void whenJsonProcessingException_thenNullIsReturned() throws IOException {
            doThrow(JsonProcessingException.class).when(mockedMapper).writeValueAsString(any());
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        }
    }

    private List<CloseableHttpResponse> mockMapperResponse(CloseableHttpClient httpClient, String... zosUsers) {
        List<CloseableHttpResponse> responses = new ArrayList<>();
        for (String zosUser : zosUsers) {
            BasicHttpEntity responseEntity;
            if (StringUtils.isBlank(zosUser)) {
                responseEntity = new BasicHttpEntity(IOUtils.toInputStream(
                    FAILURE_MAPPER_RESPONSE, StandardCharsets.UTF_8), ContentType.APPLICATION_JSON);
            } else {
                responseEntity = new BasicHttpEntity(IOUtils.toInputStream(
                    String.format(SUCCESS_MAPPER_RESPONSE_TEMPLATE, zosUser), StandardCharsets.UTF_8), ContentType.APPLICATION_JSON);
            }
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getCode()).thenReturn(HttpStatus.SC_OK);
            when(response.getEntity()).thenReturn(responseEntity);
            responses.add(response);
        }

        HttpClientMockHelper.mockExecuteWithResponse(httpClient, responses.toArray(new CloseableHttpResponse[0]));
        return responses;
    }

    @SneakyThrows
    private void verifyCallForResponse(List<CloseableHttpResponse> responses) {
        responses.forEach(response -> {
            verify(response, times(1)).getCode();
            verify(response, times(2)).getEntity();
        });
        verify(httpClient, times(responses.size())).execute(ArgumentMatchers.any(ClassicHttpRequest.class), ArgumentMatchers.any(HttpClientResponseHandler.class));
    }

}
