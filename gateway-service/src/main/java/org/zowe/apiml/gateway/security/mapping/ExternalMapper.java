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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

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

    private final String mapperUrl;
    private final String mapperUser;
    private final CloseableHttpClient httpClientProxy;
    private final TokenCreationService tokenCreationService;
    private final AuthConfigurationProperties authConfigurationProperties;

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    MapperResponse callExternalMapper(@NotNull HttpEntity payload) {
        if (StringUtils.isBlank(mapperUrl)) {
            log.warn("Configuration error: External identity mapper URL is not set.");
            return null;
        }
        if (StringUtils.isBlank(mapperUser)) {
            log.warn("Configuration error: External identity mapper user is not set.");
            return null;
        }
        try {
            HttpPost httpPost = new HttpPost(new URI(mapperUrl));
            httpPost.setEntity(payload);

            String jwtToken = tokenCreationService.createJwtTokenWithoutCredentials(mapperUser);
            httpPost.setHeader(new BasicHeader("Cookie", authConfigurationProperties.getCookieProperties().getCookieName() + "=" + jwtToken));
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            log.debug("Executing request against external identity mapper API: {}", httpPost);

            HttpResponse httpResponse = httpClientProxy.execute(httpPost);

            final int statusCode = httpResponse.getStatusLine() != null ? httpResponse.getStatusLine().getStatusCode() : 0;
            String response = "";
            if (httpResponse.getEntity() != null) {
                response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            }
            if (!org.springframework.http.HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
                if (org.springframework.http.HttpStatus.valueOf(statusCode).is5xxServerError()) {
                    log.error("Unexpected response from the external identity mapper. Status: {} body: {}", statusCode, response);
                } else {
                    log.debug("Unexpected response from the external identity mapper. Status: {} body: {}", statusCode, response);
                }
                return null;
            }
            log.debug("External identity mapper API returned: {}", response);
            if (StringUtils.isNotEmpty(response)) {
                return objectMapper.readValue(response, MapperResponse.class);
            }
        } catch (IOException e) {
            log.error("Error occurred while communicating with external identity mapper", e);
        } catch (URISyntaxException e) {
            log.error("Configuration error: Failed to construct the external identity mapper URI.", e);
        }

        return null;
    }

}
