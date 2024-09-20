/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.auth.saf.SafMethodSecurityExpressionRoot;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessDummy;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;
import org.zowe.apiml.security.common.content.BasicContentFilter;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SafMethodSecurityExpressionControllerTest.TestController.class)
@ContextConfiguration(classes = {
    SecurityControllerExceptionHandler.class,
    SafMethodSecurityExpressionControllerTest.SecurityConfiguration.class,
    SafSecurityConfigurationProperties.class,
    SafMethodSecurityExpressionControllerTest.TestController.class
})
@ActiveProfiles("SafMethodSecurityExpressionControllerTest")
class SafMethodSecurityExpressionControllerTest {

    private final static String PASSWORD = "user";
    private final static String USERNAME = "user";

    private final static String AUTHORIZATION_HEADER = "Basic " + Base64.encodeBase64String((
        USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8)
    );

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"hasSafResourceAccess", "hasSafServiceResourceAccess"})
    void testHasSafResourceAccess_whenHaveSafResourceAccess_thenReturn200(String testPrefix) throws Exception {
        mockMvc
            .perform(
                get("/gateway/" + testPrefix + "Read")
                    .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"hasSafResourceAccess", "hasSafServiceResourceAccess"})
    void testHasSafResourceAccess_whenDontHaveSafResourceAccess_thenReturn403(String testPrefix) throws Exception {
        mockMvc
            .perform(
                get("/gateway/" + testPrefix + "Update")
                    .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSecurityAccess_whenWrongCredentials_thenReturn401() throws Exception {
        mockMvc
            .perform(
                get("/gateway/test")
                    .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER + "xxx"))
            .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    @EnableMethodSecurity
    @Profile("SafMethodSecurityExpressionControllerTest")
    public static class SecurityConfiguration {

        @Bean
        public SafResourceAccessVerifying safResourceAccessVerifying() throws IOException {
            return new SafResourceAccessDummy();
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public MessageService messageService() {
            return new YamlMessageService("/security-service-messages.yml");
        }

        @Bean
        public ResourceAccessExceptionHandler resourceAccessExceptionHandler() {
            return new ResourceAccessExceptionHandler(messageService(), objectMapper());
        }

        @Bean
        public AuthExceptionHandler authExceptionHandler() {
            return new AuthExceptionHandler(messageService(), objectMapper());
        }

        @Bean
        public FailedAuthenticationHandler failedAuthenticationHandler() {
            return new FailedAuthenticationHandler(authExceptionHandler());
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .with(new CustomSecurityFilters(), Customizer.withDefaults())
                    .build();
        }

        @Bean
        public SafMethodSecurityExpressionRoot safMethodSecurityExpressionRoot(
            SafSecurityConfigurationProperties safSecurityConfigurationProperties,
            SafResourceAccessVerifying safResourceAccessVerifying
        ) {
            return new SafMethodSecurityExpressionRoot(safSecurityConfigurationProperties, safResourceAccessVerifying);
        }

        private class CustomSecurityFilters extends AbstractHttpConfigurer<CustomSecurityFilters, HttpSecurity> {
            @Override
            public void configure(HttpSecurity http) {
                AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

                http.addFilterBefore(new BasicContentFilter(
                    authenticationManager,
                    failedAuthenticationHandler(),
                    resourceAccessExceptionHandler()
                ) {
                    // test provider does not support char[] as credentials. This replaces it with a String
                    public Optional<AbstractAuthenticationToken> extractContent(HttpServletRequest request) {
                        return super.extractContent(request).map(a -> new UsernamePasswordAuthenticationToken(a.getPrincipal(), new String((char[]) a.getCredentials())));
                    }
                }, UsernamePasswordAuthenticationFilter.class);
            }
        }

        @Autowired
        public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication().withUser(USERNAME).password("{noop}" + PASSWORD).roles("TEST");
        }

    }

    @RestController
    @RequestMapping("/gateway")
    @PreAuthorize("isAuthenticated()")
    public static class TestController {

        private ResponseEntity<String> getResponse() {
            return new ResponseEntity<>("It is OK", HttpStatus.OK);
        }

        @GetMapping(value = "/hasSafResourceAccessRead", produces = MediaType.APPLICATION_JSON_VALUE)
        @PreAuthorize("@safMethodSecurityExpressionRoot.hasSafResourceAccess('CLASS', 'RESOURCE', 'READ', #root)")
        public ResponseEntity<String> test1read() {
            return getResponse();
        }

        @GetMapping(value = "/hasSafResourceAccessUpdate", produces = MediaType.APPLICATION_JSON_VALUE)
        @PreAuthorize("@safMethodSecurityExpressionRoot.hasSafResourceAccess('CLASS', 'RESOURCE', 'UPDATE', #root)")
        public ResponseEntity<String> test1update() {
            return getResponse();
        }

        @GetMapping(value = "/hasSafServiceResourceAccessRead", produces = MediaType.APPLICATION_JSON_VALUE)
        @PreAuthorize("@safMethodSecurityExpressionRoot.hasSafServiceResourceAccess('RESOURCE', 'READ', #root)")
        public ResponseEntity<String> test2read() {
            return getResponse();
        }

        @GetMapping(value = "/hasSafServiceResourceAccessUpdate", produces = MediaType.APPLICATION_JSON_VALUE)
        @PreAuthorize("@safMethodSecurityExpressionRoot.hasSafServiceResourceAccess('RESOURCE', 'UPDATE', #root)")
        public ResponseEntity<String> test2update() {
            return getResponse();
        }

    }

}
