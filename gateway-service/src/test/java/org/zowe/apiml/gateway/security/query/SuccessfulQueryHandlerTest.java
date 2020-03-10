/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.JwtSecurityInitializer;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfServiceV2;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.security.Key;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SuccessfulQueryHandlerTest {
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;
    private SuccessfulQueryHandler successfulQueryHandler;
    private String jwtToken;

    private static final String USER = "Me";
    private static final String DOMAIN = "this.com";
    private static final String LTPA = "ltpaToken";

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private JwtSecurityInitializer jwtSecurityInitializer;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private CacheManager cacheManager;

    @BeforeEach
    public void setup() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletResponse = new MockHttpServletResponse();
        AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

        SignatureAlgorithm algorithm = SignatureAlgorithm.RS256;
        KeyPair keyPair = SecurityUtils.generateKeyPair("RSA", 2048);
        Key privateKey = null;
        PublicKey publicKey = null;
        if (keyPair != null) {
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }
        ZosmfServiceV2 zosmfService = new ZosmfServiceV2(authConfigurationProperties, discoveryClient, restTemplate, new ObjectMapper());
        AuthenticationService authenticationService = new AuthenticationService(
            applicationContext, authConfigurationProperties, jwtSecurityInitializer, zosmfService,
            discoveryClient, restTemplate, cacheManager
        );
        when(jwtSecurityInitializer.getSignatureAlgorithm()).thenReturn(algorithm);
        when(jwtSecurityInitializer.getJwtSecret()).thenReturn(privateKey);
        when(jwtSecurityInitializer.getJwtPublicKey()).thenReturn(publicKey);

        jwtToken = authenticationService.createJwtToken(USER, DOMAIN, LTPA);

        ObjectMapper mapper = new ObjectMapper();
        successfulQueryHandler = new SuccessfulQueryHandler(mapper, authenticationService);
    }

    @Test
    public void shouldSetResponseParameters() throws Exception {
        httpServletResponse = new MockHttpServletResponse();
        TokenAuthentication tokenAuthentication = new TokenAuthentication(USER, jwtToken);
        httpServletResponse.setStatus(HttpStatus.EXPECTATION_FAILED.value());
        assertNotEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());

        successfulQueryHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());
        assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
        assertTrue(httpServletResponse.isCommitted());
    }

    @Test
    public void shouldWriteModelToBody() throws Exception {
        httpServletResponse = new MockHttpServletResponse();
        TokenAuthentication tokenAuthentication = new TokenAuthentication(USER, jwtToken);

        successfulQueryHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertNotNull(httpServletResponse.getContentAsString());
        String response = httpServletResponse.getContentAsString();

        assertTrue(response.contains("\"domain\":\"" + DOMAIN + "\""));
        assertTrue(response.contains("\"userId\":\"" + USER + "\""));
        assertTrue(response.contains("\"creation\":"));
        assertTrue(response.contains("\"expiration\":"));
    }

}
