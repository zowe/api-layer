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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.OIDCAuthSource;
import org.zowe.commons.usermap.MapperResponse;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OIDCNativeMapperTest {
    private static final String DISTRIBUTED_ID = "distributed_id";
    private static final String DISTRIBUTED_ID_1 = "openmainframe";
    private static final String DISTRIBUTED_ID_2 = "zowe";
    private static final String DISTRIBUTED_ID_3 = "apiml";
    private static final List<String> DISTRIBUTED_IDS = List.of(DISTRIBUTED_ID_1, DISTRIBUTED_ID_2, DISTRIBUTED_ID_3);
    private static final String MF_ID = "mf_user";
    private static final String MF_ID_2 = "mf_user2";
    private static final String MF_ID_3 = "mf_user3";
    private static final String REGISTRY = "test_registry";

    private OIDCAuthSource authSource;
    private OIDCMapperHelper mapperHelper;
    private OIDCNativeMapper oidcNativeMapper;

    private NativeMapperWrapper mockMapper;

    @BeforeEach
    void setUp() {
        authSource = new OIDCAuthSource("OIDC_access_token");
        authSource.setDistributedId(List.of(DISTRIBUTED_ID));
        mockMapper = mock(NativeMapperWrapper.class);
        mapperHelper = new OIDCMapperHelper();
        mapperHelper.registry = REGISTRY;
        oidcNativeMapper = new OIDCNativeMapper(mockMapper, mapperHelper);
    }

    @Nested
    class GivenIdentityMappingExists {

        @Test
        void thenZosUserIsReturned() {
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID, REGISTRY)).thenReturn(new MapperResponse(MF_ID, 0, 0, 0, 0));
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertEquals(MF_ID, userId);
        }

        @Test
        void givenMultipleDistributedIds_onlyOneMapped_thenZosUserIsReturned() {
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_1, REGISTRY)).thenReturn(new MapperResponse(null, 8, 8, 8, 48));
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_2, REGISTRY)).thenReturn(new MapperResponse(MF_ID_2, 0, 0, 0, 0));
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_3, REGISTRY)).thenReturn(new MapperResponse(null, 8, 8, 8, 48));
            authSource.setDistributedId(DISTRIBUTED_IDS);
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertEquals(MF_ID_2, userId);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID_1, REGISTRY);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID_2, REGISTRY);
            verify(mockMapper, never()).getUserIDForDN(DISTRIBUTED_ID_3, REGISTRY);
        }

        @Test
        void givenMultipleDistributedIds_multipleOneMapped_thenZosUserIsReturned() {
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_1, REGISTRY)).thenReturn(new MapperResponse("", 8, 8, 8, 48));
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_2, REGISTRY)).thenReturn(new MapperResponse(MF_ID_2, 0, 0, 0, 0));
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_3, REGISTRY)).thenReturn(new MapperResponse(MF_ID_3, 0, 0, 0, 0));
            authSource.setDistributedId(DISTRIBUTED_IDS);
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertEquals(MF_ID_2, userId);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID_1, REGISTRY);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID_2, REGISTRY);
            verify(mockMapper, never()).getUserIDForDN(DISTRIBUTED_ID_3, REGISTRY);
        }

    }

    @Nested
    class GivenNoIdentityMappingExists {

        @Test
        void thenNullIsReturned() {
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID, REGISTRY)).thenReturn(new MapperResponse("", 8, 8, 8, 48));
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID, REGISTRY);
        }

        @Test
        void givenMultipleDistributedIds_thenNullIsReturned() {
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_1, REGISTRY)).thenReturn(new MapperResponse(null, 8, 8, 8, 48));
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_2, REGISTRY)).thenReturn(new MapperResponse(null, 8, 8, 8, 48));
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID_3, REGISTRY)).thenReturn(new MapperResponse(null, 8, 8, 8, 48));
            authSource.setDistributedId(DISTRIBUTED_IDS);
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID_1, REGISTRY);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID_2, REGISTRY);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID_3, REGISTRY);
        }
    }

    @Nested
    class GivenRacfProcessingError {

        @BeforeEach
        void setup() {
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID, REGISTRY)).thenReturn(new MapperResponse("none", 8, 8, 8, 8));
        }

        @Test
        void thenNullIsReturned() {
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID, REGISTRY);
        }
    }

    @Nested
    class GivenConfigurationErrors {

        @Test
        void whenAnotherAuthSourceUsed_thenNullIsReturned() {
            JwtAuthSource jwtAuthSource = new JwtAuthSource("source");
            String userId = oidcNativeMapper.mapToMainframeUserId(jwtAuthSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }

        @Test
        void whenRegistryIsNotProvided_thenNullIsReturned() {
            mapperHelper.isConfigError = true;
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }

    }

    @Nested
    class GivenInvalidDistributedIds {

        @Test
        void whenEmptyListDistributedIdProvided_thenNullIsReturned() {
            authSource.setDistributedId(Collections.emptyList());
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }

        @Test
        void whenBlankValueDistributedIdProvided_thenNullIsReturned() {
            authSource.setDistributedId(List.of(" "));
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }

        @Test
        void whenNullValueDistributedIdProvided_thenNullIsReturned() {
            authSource.setDistributedId(null);
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }

    }

}
