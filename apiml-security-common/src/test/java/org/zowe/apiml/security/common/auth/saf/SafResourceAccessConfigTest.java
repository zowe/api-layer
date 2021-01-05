/*
 * Copyright (c) 2020 Broadcom.  All Rights Reserved.  The term
 * "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This software and all information contained therein is
 * confidential and proprietary and shall not be duplicated,
 * used, disclosed, or disseminated in any way except as
 * authorized by the applicable license agreement, without the
 * express written permission of Broadcom.  All authorized
 * reproductions must be marked with this language.
 *
 * EXCEPT AS SET FORTH IN THE APPLICABLE LICENSE AGREEMENT, TO
 * THE EXTENT PERMITTED BY APPLICABLE LAW, BROADCOM PROVIDES THIS
 * SOFTWARE WITHOUT WARRANTY OF ANY KIND, INCLUDING WITHOUT
 * LIMITATION, ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT WILL BROADCOM
 * BE LIABLE TO THE END USER OR ANY THIRD PARTY FOR ANY LOSS OR
 * DAMAGE, DIRECT OR INDIRECT, FROM THE USE OF THIS SOFTWARE,
 * INCLUDING WITHOUT LIMITATION, LOST PROFITS, BUSINESS
 * INTERRUPTION, GOODWILL, OR LOST DATA, EVEN IF BROADCOM IS
 * EXPRESSLY ADVISED OF SUCH LOSS OR DAMAGE.
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
