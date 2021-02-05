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
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import static org.zowe.apiml.gateway.security.service.zosmf.AbstractZosmfService.ZOSMF_CSRF_HEADER;

/**
 * Strategy to validate token through Authentication endpoint of zOSMF
 */
@RequiredArgsConstructor
public class AuthenticateEndpointStrategy implements TokenValidationStrategy {

    private final RestTemplate restTemplateWithoutKeystore;

    @InjectApimlLogger
    protected ApimlLogger apimlLog = ApimlLogger.empty();

    public static final String ZOSMF_AUTHENTICATE_END_POINT = "/zosmf/services/authenticate";

    @Override
    public boolean validate(TokenValidationRequest request) {
        if (endpointExists(request, ZOSMF_AUTHENTICATE_END_POINT)) {
            final String url = request.getZosmfBaseUrl() + ZOSMF_AUTHENTICATE_END_POINT;

            final HttpHeaders headers = new HttpHeaders();
            headers.add(ZOSMF_CSRF_HEADER, "");
            headers.add(HttpHeaders.COOKIE, ZosmfService.TokenType.JWT.getCookieName() + "=" + request.getToken());


            ResponseEntity<String> re = restTemplateWithoutKeystore.exchange(url, HttpMethod.POST,
                new HttpEntity<>(null, headers), String.class);

            if (re.getStatusCode().is2xxSuccessful())
                return true;
            if (HttpStatus.UNAUTHORIZED.equals(re.getStatusCode())) {
                throw new TokenNotValidException("Token is not valid.");
            }
            apimlLog.log("org.zowe.apiml.security.serviceUnavailable", url, re.getStatusCodeValue());
            throw new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");

        } else {
            return false;
        }
    }

    private boolean endpointExists(TokenValidationRequest request, String endpoint) {
        if (request.getEndpointExistenceMap() == null || request.getEndpointExistenceMap().isEmpty()) {
            return true;
        } else {
            return request.getEndpointExistenceMap().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(request.getZosmfBaseUrl() + endpoint))
            .findFirst().get().getValue();
        }
    }

}
