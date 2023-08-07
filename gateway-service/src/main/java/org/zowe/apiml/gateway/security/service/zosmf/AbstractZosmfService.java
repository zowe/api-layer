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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.EurekaUtils;

import javax.net.ssl.SSLHandshakeException;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

import static org.zowe.apiml.security.SecurityUtils.readPassword;

@Slf4j
public abstract class AbstractZosmfService {

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

    protected AbstractZosmfService(
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
     *
     * @param authentication credentials to generates header value
     * @return prepared header value (see header Authentication)
     */
    protected String getAuthenticationValue(Authentication authentication) {
        final String user = authentication.getPrincipal().toString();
        char[] password = null;
        byte[] credentials = null;
        boolean cleanup = false;
        try {
            if (authentication.getCredentials() instanceof LoginRequest) {
                LoginRequest loginRequest = (LoginRequest) authentication.getCredentials();
                password = loginRequest.getPassword();
            } else {
                password = readPassword(authentication.getCredentials());
                cleanup = !(authentication.getCredentials() instanceof char[]);
            }

            final byte[] userByteArray = user.getBytes(StandardCharsets.UTF_8);
            credentials = new byte[userByteArray.length + 1 + password.length];

            int j = 0;
            for (byte b : userByteArray) {
                credentials[j++] = b;
            }
            credentials[j++] = (byte) ':';
            for (char c : password) {
                credentials[j++] = (byte) c;
            }

            return "Basic " + Base64.getEncoder().encodeToString(credentials);
        } finally {
            if (credentials != null) {
                Arrays.fill(credentials, (byte) 0);
            }
            if (cleanup) {
                Arrays.fill(password, (char) 0);
            }
        }
    }

    /**
     * Return z/OSMF instance uri
     *
     * @param zosmf the z/OSMF service id
     * @return the uri
     *
     * @throws ServiceNotAccessibleException if z/OSMF is not available in discovery service
     */
    protected String getURI(String zosmf) {
        Supplier<ServiceNotAccessibleException> authenticationServiceExceptionSupplier = () -> {
            log.debug("z/OSMF instance not found or incorrectly configured.");
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
     * @param re  original exception
     * @return translated exception
     */
    protected RuntimeException handleExceptionOnCall(String url, RuntimeException re) {
        if (re instanceof ResourceAccessException) {
            if (re.getCause() instanceof SSLHandshakeException) {
                apimlLog.log("org.zowe.apiml.security.auth.zosmf.sslError");
            } else {
                apimlLog.log("org.zowe.apiml.security.serviceUnavailable", url, re.getMessage());
            }
            log.debug("ResourceAccessException accessing {}", url, re);
            return new ServiceNotAccessibleException("Could not get an access to z/OSMF service.", re);
        }

        if (re instanceof HttpClientErrorException.Unauthorized) {
            log.warn("Request to z/OSMF requires authentication", re.getMessage());
            return new BadCredentialsException("Invalid Credentials");
        }

        if (re instanceof RestClientResponseException) {
            RestClientResponseException responseException = (RestClientResponseException) re;
            if (log.isTraceEnabled()) {
                log.trace("z/OSMF request {} failed with status code {}, server response: {}", url, responseException.getRawStatusCode(), responseException.getResponseBodyAsString());
            } else {
                log.debug("z/OSMF request {} failed with status code {}", url, responseException.getRawStatusCode());
            }
        }

        if (re.getCause() instanceof ConnectException) {
            apimlLog.log("org.zowe.apiml.security.auth.zosmf.connectError", re.getMessage());
            return new ServiceNotAccessibleException("Could not connect to z/OSMF service.");
        }

        if (re instanceof RestClientException) {
            log.debug("z/OSMF isn't accessible. {}", re.getMessage());
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
}
