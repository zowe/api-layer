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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class OIDCNativeMapperTest {
    private static final String DISTRIBUTED_ID = "distributed_id";
    private static final String MF_ID = "mf_user";
    private static final String REGISTRY = "test_registry";

    private OIDCAuthSource authSource;
    private OIDCNativeMapper oidcNativeMapper;

    private NativeMapperWrapper mockMapper;

    @BeforeEach
    void setUp() {
        authSource = new OIDCAuthSource("OIDC_access_token");
        authSource.setDistributedId(DISTRIBUTED_ID);
        mockMapper = mock(NativeMapperWrapper.class);
        oidcNativeMapper = new OIDCNativeMapper(mockMapper);
        oidcNativeMapper.registry = REGISTRY;
    }

    @Nested
    class GivenIdentityMappingExists {
        @BeforeEach
        void setup() {
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID, REGISTRY)).thenReturn(new MapperResponse(MF_ID, 0, 0, 0, 0));
        }

        @Test
        void thenZosUserIsReturned() {
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertEquals(MF_ID, userId);
        }
    }

    @Nested
    class GivenNoIdentityMappingExists {

        @BeforeEach
        void setup() {
            when(mockMapper.getUserIDForDN(DISTRIBUTED_ID, REGISTRY)).thenReturn(new MapperResponse("", 8, 8, 8, 48));
        }

        @Test
        void thenNullIsReturned() {
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(mockMapper, times(1)).getUserIDForDN(DISTRIBUTED_ID, REGISTRY);
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
            oidcNativeMapper.isConfigError = true;
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }

        @Test
        void whenNoDistributedIdProvided_thenNullIsReturned() {
            authSource.setDistributedId("");
            String userId = oidcNativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }

    }

}
