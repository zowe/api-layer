/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.zosmf;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;

import java.util.Map.Entry;

import static org.zowe.apiml.gateway.security.service.zosmf.AbstractZosmfService.ZOSMF_CSRF_HEADER;

/**
 * Strategy to validate token through Authentication endpoint of zOSMF
 */
@RequiredArgsConstructor
public class AuthenticatedEndpointStrategy implements TokenValidationStrategy {

    private final RestTemplate restTemplateWithoutKeystore;


    @InjectApimlLogger
    protected ApimlLogger apimlLog = ApimlLogger.empty();

    public final String authenticatedEndpoint;

    private final HttpMethod httpMethod;

    @Override
    public void validate(TokenValidationRequest request) {

        final String url = request.getZosmfBaseUrl() + authenticatedEndpoint;
        String errorReturned = "Endpoint does not exist";

        if (endpointExists(request, authenticatedEndpoint)) {
            try {
                final HttpHeaders headers = new HttpHeaders();
                headers.add(ZOSMF_CSRF_HEADER, "");
                headers.add(HttpHeaders.COOKIE, ZosmfService.TokenType.JWT.getCookieName() + "=" + request.getToken());


                ResponseEntity<String> re = restTemplateWithoutKeystore.exchange(url, httpMethod,
                    new HttpEntity<>(null, headers), String.class);

                if (re.getStatusCode().is2xxSuccessful()) {
                    request.setAuthenticated(TokenValidationRequest.STATUS.AUTHENTICATED);
                    return;
                }
                if (HttpStatus.UNAUTHORIZED.equals(re.getStatusCode())) {
                    request.setAuthenticated(TokenValidationRequest.STATUS.INVALID);
                    return;
                }
                errorReturned = String.valueOf(re.getStatusCode());

            } catch (HttpClientErrorException.Unauthorized e) {
                request.setAuthenticated(TokenValidationRequest.STATUS.INVALID);
                return;
            }
        }

        apimlLog.log("org.zowe.apiml.security.serviceUnavailable", url, errorReturned);
        throw new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");
    }

    private boolean endpointExists(TokenValidationRequest request, String endpoint) {
        if (request.getEndpointExistenceMap() == null || request.getEndpointExistenceMap().isEmpty()) {
            return true;
        } else {
            return request.getEndpointExistenceMap().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(request.getZosmfBaseUrl() + endpoint))
                .findFirst().map(Entry::getValue).orElse(true);
        }
    }

    public String toString() {
        return "AuthenticatedEndpointStrategy{endpoint=" + authenticatedEndpoint + "}";
    }

}
