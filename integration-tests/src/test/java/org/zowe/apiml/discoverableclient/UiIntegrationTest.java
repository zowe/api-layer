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

import org.junit.experimental.categories.Category;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class UiIntegrationTest {
    @Test
    @Category(TestsNotMeantForZowe.class)
    public void shouldCallDiscoverableUiWithSlashAtPathEnd() throws Exception {
        // Given
        HttpGet request = HttpRequestUtils.getRequest("/ui/v1/discoverableclient/");

        // When
        final HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
    }

    @Test
    @Category(TestsNotMeantForZowe.class)
    public void shouldRedirectToDiscoverableUiWithoutSlashAtPathEnd() throws Exception {
        // Given
        HttpGet request = HttpRequestUtils.getRequest("/ui/v1/discoverableclient");

        // When
        final HttpResponse response = HttpClientUtils.client(true).execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_MOVED_TEMPORARILY));
    }

}
