/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import lombok.SneakyThrows;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.forwardHeader.trustedProxies="
})
@ActiveProfiles("forward-headers-proxy-test")
class ForwardedHeadersProxyTest extends AcceptanceTestWithTwoServices {

    String OTHER_PROXY_ADDRESS = "2.2.2.2";
    String OTHER_PROXY_PREFIX = "/untrusted-proxy-prefix";

    String serviceUrl;

    @BeforeEach
    @SneakyThrows
    void setup() {
        serviceUrl = basePath + serviceWithDefaultConfiguration.getPath();

        // Tell the infrastructure to work with the request for serviceid
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockValid200HttpResponse();
    }

    @Test
    void whenNoXForwardHeadersInRequest_ThenXForwardHeadersCreated() {
        given()
            .log().all()
        .when()
            .get(serviceUrl)
        .then()
            .statusCode(Matchers.is(SC_OK));

        HttpUriRequest toVerify = getCaptorToEvaluate();

        assertHeaderEqualsValue(toVerify, "X-Forwarded-Host", "localhost:" + port);
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Prefix", "/" + serviceWithDefaultConfiguration.getId());
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Port", String.valueOf(port));
    }

    @Test
    void whenXForwardHeadersInRequest_ThenNoXForwardHeadersForwarded() {
        given()
            .log().all()
            .header("x-Forwarded-for", OTHER_PROXY_ADDRESS)
            .header("X-forwarded-prefix", OTHER_PROXY_PREFIX)
            .header("forwarded", "for=" + OTHER_PROXY_ADDRESS + ";prefix=/test")
        .when()
            .get(serviceUrl)
        .then()
            .statusCode(Matchers.is(SC_OK));

        HttpUriRequest toVerify = getCaptorToEvaluate();

        assertHeaderEqualsValue(toVerify, "X-Forwarded-Prefix",  "/" + serviceWithDefaultConfiguration.getId());
        //Zuul does not validate the content of headers, hence the value with null is passed to the mockClient
        assertHeaderNullValue(toVerify, "X-Forwarded-For");
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Host", "localhost:" + port);
        assertThat(toVerify.getHeaders("Forwarded").length, is(0));
    }

    @Test
    void whenXForwardHeadersInRequestFromGW_ThenXForwardHeadersForwarded() {
        doReturn(true).when(mockCertificateValidator).isTrusted(any());
        given()
            .header("x-forwarded-For", OTHER_PROXY_ADDRESS)
            .header("X-forwarded-Prefix", OTHER_PROXY_PREFIX)
        .when()
            .get(serviceUrl)
        .then()
            .statusCode(Matchers.is(SC_OK));

        HttpUriRequest toVerify = getCaptorToEvaluate();

        assertHeaderEqualsValue(toVerify, "X-Forwarded-Prefix", OTHER_PROXY_PREFIX + "/" + serviceWithDefaultConfiguration.getId());
        assertHeaderContainsValue(toVerify, "X-Forwarded-For", OTHER_PROXY_ADDRESS);
        assertHeaderContainsValue(toVerify, "X-Forwarded-For", proxyAddress);

        assertHeaderEqualsValue(toVerify, "X-Forwarded-Host", "localhost:" + port);
    }

    @SneakyThrows
    private HttpUriRequest getCaptorToEvaluate() {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(mockClient, times(1)).execute(captor.capture());

        return captor.getValue();
    }
}
