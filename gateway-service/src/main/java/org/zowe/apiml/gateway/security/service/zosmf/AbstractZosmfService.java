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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.login.LoginProvider;
import org.zowe.apiml.gateway.security.service.ZosmfService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.util.EurekaUtils;

import java.util.*;
import java.util.function.Supplier;

public abstract class AbstractZosmfService implements ZosmfService {

    protected static final String ZOSMF_INFO_END_POINT = "/zosmf/info";
    protected static final String ZOSMF_AUTHENTICATE_END_POINT = "/zosmf/services/authenticate";
    protected static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";
    protected static final String ZOSMF_DOMAIN = "zosmf_saf_realm";

    @InjectApimlLogger
    protected ApimlLogger apimlLog = ApimlLogger.empty();

    protected final AuthConfigurationProperties authConfigurationProperties;
    protected final DiscoveryClient discovery;
    protected final RestTemplate restTemplateWithoutKeystore;
    protected final ObjectMapper securityObjectMapper;

    public AbstractZosmfService(
        AuthConfigurationProperties authConfigurationProperties,
        DiscoveryClient discovery,
        @Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore,
        ObjectMapper securityObjectMapper
    ) {
        this.authConfigurationProperties = authConfigurationProperties;
        this.discovery = discovery;
        this.restTemplateWithoutKeystore = restTemplateWithoutKeystore;
        this.securityObjectMapper = securityObjectMapper;
    }

    /**
     * @return serviceId of z/OSMF service from configuration, which is used
     */
    protected String getZosmfServiceId() {
        return authConfigurationProperties.validatedZosmfServiceId();
    }

    /**
     * Methods construct the value of authentication header by credentials
     * @param authentication credentials to generates header value
     * @return prepared header value (see header Authentication)
     */
    protected String getAuthenticationValue(Authentication authentication) {
        final String user = authentication.getPrincipal().toString();
        final String password = authentication.getCredentials().toString();

        final String credentials = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    /**
     * Return z/OSMF instance uri
     *
     * @param zosmf the z/OSMF service id
     * @return the uri
     */
    protected String getURI(String zosmf) {
        Supplier<ServiceNotAccessibleException> authenticationServiceExceptionSupplier = () -> {
            apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", zosmf);
            return new ServiceNotAccessibleException("z/OSMF instance not found or incorrectly configured.");
        };

        return Optional.ofNullable(discovery.getApplication(zosmf))
            .orElseThrow(authenticationServiceExceptionSupplier)
            .getInstances()
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .map(EurekaUtils::getUrl)
            .orElseThrow(authenticationServiceExceptionSupplier);
    }

    /**
     * Method handles exception from REST call to z/OSMF into internal exception. It convert original exception into
     * custom one with better messages and types for subsequent treatment.
     *
     * @param url URL of invoked REST endpoint
     * @param re original exception
     * @return translated exception
     */
    protected RuntimeException handleExceptionOnCall(String url, RuntimeException re) {
        if (re instanceof ResourceAccessException) {
            apimlLog.log("org.zowe.apiml.security.serviceUnavailable", url, re.getMessage());
            return new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");
        }

        if (re instanceof HttpClientErrorException.Unauthorized) {
            return new BadCredentialsException("Username or password are invalid.");
        }

        if (re instanceof RestClientException) {
            apimlLog.log("org.zowe.apiml.security.generic", re.getMessage(), url);
            return new AuthenticationServiceException("A failure occurred when authenticating.", re);
        }

        return re;
    }

    /**
     * Read the token with name cookieName from the cookies
     *
     * @param cookies the cookies
     * @return the token if is set in cookies, otherwise null
     */
    protected String readTokenFromCookie(List<String> cookies, String cookieName) {
        if (cookies == null) return null;

        return cookies.stream()
            .filter(x -> x.startsWith(cookieName + "="))
            .findFirst()
            .map(x -> {
                final int beginIndex = cookieName.length() + 1;
                final int endIndex = x.indexOf(';');
                return endIndex > 0 ? x.substring(beginIndex, endIndex) : x.substring(beginIndex);
            })
            .orElse(null);
    }

    /**
     * Method reads authentication values from answer of REST call. It read all supported tokens, which are returned
     * from z/OSMF.
     *
     * @param responseEntity answer of REST call
     * @return AuthenticationResponse with all supported tokens from responseEntity
     */
    protected AuthenticationResponse getAuthenticationResponse(ResponseEntity<String> responseEntity) {
        final List<String> cookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
        final EnumMap<TokenType, String> tokens = new EnumMap<>(TokenType.class);
        if (cookies != null) {
            for (final TokenType tokenType : TokenType.values()) {
                final String token = readTokenFromCookie(cookies, tokenType.getCookieName());
                if (token != null) tokens.put(tokenType, token);
            }
        }
        return new AuthenticationResponse(tokens);
    }

    @Override
    public boolean isAvailable() {
        return discovery.getApplication(authConfigurationProperties.validatedZosmfServiceId()) != null;
    }

    @Override
    public boolean isUsed() {
        return authConfigurationProperties.getProvider().equalsIgnoreCase(LoginProvider.ZOSMF.toString());
    }

}
