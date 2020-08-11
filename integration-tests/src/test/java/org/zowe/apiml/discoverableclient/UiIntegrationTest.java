/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discoverableclient;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

class UiIntegrationTest {
    @Test
    @TestsNotMeantForZowe
    void shouldCallDiscoverableUiWithSlashAtPathEnd() throws Exception {
        // Given
        HttpGet request = HttpRequestUtils.getRequest("/discoverableclient/ui/v1/");

        // When
        final HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
    }

    @Test
    @TestsNotMeantForZowe
    void shouldRedirectToDiscoverableUiWithoutSlashAtPathEnd() throws Exception {
        // Given
        HttpGet request = HttpRequestUtils.getRequest("/discoverableclient/ui/v1");

        // When
        final HttpResponse response = HttpClientUtils.client(true).execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_MOVED_TEMPORARILY));
    }

    @Test
    @TestsNotMeantForZowe
    void shouldCallDiscoverableUiWithSlashAtPathEnd_OldPathFormat() throws Exception {
        // Given
        HttpGet request = HttpRequestUtils.getRequest("/ui/v1/discoverableclient/");

        // When
        final HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
    }

    @Test
    @TestsNotMeantForZowe
    void shouldRedirectToDiscoverableUiWithoutSlashAtPathEnd_OldPathFormat() throws Exception {
        // Given
        HttpGet request = HttpRequestUtils.getRequest("/ui/v1/discoverableclient");

        // When
        final HttpResponse response = HttpClientUtils.client(true).execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_MOVED_TEMPORARILY));
    }
}
