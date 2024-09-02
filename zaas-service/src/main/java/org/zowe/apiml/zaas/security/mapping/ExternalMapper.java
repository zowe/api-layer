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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.zaas.security.mapping.model.MapperResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;

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
    @Autowired
    @Qualifier("secureHttpClientWithoutKeystore")
    private final CloseableHttpClient secureHttpClientWithoutKeystore;
    private final TokenCreationService tokenCreationService;
    private final AuthConfigurationProperties authConfigurationProperties;
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    @InjectApimlLogger
    protected ApimlLogger apimlLog = ApimlLogger.empty();

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

            var response = secureHttpClientWithoutKeystore.execute(httpPost, httpResponse -> {
                final int statusCode = httpResponse.getCode();
                String responseBody = "";
                if (httpResponse.getEntity() != null) {
                    responseBody = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                }
                if (statusCode == 0) {
                    return null;
                }
                if (!org.springframework.http.HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
                    if (org.springframework.http.HttpStatus.valueOf(statusCode).is5xxServerError()) {
                        apimlLog.log("org.zowe.apiml.zaas.security.unexpectedMappingResponse", statusCode, httpResponse);
                    } else {
                        log.debug("Unexpected response from the external identity mapper. Status: {} body: {}", statusCode, httpResponse);
                    }
                    return null;
                }
                log.debug("External identity mapper API returned: {}", responseBody);
                return responseBody;
            });

            if (StringUtils.isNotEmpty(response)) {
                return objectMapper.readValue(response, MapperResponse.class);
            }
        } catch (IOException e) {
            apimlLog.log("org.zowe.apiml.zaas.security.InvalidMappingResponse", e);
        } catch (URISyntaxException e) {
            apimlLog.log("org.zowe.apiml.zaas.security.InvalidMapperUrl", e);
        }

        return null;
    }

}
