/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.proxy;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@DiscoverableClientDependentTest
class UiIntegrationTest implements TestWithStartedInstances {
    protected static String[] discoverableClientSource() {
        return new String[]{
            "/discoverableclient/ui/v1",
            "/ui/v1/discoverableclient"
        };
    }

    @ParameterizedTest
    @MethodSource("discoverableClientSource")
    @TestsNotMeantForZowe
    void shouldCallDiscoverableUiWithSlashAtPathEnd(String url) throws Exception {
        // Given
        HttpGet request = HttpRequestUtils.getRequest(url + "/");

        // When
        final HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
    }

    @ParameterizedTest
    @MethodSource("discoverableClientSource")
    @TestsNotMeantForZowe
    void shouldRedirectToDiscoverableUiWithoutSlashAtPathEnd(String url) throws Exception {
        // Given
        HttpGet request = HttpRequestUtils.getRequest(url);

        // When
        final HttpResponse response = HttpClientUtils.client(true).execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_MOVED_TEMPORARILY));
    }
}
