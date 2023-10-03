/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebSecurityTest {

    private ReactiveUserDetailsService reactiveUserDetailsService;

    @Nested
    class WhenListOfAllowedUserDefined {
        @BeforeEach
        void setUp() {
            WebSecurity webSecurity = new WebSecurity();
            ReflectionTestUtils.setField(webSecurity, "allowedUsers", "registryUser,registryAdmin");
            webSecurity.initScopes();
            reactiveUserDetailsService = webSecurity.userDetailsService();
        }

        @Test
        void shouldAddRegistryAuthorityToAllowedUser() {
            Mono<UserDetails> userDetailsMono = reactiveUserDetailsService.findByUsername("registryUser");

            StepVerifier.create(userDetailsMono)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("registryUser");
                    assertThat(details.getAuthorities()).hasSize(1);
                    assertThat(details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                        .containsExactly("REGISTRY");
                })
                .verifyComplete();
        }

        @Test
        void shouldAddRegistryAuthorityToAllowedUserIgnoringCase() {
            Mono<UserDetails> userDetailsMono = reactiveUserDetailsService.findByUsername("registryadmin");

            StepVerifier.create(userDetailsMono)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("registryadmin");
                    assertThat(details.getAuthorities()).hasSize(1);
                    assertThat(details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                        .containsExactly("REGISTRY");
                })
                .verifyComplete();
        }

        @Test
        void shouldNotAddRegistryAuthorityToUnknownUser() {
            Mono<UserDetails> userDetailsMono = reactiveUserDetailsService.findByUsername("unknownUser");

            StepVerifier.create(userDetailsMono)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("unknownUser");
                    assertThat(details.getAuthorities()).isEmpty();
                })
                .verifyComplete();
        }
    }

    @Nested
    class WhenAnyUsersWildcardDefined {
        @BeforeEach
        void setUp() {
            WebSecurity webSecurity = new WebSecurity();
            ReflectionTestUtils.setField(webSecurity, "allowedUsers", "*");
            webSecurity.initScopes();
            reactiveUserDetailsService = webSecurity.userDetailsService();
        }

        @Test
        void shouldAddRegistryAuthorityToAnyUser() {
            Mono<UserDetails> userDetailsMono = reactiveUserDetailsService.findByUsername("guest");

            StepVerifier.create(userDetailsMono)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("guest");
                    assertThat(details.getAuthorities()).hasSize(1);
                    assertThat(details.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                        .containsExactly("REGISTRY");
                })
                .verifyComplete();
        }
    }
}
