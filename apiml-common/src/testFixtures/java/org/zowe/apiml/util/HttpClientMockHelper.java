/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.OngoingStubbing;

import java.nio.charset.StandardCharsets;

public class HttpClientMockHelper {

    @SneakyThrows
    public static OngoingStubbing<?> whenExecuteThenThrow(CloseableHttpClient httpClientMock, Exception exception) {
        return Mockito.when(httpClientMock.execute(ArgumentMatchers.any(ClassicHttpRequest.class), ArgumentMatchers.any(HttpClientResponseHandler.class)))
                .thenThrow(exception);
    }

    @SneakyThrows
    public static OngoingStubbing<?> mockExecuteWithResponse(CloseableHttpClient httpClientMock, ClassicHttpResponse responseMock) {
        return Mockito.when(httpClientMock.execute(ArgumentMatchers.any(ClassicHttpRequest.class), ArgumentMatchers.any(HttpClientResponseHandler.class)))
                .thenAnswer((InvocationOnMock invocation) -> invokeResponseHandler(invocation, responseMock));
    }

    @SneakyThrows
    public static <T> T invokeResponseHandler(InvocationOnMock invocation, ClassicHttpResponse responseMock) {
        @SuppressWarnings("unchecked")
        HttpClientResponseHandler<T> handler
                = (HttpClientResponseHandler<T>) invocation.getArguments()[1];
        return handler.handleResponse(responseMock);

    }

    public static void mockResponse(ClassicHttpResponse responseMock, int statusCode, String responseBody) {
        mockResponse(responseMock, statusCode);
        mockResponse(responseMock, responseBody);
    }

    public static void mockResponse(ClassicHttpResponse responseMock, String responseBody) {
        BasicHttpEntity responseEntity = (responseBody == null) ? null : new BasicHttpEntity(IOUtils.toInputStream(responseBody, StandardCharsets.UTF_8), ContentType.APPLICATION_JSON);
        Mockito.when(responseMock.getEntity()).thenReturn(responseEntity);
    }

    public static void mockResponse(ClassicHttpResponse responseMock, int statusCode) {
        Mockito.when(responseMock.getCode()).thenReturn(statusCode);
    }
}
