/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Collections;

@RequiredArgsConstructor
public class SafResourceAccessEndpoint implements SafResourceAccessVerifying {

    private static final String URL_VARIABLE_SUFFIX = "/{entity}/{level}";

    @Value("${apiml.security.authorization.endpoint.url:http://localhost:8542/saf-auth}")
    private String endpointUrl;

    private final RestTemplate restTemplate;

    private <T> HttpEntity<T> createHttpEntity(Authentication authentication) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (authentication instanceof TokenAuthentication) {
            TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
            headers.set(HttpHeaders.COOKIE, ApimlConstants.COOKIE_AUTH_NAME + "=" + tokenAuthentication.getCredentials());
        }
        return new HttpEntity<>(headers);
    }

    @Override
    public boolean hasSafResourceAccess(Authentication authentication, String resourceClass, String resourceName, String accessLevel) {
        if (!StringUtils.equalsIgnoreCase("ZOWE", resourceClass)) {
            throw new UnsupportedResourceClassException(resourceClass, "The SAF provider `endpoint` supports only resource class 'ZOWE', but current one is '" + resourceClass + "'");
        }

        try {
            HttpEntity<HttpHeaders> httpEntity = createHttpEntity(authentication);
            ResponseEntity<Response> responseEntity = restTemplate.exchange(
                    endpointUrl + URL_VARIABLE_SUFFIX, HttpMethod.GET, httpEntity, Response.class, resourceName, accessLevel
            );
            Response response = responseEntity.getBody();
            if (response != null && response.isError()) {
                throw new EndpointImproprietyConfigureException("Endpoint " + endpointUrl + " is not properly configured: " + response.getMessage(), endpointUrl);
            }
            return response != null && !response.isError() && response.isAuthorized();
        } catch (EndpointImproprietyConfigureException e) {
            throw e;
        } catch (Exception e) {
            throw new EndpointImproprietyConfigureException("Endpoint " + endpointUrl + " is not properly configured.", endpointUrl, e);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        private boolean authorized;
        private boolean error;
        private String message;

    }

}
