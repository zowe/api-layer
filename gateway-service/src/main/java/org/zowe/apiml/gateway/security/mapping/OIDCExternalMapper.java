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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.mapping.model.OIDCRequest;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.io.UnsupportedEncodingException;

@Slf4j
@Component("oidcMapper")
public class OIDCExternalMapper extends ExternalMapper implements AuthenticationMapper {

    @Value("${apiml.security.oidc.registry}")
    private String registry;

    public OIDCExternalMapper(CloseableHttpClient httpClientProxy, TokenCreationService tokenCreationService, AuthConfigurationProperties authConfigurationProperties) {
        super(httpClientProxy, tokenCreationService, Type.OIDC, authConfigurationProperties);
    }

    public String mapToMainframeUserId(AuthSource authSource) {
        if (!(authSource instanceof OIDCAuthSource)) {
            return null;
        }

        OIDCRequest oidcRequest = new OIDCRequest(((OIDCAuthSource) authSource).getDistributedId(), registry);
        try {
            StringEntity payload = new StringEntity(objectMapper.writeValueAsString(oidcRequest));
            MapperResponse mapperResponse = callExternalMapper(payload);

            if (mapperResponse != null) {
                mapperResponse.validateOIDCResults();
                String userId = mapperResponse.getUserId().trim();
                return StringUtils.isNotEmpty(userId) ? userId : null;
            }

        } catch (UnsupportedEncodingException e) {
            log.debug("Unable to encode payload for identity mapping request", e);
        } catch (JsonProcessingException e) {
            log.debug("Unable to generate JSON payload for identity mapping request", e);
        }
        return null;
    }

}
