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

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.zowe.apiml.zaas.security.mapping.model.MapperResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Certificate mapper that allows to return user id of the provided x509 certificate
 * This mapper will be executed when ZSS is used
 */

@Slf4j
@Component("x509Mapper")
@ConditionalOnExpression(
    "T(org.apache.commons.lang3.StringUtils).isNotEmpty('${apiml.security.x509.externalMapperUrl:}') && '${apiml.security.useInternalMapper:false}' == 'false'"
)
public class X509ExternalMapper extends ExternalMapper implements AuthenticationMapper {

    public X509ExternalMapper(@Value("${apiml.security.x509.externalMapperUrl:}") String mapperUrl,
                              @Value("${apiml.security.x509.externalMapperUser:}") String mapperUser,
                              CloseableHttpClient httpClientProxy,
                              TokenCreationService tokenCreationService,
                              AuthConfigurationProperties authConfigurationProperties) {
        super(mapperUrl, mapperUser, httpClientProxy, tokenCreationService, authConfigurationProperties);
    }

    /**
     * Maps certificate to the mainframe user id.
     *
     * @param authSource Certificate to get mapping for.
     * @return the user id or null if there is either no mapping or problem with certificate
     */
    @Override
    public String mapToMainframeUserId(AuthSource authSource) {
        if (authSource instanceof X509AuthSource) {
            X509Certificate certificate = (X509Certificate) authSource.getRawSource();
            if (certificate != null) {
                try {
                    HttpEntity payload = new ByteArrayEntity(certificate.getEncoded());
                    MapperResponse mapperResponse = callExternalMapper(payload);
                    if (mapperResponse != null) {
                        return mapperResponse.getUserId().trim();
                    }
                } catch (CertificateEncodingException e) {
                    log.error("Can`t get encoded data from certificate", e);
                }
            } else {
                log.warn("No certificate found in the authentication source.");
            }
        } else {
            log.debug("The used authentication source type is {} and not X509", authSource.getType());
        }
        return null;
    }

}
