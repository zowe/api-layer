/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactivePassTicketControllerTest {

    @Mock private PassTicketService passTicketService;

    @InjectMocks
    private ReactivePassTicketController controller;

    private TicketRequest ticketRequest;

    @Test
    void whenNoBody_thenInvalidRequestBody() {
        StepVerifier.create(Mono.<ResponseEntity<TicketResponse>>defer(() -> controller.createPassTicket(ticketRequest)))
            .expectErrorMatches(e -> e instanceof IncorrectPassTicketRequestBodyException)
            .verify();
    }

    @Nested
    class GivenUnauthenticated {

        @BeforeEach
        void setUp() {
            ticketRequest = new TicketRequest("ANAPP");
        }

        @Test
        void then_Unauthorized() {
            var result = controller.createPassTicket(ticketRequest);

            StepVerifier.create(result)
                .expectNextMatches(re -> re.getStatusCode().equals(HttpStatusCode.valueOf(401)))
                .verifyComplete();
        }

    }

    @Nested
    class GivenAuthenticated {

        @Mock private SecurityContext securityContext;
        @Mock private TokenAuthentication authentication;

        @BeforeEach
        void setUp() {
            ticketRequest = new TicketRequest("VALIDAP");

            when(securityContext.getAuthentication()).thenReturn(authentication);
            lenient().when(authentication.isAuthenticated()).thenReturn(true);
        }

        @Test
        void whenTokenAuth_thenGeneratePassTicket() {
            try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
                mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));
                StepVerifier.create(controller.createPassTicket(ticketRequest))
                    .expectNextMatches(re -> {
                        assertEquals(HttpStatusCode.valueOf(200), re.getStatusCode());
                        assertEquals(MediaType.APPLICATION_JSON, re.getHeaders().getContentType());
                        assertNotNull(re.getBody());
                        return true;
                    })
                    .verifyComplete();
            }

        }

        @Test
        void whenNonTokenAuth_thenUnauthorized() {
            var nonTokenAuth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(nonTokenAuth);
            try (MockedStatic<ReactiveSecurityContextHolder> mockedContextHolder = Mockito.mockStatic(ReactiveSecurityContextHolder.class)) {
                mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));
                StepVerifier.create(controller.createPassTicket(ticketRequest))
                    .expectNextMatches(re -> {
                        assertEquals(HttpStatusCode.valueOf(401), re.getStatusCode());
                        return true;
                    })
                    .verifyComplete();
            }

        }

    }

}
