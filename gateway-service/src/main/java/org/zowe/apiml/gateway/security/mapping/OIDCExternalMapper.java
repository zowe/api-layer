package org.zowe.apiml.gateway.security.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.mapping.model.OIDCRequest;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.io.UnsupportedEncodingException;

@Slf4j
public class OIDCExternalMapper extends ExternalMapper implements AuthenticationMapper {

    public OIDCExternalMapper(CloseableHttpClient httpClientProxy, TokenCreationService tokenCreationService, AuthConfigurationProperties authConfigurationProperties) {
        super(httpClientProxy, tokenCreationService, Type.OIDC, authConfigurationProperties);
    }

    public String mapToMainframeUserId(AuthSource authSource) {
        if (!(authSource instanceof OIDCAuthSource)) {
            return null;
        }

        OIDCRequest oidcRequest = new OIDCRequest((String) authSource.getRawSource(), (String) authSource.getRawSource()); // TODO: Fix source
        try {
            StringEntity payload = new StringEntity(objectMapper.writeValueAsString(oidcRequest));
            MapperResponse mapperResponse = callExternalMapper(payload);

            if (mapperResponse == null) {
                throw new OIDCExternalMapperException("External identity mapper returned no response");
            }
            mapperResponse.validateOIDCResults();

            return mapperResponse.getUserId().trim();
        } catch (UnsupportedEncodingException e) {
            throw new OIDCExternalMapperException("Unable to encode payload for identity mapping request", e);
        } catch (JsonProcessingException e) {
            throw new OIDCExternalMapperException("Unable to generate JSON payload for identity mapping request", e);
        }
    }

}
