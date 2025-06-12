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
import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.forwardHeader.trusted-proxies=${test.trustedProxiesPattern}"
})
@ActiveProfiles("forward-headers-proxy-test")
class ForwardedHeadersTrustedProxyTest extends AcceptanceTestWithTwoServices {

    String OTHER_PROXY_ADDRESS = "1.1.1.1";
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
            .get(serviceUrl)
            .then()
            .statusCode(Matchers.is(SC_OK));

        HttpUriRequest toVerify = getCaptorToEvaluate();

        assertHeaderEqualsValue(toVerify, "X-Forwarded-Host", "localhost:" + port);
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Prefix", "/" + serviceWithDefaultConfiguration.getId());
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Port", String.valueOf(port));
        assertHeaderEqualsValue(toVerify, "X-Forwarded-For", proxyAddress);
    }

    @Test
    void whenXForwardHeadersInRequest_ThenXForwardedHeadersForwarded() {
        given()
            .header("X-forwarded-For", OTHER_PROXY_ADDRESS)
            .header("X-forwarded-prefix", OTHER_PROXY_PREFIX)
            .when()
            .get(serviceUrl)
            .then()
            .statusCode(Matchers.is(SC_OK));

        assertHeadersForwarded();
    }

    @Test
    void whenXForwardHeadersInRequestFromGW_ThenXForwardedHeadersForwarded() {
        doReturn(true).when(mockCertificateValidator).isTrusted(any());
        given()
            .header("x-forwarded-For", OTHER_PROXY_ADDRESS)
            .header("X-forwarded-Prefix", OTHER_PROXY_PREFIX)
            .when()
            .get(serviceUrl)
            .then()
            .statusCode(Matchers.is(SC_OK));

        assertHeadersForwarded();
    }

    @Test
    void givenServiceWithOverwrittenTimeoutAndAnotherWithout_whenOverwritingConfigurationForOneService_thenTheOtherServicesKeepDefault() {
        when()
            .get(basePath + serviceWithDefaultConfiguration.getPath())
        .then()
            .statusCode(is(SC_OK));

        HttpUriRequest toVerify = getCaptorToEvaluate();

        assertHeaderEqualsValue(toVerify, "X-Forwarded-Host", "localhost:" + port);
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Prefix", "/" + serviceWithDefaultConfiguration.getId());
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Port", String.valueOf(port));
        assertHeaderEqualsValue(toVerify, "X-Forwarded-For", proxyAddress);
    }

    private void assertHeadersForwarded() {
        HttpUriRequest toVerify = getCaptorToEvaluate();
        assertHeaderContainsValue(toVerify, "X-Forwarded-For", proxyAddress);
        assertHeaderContainsValue(toVerify, "X-Forwarded-For", OTHER_PROXY_ADDRESS);
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Prefix", OTHER_PROXY_PREFIX + "/" + serviceWithDefaultConfiguration.getId());
    }

    @SneakyThrows
    private HttpUriRequest getCaptorToEvaluate() {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(mockClient, times(1)).execute(captor.capture());

        return captor.getValue();
    }

}
