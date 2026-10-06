/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class IPAddressUtilTest {

    @ParameterizedTest
    @EmptySource
    @NullSource
    void givenNoHostname_whenGetIpAddress_thenReturnNull(String hostname) {
        assertNull(IPAddressUtil.getIpAddress(hostname));
    }

    @Test
    void givenLocalHost_whenGetIpAddress_theReturnLoopback() {
        assertEquals("127.0.0.1", IPAddressUtil.getIpAddress("localhost"));
    }

    @Test
    void givenUnknownDomain_whenGetIpAddress_theReturnNull() {
        assertNull(IPAddressUtil.getIpAddress("absolutellyunknowndomainatall"));
    }

    @ParameterizedTest
    @CsvSource({
        "127.0.0.1,false",
        "::1, true",
        "[::1], true",
        "2001:db8::/32,false",
        "[::1]:8080, true"
})
    void testIsIPV6Single(String input, boolean isIpAddress) {
        assertEquals(isIpAddress, IPAddressUtil.isIPV6Single(input));
    }

}
