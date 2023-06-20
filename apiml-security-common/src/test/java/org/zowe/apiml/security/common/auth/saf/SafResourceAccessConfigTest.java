/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SafResourceAccessConfigTest {

    private static final RestTemplate restTemplate = mock(RestTemplate.class);

    private static final SafResourceAccessVerifying ENDPOINT_PROVIDER = mock(SafResourceAccessVerifying.class);
    private static final SafResourceAccessVerifying NATIVE_PROVIDER = mock(SafResourceAccessVerifying.class);
    private static final SafResourceAccessVerifying DUMMY_PROVIDER = mock(SafResourceAccessVerifying.class);

    private SafResourceAccessVerifying getSafResourceAccessConfig(
        String provider, boolean endpointEnabled,
        boolean endpointAvailable, boolean nativeAvailable, boolean dummyAvailable
    ) {
        SafResourceAccessConfig output = new SafResourceAccessConfigMock(endpointAvailable, nativeAvailable, dummyAvailable);
        ReflectionTestUtils.setField(output, "provider", provider);
        ReflectionTestUtils.setField(output, "endpointEnabled", endpointEnabled);
        return output.safResourceAccessVerifying(restTemplate);
    }

    @Test
    void givenNoProviderAndAvailableEnabledEndpoint_whenCreate_thenReturnEndpointProvider() {
        assertSame(ENDPOINT_PROVIDER, getSafResourceAccessConfig("", true, true, false, false));
        assertSame(ENDPOINT_PROVIDER, getSafResourceAccessConfig(null, true, true, false, false));
    }

    @Test
    void givenNoProviderAndAvailableDisableEndpoint_whenCreate_thenReturnNull() {
        assertNull(getSafResourceAccessConfig("", false, true, false, false));
        assertNull(getSafResourceAccessConfig(null, false, true, false, false));
    }

    @Test
    void givenNoProviderAndAvailableDisableEndpointAndNative_whenCreate_thenReturnNativeProvider() {
        assertSame(NATIVE_PROVIDER, getSafResourceAccessConfig("", false, true, true, false));
        assertSame(NATIVE_PROVIDER, getSafResourceAccessConfig(null, false, true, true, false));
    }


    @Test
    void givenNoProviderAndAvailableNative_whenCreate_thenReturnNativeProvider() {
        assertSame(NATIVE_PROVIDER, getSafResourceAccessConfig("", true, false, true, false));
        assertSame(NATIVE_PROVIDER, getSafResourceAccessConfig(null, false, false, true, false));
    }

    @Test
    void givenNoProviderAndAvailableDummy_whenCreate_thenReturnDummyProvider() {
        assertSame(DUMMY_PROVIDER, getSafResourceAccessConfig("", true, false, false, true));
        assertSame(DUMMY_PROVIDER, getSafResourceAccessConfig(null, false, false, false, true));
    }

    @Test
    void givenNoProviderAndNoAvailableProvider_whenCreate_thenReturnNull() {
        assertNull(getSafResourceAccessConfig("", true, false, false, false));
        assertNull(getSafResourceAccessConfig(null, false, false, false, false));
    }

    @Test
    void givenEndpointProvider_whenCreate_thenReturnEndpointProvider() {
        assertSame(ENDPOINT_PROVIDER, getSafResourceAccessConfig("endpoint", true, true, true, true));
    }

    @Test
    void givenNativeProvider_whenCreate_thenReturnNativeProvider() {
        assertSame(NATIVE_PROVIDER, getSafResourceAccessConfig("NATIVE", true, true, true, true));
    }

    @Test
    void givenDummyProvider_whenCreate_thenReturnDummyProvider() {
        assertSame(DUMMY_PROVIDER, getSafResourceAccessConfig("duMMy", true, true, true, true));
    }

    @Test
    void givenEndpointProviderAndDisabledEndpoint_whenCreate_thenReturnEndpointProvider() {
        assertSame(ENDPOINT_PROVIDER, getSafResourceAccessConfig("endpoint", false, true, true, true));
    }


    @Test
    void givenUnknownProvider_whenCreate_thenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> getSafResourceAccessConfig("unknown", true, true, true, true));
    }

    @RequiredArgsConstructor
    public static class SafResourceAccessConfigMock extends SafResourceAccessConfig {

        private final boolean endpointAvailable;
        private final boolean nativeAvailable;
        private final boolean dummyAvailable;

        @Override
        protected SafResourceAccessVerifying createEndpoint(RestTemplate restTemplate) {
            if (!endpointAvailable) return null;
            return ENDPOINT_PROVIDER;
        }

        @Override
        protected SafResourceAccessVerifying createNative() throws ClassNotFoundException {
            if (!nativeAvailable) throw new ClassNotFoundException();
            return NATIVE_PROVIDER;
        }

        @Override
        protected SafResourceAccessVerifying createDummy() throws IOException {
            if (!dummyAvailable) throw new IOException();
            return DUMMY_PROVIDER;
        }

    }

}
