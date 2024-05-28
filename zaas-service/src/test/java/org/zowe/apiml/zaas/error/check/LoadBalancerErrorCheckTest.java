/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.error.check;

import com.netflix.client.ClientException;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.zaas.config.MessageServiceConfiguration;
import org.zowe.apiml.zaas.error.ErrorUtils;
import org.zowe.apiml.zaas.error.controllers.InternalServerErrorController;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MessageServiceConfiguration.class})
class LoadBalancerErrorCheckTest {

    private static InternalServerErrorController underTest;

    @Autowired
    private MessageService messageService;

    @BeforeAll
    public static void setupAll() {
        MonitoringHelper.initMocks();
    }

    @BeforeEach
    public void setup() {
        underTest = new InternalServerErrorController(messageService);
    }

    private static Stream<Arguments> provideExceptionsWithRelevantTexts() {
        return Stream.of(
            Arguments.of("givenExceptionChain_whenIsClientExceptionFromLoadBalancer_thenServiceUnavailable",
                new ClientException("Load balancer does not have available server for client: discoverableclient"),
                "Load balancer does not have available server for client: discoverableclient",
                "org.zowe.apiml.zaas.loadBalancerDoesNotHaveAvailableServer",
                HttpStatus.SERVICE_UNAVAILABLE
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideExceptionsWithRelevantTexts")
    void givenExceptionChain_whenExceptionIsRaised_thenAdequatelyHandle(String description, Exception toWrap, String message, String key, HttpStatus statusCode) {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ZuulException exc = new ZuulException(new Exception(toWrap), HttpStatus.INTERNAL_SERVER_ERROR.value(), "");
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> response = underTest.error(request);

        assertCorrectResponse(response, message, statusCode, key);
    }

    private void assertCorrectResponse(ResponseEntity<ApiMessageView> response, String expectedMessage, HttpStatus expectedStatus, String expectedKey) {
        assertThat(response.getStatusCodeValue(), is(expectedStatus.value()));
        ApiMessage firstMessage = response.getBody().getMessages().get(0);
        assertThat(firstMessage.getMessageKey(), is(expectedKey));
        assertThat(firstMessage.getMessageContent(), containsString(expectedMessage));
    }
}
