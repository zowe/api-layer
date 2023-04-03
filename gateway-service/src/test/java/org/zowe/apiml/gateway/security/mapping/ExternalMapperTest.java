/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.mapping;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalMapperTest {

    class TestExternalMapper extends ExternalMapper {
        public TestExternalMapper(CloseableHttpClient httpClientProxy, TokenCreationService tokenCreationService) {
            super(httpClientProxy, tokenCreationService, Type.X509, authConfigurationProperties);
        }
    }

    private TestExternalMapper mapper;
    private TokenCreationService tokenCreationService;

    private CloseableHttpClient closeableHttpClient;
    private CloseableHttpResponse httpResponse;
    private StatusLine statusLine;
    private HttpEntity responseEntity;
    private AuthConfigurationProperties authConfigurationProperties;


    @BeforeEach
    void setup() throws IOException {
        closeableHttpClient = mock(CloseableHttpClient.class);
        httpResponse = mock(CloseableHttpResponse.class);
        statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(closeableHttpClient.execute(any())).thenReturn(httpResponse);
        tokenCreationService = mock(TokenCreationService.class);
        when(tokenCreationService.createJwtTokenWithoutCredentials(anyString())).thenReturn("validJwtToken");
        responseEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(responseEntity);
        authConfigurationProperties = new AuthConfigurationProperties();

        mapper = new TestExternalMapper(closeableHttpClient, tokenCreationService);
        ReflectionTestUtils.setField(mapper,"externalMapperUrl","http://localhost/test");
        ReflectionTestUtils.setField(mapper,"externalMapperUser","mapper_user");
    }

    @Nested
    class GivenValidMapperResponse {
        @Nested
        class WhenUserMappingExists {
            @BeforeEach
            void setup() throws IOException {
                when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream(
                    "{\"userid\":\"ZOSUSER\",\"returnCode\":0,\"safReturnCode\":0,\"racfReturnCode\":0,\"racfReasonCode\":0}".getBytes()
                ));
            }

            @Test
            void thenMFUserIsReturned() {
                HttpEntity payload = new BasicHttpEntity();
                MapperResponse response = mapper.callExternalMapper(payload);
                assertNotNull(response);
                assertEquals("ZOSUSER", response.getUserId());
                assertEquals(0, response.getRc());
                assertEquals(0, response.getSafRc());
                assertEquals(0, response.getRacfRc());
                assertEquals(0, response.getRacfRs());
            }
        }

        @Nested
        class WhenUserMappingFailed {
            @BeforeEach
            void setup() throws IOException {
                when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream(
                    "{\"userid\":\"\",\"returnCode\":0,\"safReturnCode\":8,\"racfReturnCode\":8,\"racfReasonCode\":48}".getBytes()
                ));
            }

            @Test
            void thenMFUserIsEmpty() {
                HttpEntity payload = new BasicHttpEntity();
                MapperResponse response = mapper.callExternalMapper(payload);
                assertNotNull(response);
                assertEquals("", response.getUserId());
                assertEquals(0, response.getRc());
                assertEquals(8, response.getSafRc());
                assertEquals(8, response.getRacfRc());
                assertEquals(48, response.getRacfRs());
            }
        }
    }
    @Nested
    class GivenInvalidMapperResponse {

        @Nested
        class WhenStatusCode400 {
            @BeforeEach
            void setup() {
                when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
            }
            @Test
            void thenResponseIsNull() {
                HttpEntity payload = new BasicHttpEntity();
                MapperResponse response = mapper.callExternalMapper(payload);
                assertNull(response);
            }
        }

        @Nested
        class WhenStatusCode102 {
            @BeforeEach
            void setup() {
                when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_PROCESSING);
            }
            @Test
            void thenResponseIsNull() {
                HttpEntity payload = new BasicHttpEntity();
                MapperResponse response = mapper.callExternalMapper(payload);
                assertNull(response);
            }
        }

        @Nested
        class WhenStatusLineIsNull {
            @BeforeEach
            void setup() {
                when(httpResponse.getStatusLine()).thenReturn(null);
            }
            @Test
            void thenResponseIsNull() {
                HttpEntity payload = new BasicHttpEntity();
                MapperResponse response = mapper.callExternalMapper(payload);
                assertNull(response);
            }
        }

        @Nested
        class WhenInvalidContent {
            @BeforeEach
            void setup() throws IOException {
                when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream("invalid content".getBytes()));
            }
            @Test
            void thenResponseIsNull() {
                HttpEntity payload = new BasicHttpEntity();
                MapperResponse response = mapper.callExternalMapper(payload);
                assertNull(response);
            }
        }

        @Nested
        class WhenResponseIsEmpty {
            @BeforeEach
            void setup() throws IOException {
                when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes()));
            }
            @Test
            void thenResponseIsNull() {
                HttpEntity payload = new BasicHttpEntity();
                MapperResponse response = mapper.callExternalMapper(payload);
                assertNull(response);
            }
        }
    }

    @Nested
    class GivenInvalidMapperSetup {
        @Test
        void whenMapperUrlNull_thenResponseIsNull() {
            ReflectionTestUtils.setField(mapper,"externalMapperUrl",null);
            HttpEntity payload = new BasicHttpEntity();
            MapperResponse response = mapper.callExternalMapper(payload);
            assertNull(response);
        }
        @Test
        void whenMapperUrlEmpty_thenResponseIsNull() {
            ReflectionTestUtils.setField(mapper,"externalMapperUrl","");
            HttpEntity payload = new BasicHttpEntity();
            MapperResponse response = mapper.callExternalMapper(payload);
            assertNull(response);
        }
        @Test
        void whenMapperUrlInvalid_thenResponseIsNull() {
            ReflectionTestUtils.setField(mapper,"externalMapperUrl","%");
            HttpEntity payload = new BasicHttpEntity();
            MapperResponse response = mapper.callExternalMapper(payload);
            assertNull(response);
        }
        @Test
        void whenMapperUserNull_thenResponseIsNull() {
            ReflectionTestUtils.setField(mapper,"externalMapperUser",null);
            HttpEntity payload = new BasicHttpEntity();
            MapperResponse response = mapper.callExternalMapper(payload);
            assertNull(response);
        }
        @Test
        void whenMapperUserEmpty_thenResponseIsNull() {
            ReflectionTestUtils.setField(mapper,"externalMapperUser","");
            HttpEntity payload = new BasicHttpEntity();
            MapperResponse response = mapper.callExternalMapper(payload);
            assertNull(response);
        }
    }
}

