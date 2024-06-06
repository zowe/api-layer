/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaasclient.service.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.zaasclient.config.ConfigProperties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ZaasClientApimlBaseUrlTest {

    private static final String ZOWE_V2_BASE_URL = "/gateway/api/v1/auth";

    @ParameterizedTest
    @ValueSource(strings = {"/gateway/api/v1/auth", "gateway/api/v1/auth"})
    void givenBaseUrl_thenTransformToOrDontChangeZoweV2BaseUrl(String baseUrl) {
        ConfigProperties configProperties = new ConfigProperties();
        configProperties.setApimlBaseUrl(baseUrl);
        assertThat(configProperties.getApimlBaseUrl(), is(ZOWE_V2_BASE_URL));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/v1/zaasClient/auth,/api/v1/zaasClient/auth",
        "api/v1/zaasClient/auth,/api/v1/zaasClient/auth",
        "anyUrl,/anyUrl",
        "anyUrl/,/anyUrl/",
        "api/v1/gateway,/gateway/api/v1",
        "api/v1/gateway/x,/gateway/api/v1/x",
        "/gateway/api/v1,/gateway/api/v1",
        "gateway/api/v1,/gateway/api/v1",
        "/gateway/api/v1/x,/gateway/api/v1/x",
        "gateway/api/v1/x,/gateway/api/v1/x",
        "/anyOther/gateway/doNotChange,/anyOther/gateway/doNotChange",
        "anyOther/gateway/doNotChange,/anyOther/gateway/doNotChange"
    })
    void givenBaseUrl_thenNormalizeIt(String input, String normalized) {
        ConfigProperties configProperties = new ConfigProperties();
        configProperties.setApimlBaseUrl(input);
        assertThat(configProperties.getApimlBaseUrl(), is(normalized));
    }

    @Test
    void givenBaseUrlIsNull_thenTransformToZoweV2BaseUrl() {
        ConfigProperties configProperties = new ConfigProperties();
        configProperties.setApimlBaseUrl(null);
        assertThat(configProperties.getApimlBaseUrl(), is(ZOWE_V2_BASE_URL));
    }
}
