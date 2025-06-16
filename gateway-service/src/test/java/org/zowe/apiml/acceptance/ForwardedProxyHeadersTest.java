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

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import java.io.IOException;

import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@AcceptanceTest
class ForwardedProxyHeadersTest extends AcceptanceTestWithTwoServices {
    @Test
    void givenServiceWithOverwritenTimeoutAndAnotherWithout_whenOverwritingConfigurationForOneService_thenTheOtherServicesKeepDefault() throws IOException {
        mockValid200HttpResponse();

        // Tell the infrastructure to work with the request for serviceid
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());

        when()
            .get(basePath + serviceWithDefaultConfiguration.getPath())
        .then()
            .statusCode(is(SC_OK));

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(mockClient, times(1)).execute(captor.capture());

        HttpUriRequest toVerify = captor.getValue();

        assertHeaderEqualsValue(toVerify, "X-Forwarded-Host", "localhost:" + port);
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Prefix", "/serviceid2");
        assertHeaderEqualsValue(toVerify, "X-Forwarded-Port", String.valueOf(port));
        //x-forwarded-proxy is not present because no proxy is not trusted by default
        assertHeaderNullValue(toVerify, "X-Forwarded-For");
    }
}
