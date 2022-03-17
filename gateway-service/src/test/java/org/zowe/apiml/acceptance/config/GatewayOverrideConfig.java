/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.discovery.SimpleServiceRouteMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.acceptance.common.Service;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestConfiguration
public class GatewayOverrideConfig {
    @Bean
    @Primary
    public ServiceRouteMapper serviceRouteMapper() {
        return new SimpleServiceRouteMapper();
    }

    protected static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";
    @MockBean
    @Qualifier("mockProxy")
    public CloseableHttpClient mockProxy;

    @MockBean
    @Qualifier("restTemplateWithoutKeystore")
    public RestTemplate restTemplateWithoutKeystore;

    @MockBean
    public ZosmfService zosmfService;

    @Bean
    public SimpleRouteLocator simpleRouteLocator() {
        ZuulProperties properties = new ZuulProperties();
        properties.setRoutes(new HashMap<>());

        return new SimpleRouteLocator("", properties);
    }

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Bean
    public ApplicationRegistry registry() throws Exception {
        Map<ZosmfService.TokenType, String> tokens = new HashMap<>();
        String jwt = createJwtToken("user", "zosmf", null);
        tokens.put(ZosmfService.TokenType.JWT, jwt);
        ZosmfService.AuthenticationResponse response = new ZosmfService.AuthenticationResponse("zosmf", tokens);
        when(zosmfService.authenticate(any())).thenReturn(response);

        ResponseEntity<ZosmfService.ZosmfInfo> info = mock(ResponseEntity.class);
        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");
        when(restTemplateWithoutKeystore.exchange(anyString(), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(ZosmfService.ZosmfInfo.class))).thenReturn(info);
        when(info.getStatusCode()).thenReturn(HttpStatus.OK);
        MetadataBuilder defaultBuilder = MetadataBuilder.defaultInstance();
        defaultBuilder.withZosmf();
        ApplicationRegistry applicationRegistry = new ApplicationRegistry();
        Service zosmfService = new Service("zosmf", "/zosmf/**", "zosmf");
        applicationRegistry.addApplication(zosmfService, defaultBuilder, false);
        applicationRegistry.setCurrentApplication("zosmf");
        return applicationRegistry;
    }

    public String createJwtToken(String username, String domain, String ltpaToken) {
        long now = System.currentTimeMillis();
        long expiration = now + 100_000L;
        HttpsConfig config = HttpsConfig.builder().keyAlias("localhost").keyPassword("password".toCharArray()).keyStore(keyStore).build();
        Key jwtSecret = SecurityUtils.loadKey(config);
        return Jwts.builder()
            .setSubject(username)
            .claim("dom", domain)
            .claim("ltpa", ltpaToken)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer("APIML")
            .setId(UUID.randomUUID().toString())
            .signWith(jwtSecret, SignatureAlgorithm.RS256)
            .compact();
    }
}
