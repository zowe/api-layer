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

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class OIDCMapperTest {

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private TokenCreationService tokenCreationService;

    @Mock
    private AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

    @Test
    void test() {
        String expectedMainframeId = "ZOSUSER";

        OIDCAuthSource oidcAuthSource = new OIDCAuthSource("distributedId");
        OIDCExternalMapper oidcExternalMapper = spy(new OIDCExternalMapper(httpClient, tokenCreationService, authConfigurationProperties));
        doReturn(new MapperResponse(expectedMainframeId, 0, 0, 0, 0)).when(oidcExternalMapper).callExternalMapper(any());
        String userId = oidcExternalMapper.mapToMainframeUserId(oidcAuthSource);

        assertEquals(expectedMainframeId, userId);
    }

}
