/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.http;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HttpClientProxyConfig.class, HttpClientProxyConfigTest.Config.class })
public class HttpClientProxyConfigTest {

    static CloseableHttpClient client1 = mock(CloseableHttpClient.class);
    static CloseableHttpClient client2 = mock(CloseableHttpClient.class);

    public static class Config {

        @Bean
        public HttpClientChooser httpClientChooser() {
            return new HttpClientChooser(client1, client2);
        }

        @Bean
        public ServiceAuthenticationDecorator serviceAuthenticationDecorator() {
            return mock(ServiceAuthenticationDecorator.class);
        }
    }

    @Autowired
    CloseableHttpClient httpClientProxy;

    @Test
    public void dummyAutowireTest() {
        assertThat(httpClientProxy, is(not(nullValue())));
    }

    @Test
    public void givenProxyInTestEnv_whenMethodCalled_thenInvoked() throws IOException {
        httpClientProxy.execute(mock(HttpUriRequest.class));
        verify(client1, times(1)).execute(any());
        verify(client2, times(0)).execute(any());
    }


}
