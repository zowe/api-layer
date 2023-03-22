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

    //TODO: use APIML logger or exceptions
    public String mapToMainframeUserId(AuthSource authSource) {
        if (!(authSource instanceof OIDCAuthSource)) {
            return null;
        }

        OIDCRequest oidcRequest = new OIDCRequest((String) authSource.getRawSource(), (String) authSource.getRawSource()); // TODO: Fix source
        try {
            StringEntity payload = new StringEntity(objectMapper.writeValueAsString(oidcRequest));
            MapperResponse mapperResponse = callExternalMapper(payload);

            if(isValidIdentityResponse(mapperResponse)) {
                return mapperResponse.getUserId().trim();
            }

        } catch (UnsupportedEncodingException e) {
            log.error("Cannot encode input data", e);
        } catch (JsonProcessingException e) {
            log.error("Cannot create JSON payload", e);
        }

        return null;
    }

    public boolean isValidIdentityResponse(MapperResponse mapperResponse) {
        if (mapperResponse == null) {
            log.error("ZSS identity mapping service has not returned any response");
            return false;
        }

        //TODO: Add reasonable messages to known code combinations
        if (mapperResponse.getRc() > 0 ||
            mapperResponse.getSafRc() > 0 ||
            mapperResponse.getRacfRc() > 0 ||
            mapperResponse.getRacfRs() > 0) {
            log.error("Failed to nap distributed identity to mainframe identity. {}", mapperResponse);
            return false;
        }

        return true;
    }

}
