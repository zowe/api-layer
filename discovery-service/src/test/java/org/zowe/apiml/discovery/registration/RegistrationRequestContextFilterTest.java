/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationRequestContextFilterTest {

    private final RegistrationRequestContextFilter filter = new RegistrationRequestContextFilter();

    @Test
    void givenRequest_whenFilter_thenPeerAddressIsAvailableOnlyDuringTheChain() throws Exception {
        var request = new MockHttpServletRequest("POST", "/eureka/apps/service");
        request.setRemoteAddr("10.1.1.1");
        var seen = new AtomicReference<Optional<InetAddress>>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> seen.set(RegistrationRequestContext.getPeerAddress()));

        assertEquals(Optional.of(InetAddress.getByName("10.1.1.1")), seen.get());
        assertTrue(RegistrationRequestContext.getPeerAddress().isEmpty());
    }

    @Test
    void givenInvalidRemoteAddress_whenFilter_thenBadRequest() throws Exception {
        var request = new MockHttpServletRequest("POST", "/eureka/apps/service");
        request.setRemoteAddr("invalid.host.invalid");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("chain must not be called");
        });

        assertEquals(400, response.getStatus());
    }

}
