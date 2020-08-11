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

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class GreetingApiIntegrationTest {
    @Test
    @TestsNotMeantForZowe
    public void shouldCallDiscoverableServiceApi() throws Exception {
        // When
        final HttpResponse response = HttpRequestUtils.getResponse("/discoverableclient/api/v1/greeting", SC_OK);
        final String jsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);
        String content = jsonContext.read("$.content");

        // Then
        assertThat(content, equalTo("Hello, world!"));
    }
    @Test
    @TestsNotMeantForZowe
    public void shouldCallDiscoverableServiceApi_OldPathFormat() throws Exception {
        // When
        final HttpResponse response = HttpRequestUtils.getResponse("/api/v1/discoverableclient/greeting", SC_OK);
        final String jsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);
        String content = jsonContext.read("$.content");

        // Then
        assertThat(content, equalTo("Hello, world!"));
    }
}
