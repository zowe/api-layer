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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.service.TokenCreationService;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * Common implementation of an external mapper to call identity mapping API in the ZSS on mainframe.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class ExternalMapper {

    private final CloseableHttpClient httpClientProxy;
    private final TokenCreationService tokenCreationService;
    private final Type mapperType;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${apiml.security.x509.externalMapperUrl}")
    private String externalMapperUrl;
    @Value("${apiml.security.x509.externalMapperUser}")
    private String externalMapperUser;

    MapperResponse callExternalMapper(@NotNull HttpEntity payload) {
        try {
            HttpPost httpPost = new HttpPost(new URI(externalMapperUrl + mapperType.getUrlSuffix()));
            httpPost.setEntity(payload);

            String jwtToken = tokenCreationService.createJwtTokenWithoutCredentials(externalMapperUser);
            httpPost.setHeader(new BasicHeader("Cookie", ApimlConstants.COOKIE_AUTH_NAME + "=" + jwtToken));
            log.debug("Executing request against external mapper API: {}", httpPost);

            HttpResponse httpResponse = httpClientProxy.execute(httpPost);

            final int statusCode = httpResponse.getStatusLine() != null ? httpResponse.getStatusLine().getStatusCode() : 0;
            if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                log.error("Unexpected response from external mapper. Status: {}", httpResponse.getStatusLine());
                return null;
            }
            String response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            log.debug("External mapper API returned: {}", response);
            if (response == null || response.isEmpty()) {
                log.error("Unexpected empty response from external mapper.");
                return null;
            }
            return objectMapper.readValue(response, MapperResponse.class);
        } catch (URISyntaxException e) {
            log.error("Wrong URI provided ", e);
        } catch (IOException e) {
            log.error("Error occurred while communicating with external mapper", e);
        }
        return null;
    }

    enum Type {
        X509("/x509/map"),
        OAUTH2("/oauth2/map");

        @Getter
        private final String urlSuffix;

        Type(String urlSuffix) {
            this.urlSuffix = urlSuffix;
        }
    }
}
