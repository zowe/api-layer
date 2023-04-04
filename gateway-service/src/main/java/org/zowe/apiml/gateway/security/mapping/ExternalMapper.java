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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * Common implementation of an external mapper to call identity mapping API in the ZSS on mainframe.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class ExternalMapper {

    private final CloseableHttpClient httpClientProxy;
    private final TokenCreationService tokenCreationService;
    private final Type mapperType;
    private final AuthConfigurationProperties authConfigurationProperties;

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    //TODO: shouldn't we rename it (remove X509) (without breaking change)
    @Value("${apiml.security.x509.externalMapperUrl:}")
    private String externalMapperUrl;
    @Value("${apiml.security.x509.externalMapperUser:}")
    private String externalMapperUser;

    MapperResponse callExternalMapper(@NotNull HttpEntity payload) {
        if (StringUtils.isBlank(externalMapperUser)) {
            throw new ExternalMapperException("Configuration error: External identity mapper user is empty.");
        }
        try {
            HttpPost httpPost = new HttpPost(getMapperURI());
            httpPost.setEntity(payload);

            String jwtToken = tokenCreationService.createJwtTokenWithoutCredentials(externalMapperUser);
            httpPost.setHeader(new BasicHeader("Cookie", authConfigurationProperties.getCookieProperties().getCookieName() + "=" + jwtToken));
            log.debug("Executing request against external identity mapper API: {}", httpPost);

            HttpResponse httpResponse = httpClientProxy.execute(httpPost);

            final int statusCode = httpResponse.getStatusLine() != null ? httpResponse.getStatusLine().getStatusCode() : 0;
            String response = "";
            if (httpResponse.getEntity() != null) {
                response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            }
            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                String message = MessageFormat.format("Unexpected response from the external identity mapper. Status: {0} body: {1}",
                    statusCode, response);
                throw new ExternalMapperException(message);
            }
            log.debug("External identity mapper API returned: {}", response);
            if (StringUtils.isEmpty(response)) {
                throw new ExternalMapperException("Unexpected empty response from external identity mapper.");
            }
            return objectMapper.readValue(response, MapperResponse.class);
        } catch (IOException e) {
            throw new ExternalMapperException("Error occurred while communicating with external identity mapper", e);
        }
    }

    enum Type {
        X509("/x509/map"),
        OIDC("/dn");

        @Getter
        private final String urlSuffix;

        Type(String urlSuffix) {
            this.urlSuffix = urlSuffix;
        }
    }

    private URI getMapperURI() {
        if (StringUtils.isBlank(externalMapperUrl)) {
            throw new ExternalMapperException("Configuration error: External identity mapper URL is empty.");
        } else {
            // do not introduce braking change - if externalMapperUrl for x509 has been already configured
            // with the /x509/map at the end then make sure to remove the suffix before
            // the mapper URI is constructed properly with the mapperType suffix
            String url = StringUtils.removeEndIgnoreCase(externalMapperUrl, mapperType.getUrlSuffix());
            url = StringUtils.removeEnd(url, "/");
            try {
                return new URI(url + mapperType.getUrlSuffix());
            } catch (URISyntaxException e) {
                throw new ExternalMapperException("Configuration error: Failed to construct the external identity mapper URL.", e);
            }
        }
    }
}
