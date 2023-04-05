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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OIDCExternalMapperTest {

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private TokenCreationService tokenCreationService;

    @Mock
    private AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

    private OIDCAuthSource authSource;
    private OIDCExternalMapper oidcExternalMapper;

    private static final String ZOSUSER = "ZOSUSER";

    @BeforeEach
    void setup() {
        authSource = new OIDCAuthSource("distributedId");
        oidcExternalMapper = spy(new OIDCExternalMapper(httpClient, tokenCreationService, authConfigurationProperties));
    }

    @Nested
    class GivenIdentityMappingExists {
        @Test
        void thenZosUserIsReturned() {
            doReturn(new MapperResponse(ZOSUSER, 0, 0, 0, 0)).when(oidcExternalMapper).callExternalMapper(any());
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertEquals(ZOSUSER, userId);
        }

        @Test
        void whenAnotherAuthSourceUsed_thenNullIsReturned() {
            JwtAuthSource jwtAuthSource = new JwtAuthSource("source");
            String userId = oidcExternalMapper.mapToMainframeUserId(jwtAuthSource);
            assertNull(userId);
            verify(oidcExternalMapper, times(0)).callExternalMapper(any());
        }
    }

    @Nested
    class GivenNoIdentityMappingExists {

        @BeforeEach
        void setup() {
            doReturn(new MapperResponse("", 8, 8, 8, 48)).when(oidcExternalMapper).callExternalMapper(any());
        }

        @Test
        void thenNullIsReturned() {
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
        }
    }

    @Nested
    class GivenRegistryNameIsAllBlanks {

        @BeforeEach
        void setup() {
            doReturn(new MapperResponse("", 8, 8, 8, 44)).when(oidcExternalMapper).callExternalMapper(any());
        }

        @Test
        void thenNullIsReturned() {
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
        }
    }
    @Nested
    class GivenErrorsInRequest {

        @Mock
        private ObjectMapper mockedMapper;

        private Field objectMapperField;
        @BeforeEach
        void setup() throws ReflectiveOperationException {
            setFinalStaticField(ExternalMapper.class, "objectMapper", mockedMapper);

        }

        @AfterEach
         void teardown() throws ReflectiveOperationException {
            setFinalStaticField(ExternalMapper.class, "objectMapper", new ObjectMapper());

        }
        @Test
        void whenJsonProcessingException_thenNullIsReturned() throws JsonProcessingException {
            doThrow(JsonProcessingException.class).when(mockedMapper).writeValueAsString(any());
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(oidcExternalMapper, times(0)).callExternalMapper(any());
        }

    }

    private static void setFinalStaticField(Class<?> clazz, String fieldName, Object value)
        throws ReflectiveOperationException {

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, value);
    }

}
