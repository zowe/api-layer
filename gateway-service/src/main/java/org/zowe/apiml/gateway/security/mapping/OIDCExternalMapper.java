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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.mapping.model.OIDCRequest;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.io.UnsupportedEncodingException;

import static org.zowe.apiml.gateway.security.mapping.model.MapperResponse.OIDC_FAILED_MESSAGE_KEY;

@Component("oidcMapper")
@ConditionalOnProperty(value = "apiml.security.oidc.enabled", havingValue = "true")
public class OIDCExternalMapper extends ExternalMapper implements AuthenticationMapper {

    @Value("${apiml.security.oidc.registry:}")
    protected String registry;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    public OIDCExternalMapper(@Value("${apiml.security.oidc.identityMapperUrl:}") String mapperUrl,
                              @Value("${apiml.security.oidc.identityMapperUser:}") String mapperUser,
                              CloseableHttpClient httpClientProxy,
                              TokenCreationService tokenCreationService,
                              AuthConfigurationProperties authConfigurationProperties) {
        super(mapperUrl, mapperUser, httpClientProxy, tokenCreationService, authConfigurationProperties);
    }

    public String mapToMainframeUserId(AuthSource authSource) {
        if (!(authSource instanceof OIDCAuthSource)) {
            apimlLog.log(MessageType.DEBUG,"The used authentication source type is {} and not OIDC", authSource.getType());
            return null;
        }

        if (StringUtils.isEmpty(registry)) {
            apimlLog.log(OIDC_FAILED_MESSAGE_KEY,
                "Missing registry name configuration. Make sure that " +
                    "'components.gateway.apiml.security.oidc.registry' is correctly set in 'zowe.yaml'.");
            return null;
        }
        final String distributedId = ((OIDCAuthSource) authSource).getDistributedId();
        if (StringUtils.isEmpty(distributedId)) {
            apimlLog.log(OIDC_FAILED_MESSAGE_KEY,
                "OIDC token is missing the distributed ID. Make sure your distributed identity provider is" +
                    " properly configured.");
            return null;
        }
        OIDCRequest oidcRequest = new OIDCRequest(distributedId, registry);
        try {
            StringEntity payload = new StringEntity(objectMapper.writeValueAsString(oidcRequest));
            MapperResponse mapperResponse = callExternalMapper(payload);

            if (mapperResponse != null && mapperResponse.isOIDCResultValid()) {
                String userId = mapperResponse.getUserId().trim();
                return StringUtils.isNotEmpty(userId) ? userId : null;
            }

        } catch (UnsupportedEncodingException e) {
            apimlLog.log("org.zowe.apiml.security.common.OIDCMappingError",
                "Unable to encode payload for identity mapping request",
                e.getMessage());
        } catch (JsonProcessingException e) {
            apimlLog.log("org.zowe.apiml.security.common.OIDCMappingError",
                "Unable to generate JSON payload for identity mapping request",
                e.getMessage());
        }

        return null;
    }

}
